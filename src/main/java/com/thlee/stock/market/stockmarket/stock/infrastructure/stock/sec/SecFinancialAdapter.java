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

    private static final Set<String> VALID_QUARTERS = Set.of("Q1", "Q2", "Q3", "Q4");

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
    public List<SecFinancialStatement> getQuarterlyFinancialStatements(String ticker) {
        ParsedCompanyFacts parsed = getParsedFacts(ticker);

        return List.of(
                buildQuarterlyIncomeStatement(parsed),
                buildQuarterlyBalanceSheet(parsed),
                buildQuarterlyCashFlowStatement(parsed)
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

        // 연간 데이터
        Map<String, Map<Integer, Double>> usdData = new HashMap<>();
        Map<String, Map<Integer, Double>> sharesData = new HashMap<>();
        Set<Integer> allYears = new TreeSet<>(Comparator.reverseOrder());

        // 분기 데이터
        Map<String, Map<String, Double>> quarterlyUsdData = new HashMap<>();
        Map<String, Map<String, Double>> quarterlySharesData = new HashMap<>();
        Set<String> allQuarters = new TreeSet<>(Comparator.reverseOrder());

        for (Map.Entry<String, TagData> entry : usGaap.entrySet()) {
            String tag = entry.getKey();
            TagData tagData = entry.getValue();

            // 연간 USD
            Map<Integer, Double> usdYearValues = extractAnnualValues(tagData.getUsdEntries());
            if (!usdYearValues.isEmpty()) {
                usdData.put(tag, usdYearValues);
                allYears.addAll(usdYearValues.keySet());
            }

            // 연간 USD/shares
            Map<Integer, Double> sharesYearValues = extractAnnualValues(tagData.getSharesEntries());
            if (!sharesYearValues.isEmpty()) {
                sharesData.put(tag, sharesYearValues);
                allYears.addAll(sharesYearValues.keySet());
            }

            // 분기 USD
            Map<String, Double> qtrUsdValues = extractQuarterlyValues(tagData.getUsdEntries());
            if (!qtrUsdValues.isEmpty()) {
                quarterlyUsdData.put(tag, qtrUsdValues);
                allQuarters.addAll(qtrUsdValues.keySet());
            }

            // 분기 USD/shares
            Map<String, Double> qtrSharesValues = extractQuarterlyValues(tagData.getSharesEntries());
            if (!qtrSharesValues.isEmpty()) {
                quarterlySharesData.put(tag, qtrSharesValues);
                allQuarters.addAll(qtrSharesValues.keySet());
            }
        }

        List<Integer> recentYears = allYears.stream().limit(3).toList();
        List<String> recentQuarters = allQuarters.stream().limit(8).toList();

        return new ParsedCompanyFacts(usdData, sharesData, recentYears,
                quarterlyUsdData, quarterlySharesData, recentQuarters);
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
                        (existing, replacement) -> replacement
                ));
    }

    /**
     * 10-Q 필터로 분기 데이터 추출, 키: "2024Q1" 형식, 중복 분기는 최신 filed 기준
     */
    private Map<String, Double> extractQuarterlyValues(List<FactEntry> entries) {
        return entries.stream()
                .filter(e -> "10-Q".equals(e.getForm()))
                .filter(e -> e.getFy() != null && e.getFp() != null && e.getVal() != null)
                .filter(e -> VALID_QUARTERS.contains(e.getFp()))
                .collect(Collectors.toMap(
                        e -> e.getFy().intValue() + e.getFp(),
                        FactEntry::getVal,
                        (existing, replacement) -> replacement
                ));
    }

    // === 연간 재무제표 구성 ===

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
        items.add(buildFreeCashFlow(parsed));
        return new SecFinancialStatement(StatementType.CASHFLOW, items);
    }

    // === 분기 재무제표 구성 ===

    private SecFinancialStatement buildQuarterlyIncomeStatement(ParsedCompanyFacts parsed) {
        List<SecFinancialItem> items = new ArrayList<>();
        items.add(buildQuarterlyItem(parsed, "매출", "Revenue",
                "Revenues", "RevenueFromContractWithCustomersExcludingAssessedTax"));
        items.add(buildQuarterlyItem(parsed, "매출원가", "Cost of Revenue",
                "CostOfGoodsAndServicesSold", "CostOfRevenue"));
        items.add(buildQuarterlyItem(parsed, "매출총이익", "Gross Profit",
                "GrossProfit"));
        items.add(buildQuarterlyItem(parsed, "영업이익", "Operating Income",
                "OperatingIncomeLoss"));
        items.add(buildQuarterlyItem(parsed, "순이익", "Net Income",
                "NetIncomeLoss", "ProfitLoss"));
        return new SecFinancialStatement(StatementType.INCOME, items);
    }

    private SecFinancialStatement buildQuarterlyBalanceSheet(ParsedCompanyFacts parsed) {
        List<SecFinancialItem> items = new ArrayList<>();
        items.add(buildQuarterlyItem(parsed, "총자산", "Total Assets",
                "Assets"));
        items.add(buildQuarterlyItem(parsed, "유동자산", "Current Assets",
                "AssetsCurrent"));
        items.add(buildQuarterlyItem(parsed, "총부채", "Total Liabilities",
                "Liabilities"));
        items.add(buildQuarterlyItem(parsed, "유동부채", "Current Liabilities",
                "LiabilitiesCurrent"));
        items.add(buildQuarterlyItem(parsed, "자기자본", "Stockholders Equity",
                "StockholdersEquity",
                "StockholdersEquityIncludingPortionAttributableToNoncontrollingInterest"));
        return new SecFinancialStatement(StatementType.BALANCE, items);
    }

    private SecFinancialStatement buildQuarterlyCashFlowStatement(ParsedCompanyFacts parsed) {
        List<SecFinancialItem> items = new ArrayList<>();
        items.add(buildQuarterlyItem(parsed, "영업활동 현금흐름", "Operating Cash Flow",
                "NetCashProvidedByUsedInOperatingActivities",
                "NetCashProvidedByUsedInOperatingActivitiesContinuingOperations"));
        items.add(buildQuarterlyItem(parsed, "투자활동 현금흐름", "Investing Cash Flow",
                "NetCashProvidedByUsedInInvestingActivities",
                "NetCashProvidedByUsedInInvestingActivitiesContinuingOperations"));
        items.add(buildQuarterlyItem(parsed, "재무활동 현금흐름", "Financing Cash Flow",
                "NetCashProvidedByUsedInFinancingActivities",
                "NetCashProvidedByUsedInFinancingActivitiesContinuingOperations"));
        items.add(buildQuarterlyItem(parsed, "설비투자(CapEx)", "Capital Expenditure",
                "PaymentsToAcquirePropertyPlantAndEquipment"));
        items.add(buildQuarterlyFreeCashFlow(parsed));
        return new SecFinancialStatement(StatementType.CASHFLOW, items);
    }

    // === 항목 구성 헬퍼 ===

    private SecFinancialItem buildItem(ParsedCompanyFacts parsed,
                                       String label, String labelEn, String... tags) {
        Map<Integer, Double> values = getTagValues(parsed, tags);

        Map<String, Long> yearValues = new LinkedHashMap<>();
        for (Integer year : parsed.recentYears()) {
            Double val = values.get(year);
            yearValues.put(String.valueOf(year), val != null ? Math.round(val) : null);
        }

        return new SecFinancialItem(label, labelEn, yearValues);
    }

    private SecFinancialItem buildQuarterlyItem(ParsedCompanyFacts parsed,
                                                 String label, String labelEn, String... tags) {
        Map<String, Double> values = getQuarterlyTagValues(parsed, tags);

        Map<String, Long> quarterValues = new LinkedHashMap<>();
        for (String quarter : parsed.recentQuarters()) {
            Double val = values.get(quarter);
            quarterValues.put(quarter, val != null ? Math.round(val) : null);
        }

        return new SecFinancialItem(label, labelEn, quarterValues);
    }

    private SecFinancialItem buildFreeCashFlow(ParsedCompanyFacts parsed) {
        Map<Integer, Double> operatingCf = getTagValues(parsed,
                "NetCashProvidedByUsedInOperatingActivities",
                "NetCashProvidedByUsedInOperatingActivitiesContinuingOperations");
        Map<Integer, Double> capEx = getTagValues(parsed,
                "PaymentsToAcquirePropertyPlantAndEquipment");

        Map<String, Long> fcfValues = new LinkedHashMap<>();
        for (Integer year : parsed.recentYears()) {
            Double opCf = operatingCf.get(year);
            Double cap = capEx.get(year);
            if (opCf != null && cap != null) {
                fcfValues.put(String.valueOf(year), Math.round(opCf - cap));
            } else {
                fcfValues.put(String.valueOf(year), null);
            }
        }
        return new SecFinancialItem("잉여현금흐름(FCF)", "Free Cash Flow", fcfValues);
    }

    private SecFinancialItem buildQuarterlyFreeCashFlow(ParsedCompanyFacts parsed) {
        Map<String, Double> operatingCf = getQuarterlyTagValues(parsed,
                "NetCashProvidedByUsedInOperatingActivities",
                "NetCashProvidedByUsedInOperatingActivitiesContinuingOperations");
        Map<String, Double> capEx = getQuarterlyTagValues(parsed,
                "PaymentsToAcquirePropertyPlantAndEquipment");

        Map<String, Long> fcfValues = new LinkedHashMap<>();
        for (String quarter : parsed.recentQuarters()) {
            Double opCf = operatingCf.get(quarter);
            Double cap = capEx.get(quarter);
            if (opCf != null && cap != null) {
                fcfValues.put(quarter, Math.round(opCf - cap));
            } else {
                fcfValues.put(quarter, null);
            }
        }
        return new SecFinancialItem("잉여현금흐름(FCF)", "Free Cash Flow", fcfValues);
    }

    // === 태그 조회 헬퍼 ===

    private Map<Integer, Double> getTagValues(ParsedCompanyFacts parsed, String... tags) {
        for (String tag : tags) {
            Map<Integer, Double> values = parsed.usdData().get(tag);
            if (values != null && !values.isEmpty()) {
                return values;
            }
        }
        return Map.of();
    }

    private Map<String, Double> getQuarterlyTagValues(ParsedCompanyFacts parsed, String... tags) {
        for (String tag : tags) {
            Map<String, Double> values = parsed.quarterlyUsdData().get(tag);
            if (values != null && !values.isEmpty()) {
                return values;
            }
        }
        return Map.of();
    }

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
            List<Integer> recentYears,
            Map<String, Map<String, Double>> quarterlyUsdData,
            Map<String, Map<String, Double>> quarterlySharesData,
            List<String> recentQuarters
    ) {}
}
