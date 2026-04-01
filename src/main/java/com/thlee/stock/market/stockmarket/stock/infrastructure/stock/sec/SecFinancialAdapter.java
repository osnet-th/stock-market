package com.thlee.stock.market.stockmarket.stock.infrastructure.stock.sec;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.thlee.stock.market.stockmarket.stock.domain.model.SecFinancialItem;
import com.thlee.stock.market.stockmarket.stock.domain.model.SecFinancialStatement;
import com.thlee.stock.market.stockmarket.stock.domain.model.SecFinancialStatement.StatementType;
import com.thlee.stock.market.stockmarket.stock.domain.model.SecInvestmentMetric;
import com.thlee.stock.market.stockmarket.stock.domain.service.SecFinancialPort;
import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.sec.dto.SecCompanyFactsResponse;
import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.sec.dto.SecCompanyFactsResponse.FactEntry;
import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.sec.dto.SecCompanyFactsResponse.TagData;
import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.sec.exception.SecApiException;
import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.sec.exception.SecErrorType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Component
public class SecFinancialAdapter implements SecFinancialPort {

    private final SecApiClient secApiClient;
    private final SecCikCache secCikCache;

    /**
     * 추출/필터링된 Company Facts 데이터 캐시 (ticker → 추출 데이터)
     * raw 응답(2-8MB)이 아닌 가공된 데이터만 캐싱
     */
    private final Cache<String, ParsedCompanyFacts> factsCache = Caffeine.newBuilder()
            .expireAfterWrite(24, TimeUnit.HOURS)
            .maximumSize(100)
            .build();

    public SecFinancialAdapter(SecApiClient secApiClient, SecCikCache secCikCache) {
        this.secApiClient = secApiClient;
        this.secCikCache = secCikCache;
    }

    @Override
    public List<SecFinancialStatement> getFinancialStatements(String ticker) {
        ParsedCompanyFacts parsed = getParsedFacts(ticker);

        return List.of(
                buildIncomeStatement(parsed),
                buildBalanceSheet(parsed),
                buildCashFlowStatement(parsed)
        );
    }

    @Override
    public List<SecInvestmentMetric> getInvestmentMetrics(String ticker) {
        ParsedCompanyFacts parsed = getParsedFacts(ticker);
        List<SecInvestmentMetric> metrics = new ArrayList<>();

        // EPS (기본)
        Double epsBasic = getLatestValue(parsed, "EarningsPerShareBasic", true);
        metrics.add(new SecInvestmentMetric("EPS (기본)", epsBasic, "$",
                "주당순이익 (Basic Earnings Per Share)"));

        // EPS (희석)
        Double epsDiluted = getLatestValue(parsed, "EarningsPerShareDiluted", true);
        metrics.add(new SecInvestmentMetric("EPS (희석)", epsDiluted, "$",
                "희석주당순이익 (Diluted Earnings Per Share)"));

        // ROE
        Double netIncome = getLatestValue(parsed, "NetIncomeLoss", "ProfitLoss");
        Double equity = getLatestValue(parsed, "StockholdersEquity",
                "StockholdersEquityIncludingPortionAttributableToNoncontrollingInterest");
        Double roe = (netIncome != null && equity != null && equity != 0)
                ? Math.round(netIncome / equity * 10000.0) / 100.0 : null;
        metrics.add(new SecInvestmentMetric("ROE", roe, "%",
                "자기자본이익률 (Return on Equity)"));

        // 부채비율
        Double liabilities = getLatestValue(parsed, "Liabilities");
        Double debtRatio = (liabilities != null && equity != null && equity != 0)
                ? Math.round(liabilities / equity * 10000.0) / 100.0 : null;
        metrics.add(new SecInvestmentMetric("부채비율", debtRatio, "%",
                "부채비율 (Debt to Equity Ratio)"));

        // 영업이익률
        Double operatingIncome = getLatestValue(parsed, "OperatingIncomeLoss");
        Double revenue = getLatestValue(parsed, "Revenues",
                "RevenueFromContractWithCustomersExcludingAssessedTax");
        Double operatingMargin = (operatingIncome != null && revenue != null && revenue != 0)
                ? Math.round(operatingIncome / revenue * 10000.0) / 100.0 : null;
        metrics.add(new SecInvestmentMetric("영업이익률", operatingMargin, "%",
                "영업이익률 (Operating Margin)"));

        return metrics;
    }

    // === 내부 메서드 ===

    private ParsedCompanyFacts getParsedFacts(String ticker) {
        ParsedCompanyFacts cached = factsCache.getIfPresent(ticker.toUpperCase());
        if (cached != null) {
            return cached;
        }

        Long cik = secCikCache.getCik(ticker)
                .orElseThrow(() -> new SecApiException(SecErrorType.CIK_NOT_FOUND,
                        "SEC CIK 매핑을 찾을 수 없습니다: " + ticker));

        String cik10 = SecCikCache.formatCik(cik);
        SecCompanyFactsResponse response = secApiClient.fetchCompanyFacts(cik10);
        ParsedCompanyFacts parsed = parseCompanyFacts(response);
        factsCache.put(ticker.toUpperCase(), parsed);
        return parsed;
    }

    private ParsedCompanyFacts parseCompanyFacts(SecCompanyFactsResponse response) {
        Map<String, TagData> usGaap = response.getUsGaapFacts();
        Map<String, Map<Integer, Double>> usdData = new HashMap<>();
        Map<String, Map<Integer, Double>> sharesData = new HashMap<>();
        Set<Integer> allYears = new TreeSet<>(Comparator.reverseOrder());

        for (Map.Entry<String, TagData> entry : usGaap.entrySet()) {
            String tag = entry.getKey();
            TagData tagData = entry.getValue();

            // USD 단위 데이터
            Map<Integer, Double> usdYearValues = extractAnnualValues(tagData.getUsdEntries());
            if (!usdYearValues.isEmpty()) {
                usdData.put(tag, usdYearValues);
                allYears.addAll(usdYearValues.keySet());
            }

            // USD/shares 단위 데이터 (EPS 등)
            Map<Integer, Double> sharesYearValues = extractAnnualValues(tagData.getSharesEntries());
            if (!sharesYearValues.isEmpty()) {
                sharesData.put(tag, sharesYearValues);
                allYears.addAll(sharesYearValues.keySet());
            }
        }

        // 최근 3개년
        List<Integer> recentYears = allYears.stream().limit(3).toList();

        return new ParsedCompanyFacts(usdData, sharesData, recentYears);
    }

    /**
     * 10-K + FY 필터로 연간 데이터만 추출, 중복 연도는 최신 filed 기준
     */
    private Map<Integer, Double> extractAnnualValues(List<FactEntry> entries) {
        return entries.stream()
                .filter(e -> "10-K".equals(e.getForm()) && "FY".equals(e.getFp()))
                .filter(e -> e.getFy() != null && e.getVal() != null)
                .collect(Collectors.toMap(
                        e -> e.getFy().intValue(),
                        FactEntry::getVal,
                        (existing, replacement) -> replacement  // 동일 연도 시 나중 것(최신 filed) 사용
                ));
    }

    // === 재무제표 구성 ===

    private SecFinancialStatement buildIncomeStatement(ParsedCompanyFacts parsed) {
        List<SecFinancialItem> items = new ArrayList<>();
        items.add(buildItem(parsed, "매출", "Revenue",
                "Revenues", "RevenueFromContractWithCustomersExcludingAssessedTax"));
        items.add(buildItem(parsed, "매출원가", "Cost of Revenue",
                "CostOfGoodsAndServicesSold", "CostOfRevenue"));
        items.add(buildItem(parsed, "매출총이익", "Gross Profit",
                "GrossProfit"));
        items.add(buildItem(parsed, "영업이익", "Operating Income",
                "OperatingIncomeLoss"));
        items.add(buildItem(parsed, "순이익", "Net Income",
                "NetIncomeLoss", "ProfitLoss"));
        return new SecFinancialStatement(StatementType.INCOME, items);
    }

    private SecFinancialStatement buildBalanceSheet(ParsedCompanyFacts parsed) {
        List<SecFinancialItem> items = new ArrayList<>();
        items.add(buildItem(parsed, "총자산", "Total Assets",
                "Assets"));
        items.add(buildItem(parsed, "유동자산", "Current Assets",
                "AssetsCurrent"));
        items.add(buildItem(parsed, "총부채", "Total Liabilities",
                "Liabilities"));
        items.add(buildItem(parsed, "유동부채", "Current Liabilities",
                "LiabilitiesCurrent"));
        items.add(buildItem(parsed, "자기자본", "Stockholders Equity",
                "StockholdersEquity",
                "StockholdersEquityIncludingPortionAttributableToNoncontrollingInterest"));
        return new SecFinancialStatement(StatementType.BALANCE, items);
    }

    private SecFinancialStatement buildCashFlowStatement(ParsedCompanyFacts parsed) {
        List<SecFinancialItem> items = new ArrayList<>();
        items.add(buildItem(parsed, "영업활동 현금흐름", "Operating Cash Flow",
                "NetCashProvidedByUsedInOperatingActivities",
                "NetCashProvidedByUsedInOperatingActivitiesContinuingOperations"));
        items.add(buildItem(parsed, "투자활동 현금흐름", "Investing Cash Flow",
                "NetCashProvidedByUsedInInvestingActivities",
                "NetCashProvidedByUsedInInvestingActivitiesContinuingOperations"));
        items.add(buildItem(parsed, "재무활동 현금흐름", "Financing Cash Flow",
                "NetCashProvidedByUsedInFinancingActivities",
                "NetCashProvidedByUsedInFinancingActivitiesContinuingOperations"));
        items.add(buildItem(parsed, "설비투자(CapEx)", "Capital Expenditure",
                "PaymentsToAcquirePropertyPlantAndEquipment"));

        // Free Cash Flow = 영업CF - CapEx
        items.add(buildFreeCashFlow(parsed));

        return new SecFinancialStatement(StatementType.CASHFLOW, items);
    }

    private SecFinancialItem buildFreeCashFlow(ParsedCompanyFacts parsed) {
        Map<Integer, Double> operatingCf = getTagValues(parsed,
                "NetCashProvidedByUsedInOperatingActivities",
                "NetCashProvidedByUsedInOperatingActivitiesContinuingOperations");
        Map<Integer, Double> capEx = getTagValues(parsed,
                "PaymentsToAcquirePropertyPlantAndEquipment");

        Map<Integer, Long> fcfValues = new LinkedHashMap<>();
        for (Integer year : parsed.recentYears()) {
            Double opCf = operatingCf.get(year);
            Double cap = capEx.get(year);
            if (opCf != null && cap != null) {
                fcfValues.put(year, Math.round(opCf - cap));
            } else {
                fcfValues.put(year, null);
            }
        }
        return new SecFinancialItem("잉여현금흐름(FCF)", "Free Cash Flow", fcfValues);
    }

    /**
     * XBRL 태그에서 항목 구성 (fallback 지원)
     */
    private SecFinancialItem buildItem(ParsedCompanyFacts parsed,
                                       String label, String labelEn, String... tags) {
        Map<Integer, Double> values = getTagValues(parsed, tags);

        Map<Integer, Long> yearValues = new LinkedHashMap<>();
        for (Integer year : parsed.recentYears()) {
            Double val = values.get(year);
            yearValues.put(year, val != null ? Math.round(val) : null);
        }

        return new SecFinancialItem(label, labelEn, yearValues);
    }

    /**
     * 태그 fallback으로 값 조회 (USD 단위)
     */
    private Map<Integer, Double> getTagValues(ParsedCompanyFacts parsed, String... tags) {
        for (String tag : tags) {
            Map<Integer, Double> values = parsed.usdData().get(tag);
            if (values != null && !values.isEmpty()) {
                return values;
            }
        }
        return Map.of();
    }

    /**
     * 최신 연도 값 조회 (USD 단위, fallback 지원)
     */
    private Double getLatestValue(ParsedCompanyFacts parsed, String... tags) {
        return getLatestValue(parsed, tags, false);
    }

    private Double getLatestValue(ParsedCompanyFacts parsed, String tag, boolean isShares) {
        return getLatestValue(parsed, new String[]{tag}, isShares);
    }

    private Double getLatestValue(ParsedCompanyFacts parsed, String[] tags, boolean isShares) {
        for (String tag : tags) {
            Map<String, Map<Integer, Double>> dataSource = isShares ? parsed.sharesData() : parsed.usdData();
            Map<Integer, Double> values = dataSource.get(tag);
            if (values != null && !values.isEmpty()) {
                // 최근 연도 값
                for (Integer year : parsed.recentYears()) {
                    Double val = values.get(year);
                    if (val != null) {
                        return val;
                    }
                }
            }
        }
        return null;
    }

    /**
     * 파싱된 Company Facts (캐시 대상)
     */
    record ParsedCompanyFacts(
            Map<String, Map<Integer, Double>> usdData,
            Map<String, Map<Integer, Double>> sharesData,
            List<Integer> recentYears
    ) {}
}