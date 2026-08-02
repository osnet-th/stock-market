package com.thlee.stock.market.stockmarket.stock.infrastructure.stock.sec;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.thlee.stock.market.stockmarket.stock.domain.model.SecFinancialItem;
import com.thlee.stock.market.stockmarket.stock.domain.model.SecFinancialStatement;
import com.thlee.stock.market.stockmarket.stock.domain.model.SecFinancialStatement.StatementType;
import com.thlee.stock.market.stockmarket.stock.domain.model.SecInvestmentMetric;
import com.thlee.stock.market.stockmarket.stock.domain.model.UsCompanyFacts;
import com.thlee.stock.market.stockmarket.stock.domain.model.UsCompanyProfile;
import com.thlee.stock.market.stockmarket.stock.domain.model.UsFiling;
import com.thlee.stock.market.stockmarket.stock.domain.model.UsFinancialConcept;
import com.thlee.stock.market.stockmarket.stock.domain.service.SecFinancialPort;
import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.sec.dto.SecCompanyFactsResponse;
import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.sec.dto.SecCompanyFactsResponse.FactEntry;
import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.sec.dto.SecCompanyFactsResponse.TagData;
import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.sec.dto.SecSubmissionsResponse;
import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.sec.exception.SecApiException;
import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.sec.exception.SecErrorType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
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

    /**
     * submissions 응답 캐시 (ticker → 원본 응답, ~100KB급이라 원본 보관)
     */
    private final Cache<String, SecSubmissionsResponse> submissionsCache = Caffeine.newBuilder()
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
                "RevenueFromContractWithCustomerExcludingAssessedTax",
                "SalesRevenueNet", "SalesRevenueGoodsNet", "SalesRevenueServicesNet");
        Double operatingMargin = (operatingIncome != null && revenue != null && revenue != 0)
                ? Math.round(operatingIncome / revenue * 10000.0) / 100.0 : null;
        metrics.add(new SecInvestmentMetric("영업이익률", operatingMargin, "%",
                "영업이익률 (Operating Margin)"));

        return metrics;
    }

    @Override
    public Long getCik(String ticker) {
        return secCikCache.getCik(ticker)
                .orElseThrow(() -> new SecApiException(SecErrorType.CIK_NOT_FOUND,
                        "CIK를 찾을 수 없습니다: " + ticker));
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
        Map<String, Map<Integer, Double>> shareCountData = new HashMap<>();
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

            // 연간 shares (주식수 — 리포트 조달용, recentYears 산정에는 미반영)
            Map<Integer, Double> shareCountValues = extractAnnualValues(tagData.getShareCountEntries());
            if (!shareCountValues.isEmpty()) {
                shareCountData.put(tag, shareCountValues);
            }

            // 분기 USD (10-Q + 10-K fp="Q4", FY Fallback)
            Map<String, Double> qtrUsdValues = extractQuarterlyValues(tagData.getUsdEntries());
            fillQ4Fallback(qtrUsdValues, usdYearValues, tagData.getUsdEntries());
            if (!qtrUsdValues.isEmpty()) {
                quarterlyUsdData.put(tag, qtrUsdValues);
                allQuarters.addAll(qtrUsdValues.keySet());
            }

            // 분기 USD/shares (10-Q + 10-K fp="Q4", FY Fallback)
            Map<String, Double> qtrSharesValues = extractQuarterlyValues(tagData.getSharesEntries());
            fillQ4Fallback(qtrSharesValues, sharesYearValues, tagData.getSharesEntries());
            if (!qtrSharesValues.isEmpty()) {
                quarterlySharesData.put(tag, qtrSharesValues);
                allQuarters.addAll(qtrSharesValues.keySet());
            }
        }

        // dei 네임스페이스 주식수 (EntityCommonStockSharesOutstanding 등)
        for (Map.Entry<String, TagData> entry : response.getDeiFacts().entrySet()) {
            Map<Integer, Double> shareCountValues = extractAnnualValues(entry.getValue().getShareCountEntries());
            if (!shareCountValues.isEmpty()) {
                shareCountData.put(entry.getKey(), shareCountValues);
            }
        }

        List<Integer> recentYears = allYears.stream().limit(3).toList();
        List<String> recentQuarters = allQuarters.stream().limit(8).toList();

        return new ParsedCompanyFacts(usdData, sharesData, shareCountData, recentYears,
                quarterlyUsdData, quarterlySharesData, recentQuarters);
    }

    /**
     * 10-K + FY 필터로 연간 데이터만 추출.
     * 10-K 파일링에 포함된 분기 행(Q4)·전기 비교치 행도 fy/fp가 파일링 기준(FY)으로 라벨되므로,
     * 기간 데이터는 연간 기간(300~400일)만 채택하고 같은 연도 중복은 종료일(end)이 최신인 엔트리를 취한다
     * (전기 비교치는 end가 이르고, Q4는 기간 필터에서 배제됨).
     */
    private Map<Integer, Double> extractAnnualValues(List<FactEntry> entries) {
        Map<Integer, FactEntry> best = new HashMap<>();
        for (FactEntry entry : entries) {
            if (!isAnnualEntry(entry)) {
                continue;
            }
            best.merge(entry.getFy().intValue(), entry, this::laterEnd);
        }
        Map<Integer, Double> values = new HashMap<>();
        best.forEach((year, entry) -> values.put(year, entry.getVal()));
        return values;
    }

    private boolean isAnnualEntry(FactEntry entry) {
        return "10-K".equals(entry.getForm()) && "FY".equals(entry.getFp())
                && entry.getFy() != null && entry.getVal() != null
                && isAnnualPeriod(entry);
    }

    /** 시점 데이터(start 없음)는 통과, 기간 데이터는 약 1년(300~400일)만 연간으로 인정 */
    private boolean isAnnualPeriod(FactEntry entry) {
        if (entry.getStart() == null || entry.getEnd() == null) {
            return true;
        }
        try {
            long days = ChronoUnit.DAYS.between(
                    LocalDate.parse(entry.getStart()), LocalDate.parse(entry.getEnd()));
            return days >= 300 && days <= 400;
        } catch (Exception e) {
            return true;
        }
    }

    private FactEntry laterEnd(FactEntry current, FactEntry candidate) {
        String currentEnd = current.getEnd() == null ? "" : current.getEnd();
        String candidateEnd = candidate.getEnd() == null ? "" : candidate.getEnd();
        return candidateEnd.compareTo(currentEnd) > 0 ? candidate : current;
    }

    /**
     * 10-Q + 10-K(fp="Q4") 필터로 분기 데이터 추출, 키: "2024Q1" 형식, 중복 분기는 최신 filed 기준
     */
    private Map<String, Double> extractQuarterlyValues(List<FactEntry> entries) {
        return entries.stream()
                .filter(e -> "10-Q".equals(e.getForm())
                        || ("10-K".equals(e.getForm()) && "Q4".equals(e.getFp())))
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
                "Revenues", "RevenueFromContractWithCustomerExcludingAssessedTax",
                "SalesRevenueNet", "SalesRevenueGoodsNet", "SalesRevenueServicesNet"));
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
                "Revenues", "RevenueFromContractWithCustomerExcludingAssessedTax",
                "SalesRevenueNet", "SalesRevenueGoodsNet", "SalesRevenueServicesNet"));
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

    /**
     * 2순위 Fallback: 연간(FY) 데이터로 Q4 계산.
     * - 시점 데이터 (대차대조표, start==null): Q4 = FY
     * - 기간 데이터 (손익/현금흐름, start!=null): Q4 = FY - 누적Q3
     *   (누적Q3 = start가 FY start와 동일한 Q3 엔트리, 즉 9개월 YTD 값)
     */
    private void fillQ4Fallback(Map<String, Double> quarterlyValues,
                                 Map<Integer, Double> annualValues,
                                 List<FactEntry> entries) {
        if (annualValues == null || annualValues.isEmpty()) {
            return;
        }

        boolean isPointInTime = entries.stream()
                .filter(e -> "10-K".equals(e.getForm()) && "FY".equals(e.getFp()))
                .anyMatch(e -> e.getStart() == null);

        // 기간 데이터용: FY 엔트리의 start 날짜를 연도별로 수집 (누적 Q3 매칭용).
        // 연간 기간 필터로 10-K 내 Q4 행의 start가 섞이는 것 방지 (섞이면 누적 Q3 매칭 실패로 Q4 누락)
        Map<Integer, String> fyStartByYear = isPointInTime ? Map.of() : entries.stream()
                .filter(e -> "10-K".equals(e.getForm()) && "FY".equals(e.getFp()))
                .filter(e -> e.getFy() != null && e.getStart() != null)
                .filter(this::isAnnualPeriod)
                .collect(Collectors.toMap(
                        e -> e.getFy().intValue(),
                        FactEntry::getStart,
                        (existing, replacement) -> replacement
                ));

        for (Map.Entry<Integer, Double> annualEntry : annualValues.entrySet()) {
            int year = annualEntry.getKey();
            String q4Key = year + "Q4";

            if (quarterlyValues.containsKey(q4Key)) {
                continue;
            }

            Double fyValue = annualEntry.getValue();
            if (fyValue == null) {
                continue;
            }

            if (isPointInTime) {
                quarterlyValues.put(q4Key, fyValue);
            } else {
                // Q4 = FY - 누적Q3 (start가 FY start와 동일한 9개월 YTD 값)
                String fyStart = fyStartByYear.get(year);
                if (fyStart == null) {
                    continue;
                }

                Double cumulativeQ3 = entries.stream()
                        .filter(e -> "10-Q".equals(e.getForm()) && "Q3".equals(e.getFp()))
                        .filter(e -> e.getFy() != null && e.getFy().intValue() == year)
                        .filter(e -> fyStart.equals(e.getStart()))
                        .filter(e -> e.getVal() != null)
                        .map(FactEntry::getVal)
                        .reduce((first, second) -> second)
                        .orElse(null);

                if (cumulativeQ3 != null) {
                    quarterlyValues.put(q4Key, fyValue - cumulativeQ3);
                }
            }
        }
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

    /**
     * 폴백 체인 중 최신 연도를 커버하는 태그의 시리즈 선택 (동률이면 데이터 포인트 많은 쪽, 그것도 같으면 체인 순서).
     * "첫 번째 비어있지 않은 태그" 방식은 태그 세대교체 기업(예: AAPL 매출 Revenues→RevenueFromContract...)에서
     * 옛 태그의 잔존 값이 채택돼 최근 연도가 전부 null이 되는 문제가 있어 커버리지 기준으로 선택한다.
     */
    private Map<Integer, Double> getTagValues(ParsedCompanyFacts parsed, String... tags) {
        Map<Integer, Double> best = Map.of();
        for (String tag : tags) {
            Map<Integer, Double> values = parsed.usdData().get(tag);
            if (values == null || values.isEmpty()) {
                continue;
            }
            if (best.isEmpty() || compareCoverage(values, best) > 0) {
                best = values;
            }
        }
        return best;
    }

    private Map<String, Double> getQuarterlyTagValues(ParsedCompanyFacts parsed, String... tags) {
        Map<String, Double> best = Map.of();
        for (String tag : tags) {
            Map<String, Double> values = parsed.quarterlyUsdData().get(tag);
            if (values == null || values.isEmpty()) {
                continue;
            }
            if (best.isEmpty() || compareQuarterCoverage(values, best) > 0) {
                best = values;
            }
        }
        return best;
    }

    /** 최신 연도 우선, 동률이면 데이터 포인트 수 (양수면 left가 더 나은 커버리지) */
    private int compareCoverage(Map<Integer, Double> left, Map<Integer, Double> right) {
        int byLatest = Integer.compare(Collections.max(left.keySet()), Collections.max(right.keySet()));
        return byLatest != 0 ? byLatest : Integer.compare(left.size(), right.size());
    }

    /** 분기 키("2024Q3")는 고정 포맷이라 사전순 비교가 시간순과 일치 */
    private int compareQuarterCoverage(Map<String, Double> left, Map<String, Double> right) {
        int byLatest = Collections.max(left.keySet()).compareTo(Collections.max(right.keySet()));
        return byLatest != 0 ? byLatest : Integer.compare(left.size(), right.size());
    }

    private Double getLatestValue(ParsedCompanyFacts parsed, String... tags) {
        return getLatestValue(parsed, tags, false);
    }

    private Double getLatestValue(ParsedCompanyFacts parsed, String tag, boolean isShares) {
        return getLatestValue(parsed, new String[]{tag}, isShares);
    }

    /**
     * 최근 연도부터 폴백 체인 전체를 훑어 가장 최신 값을 반환 (태그 순서보다 연도 최신성 우선)
     */
    private Double getLatestValue(ParsedCompanyFacts parsed, String[] tags, boolean isShares) {
        Map<String, Map<Integer, Double>> dataSource = isShares ? parsed.sharesData() : parsed.usdData();
        for (Integer year : parsed.recentYears()) {
            for (String tag : tags) {
                Map<Integer, Double> values = dataSource.get(tag);
                Double val = values == null ? null : values.get(year);
                if (val != null) {
                    return val;
                }
            }
        }
        return null;
    }

    // === 리포트 조달 (연간 팩트 / 프로필 / 제출 서식) ===

    @Override
    public UsCompanyFacts getAnnualReportFacts(String ticker, int years) {
        ParsedCompanyFacts parsed = getParsedFacts(ticker);
        Map<UsFinancialConcept, Map<Integer, Double>> merged = new EnumMap<>(UsFinancialConcept.class);
        for (ConceptMapping mapping : CONCEPT_MAPPINGS) {
            Map<Integer, Double> series = mergeChain(sourceOf(parsed, mapping.source()), mapping.tags());
            if (!series.isEmpty()) {
                merged.put(mapping.concept(), series);
            }
        }
        List<Integer> recent = recentReportYears(merged, years);
        Map<UsFinancialConcept, Map<String, BigDecimal>> annual = new EnumMap<>(UsFinancialConcept.class);
        merged.forEach((concept, series) -> {
            Map<String, BigDecimal> filtered = new LinkedHashMap<>();
            for (Integer year : recent) {
                Double val = series.get(year);
                if (val != null) {
                    filtered.put(String.valueOf(year), BigDecimal.valueOf(val));
                }
            }
            if (!filtered.isEmpty()) {
                annual.put(concept, filtered);
            }
        });
        return new UsCompanyFacts(recent.stream().map(String::valueOf).toList(), annual);
    }

    private Map<String, Map<Integer, Double>> sourceOf(ParsedCompanyFacts parsed, ConceptSource source) {
        return switch (source) {
            case USD -> parsed.usdData();
            case PER_SHARE -> parsed.sharesData();
            case SHARES -> parsed.shareCountData();
        };
    }

    /**
     * 폴백 체인 병합: 최신 연도 커버 태그 우선으로 정렬 후, 남은 태그로 과거 연도 공백을 채운다
     * (태그 세대교체 시 과거 이력 보존 — 예: AAPL 매출 SalesRevenueNet(과거) + RevenueFromContract...(현행))
     */
    private Map<Integer, Double> mergeChain(Map<String, Map<Integer, Double>> source, List<String> tags) {
        List<Map<Integer, Double>> candidates = tags.stream()
                .map(source::get)
                .filter(values -> values != null && !values.isEmpty())
                .sorted((left, right) -> compareCoverage(right, left))
                .toList();
        Map<Integer, Double> merged = new HashMap<>();
        for (Map<Integer, Double> candidate : candidates) {
            candidate.forEach(merged::putIfAbsent);
        }
        return merged;
    }

    /**
     * 리포트 컬럼 연도: 핵심 개념(매출·순이익·자산총계)이 커버하는 연도의 합집합에서 최근 years개 (오름차순)
     */
    private List<Integer> recentReportYears(Map<UsFinancialConcept, Map<Integer, Double>> merged, int years) {
        TreeSet<Integer> all = new TreeSet<>();
        for (UsFinancialConcept anchor : List.of(UsFinancialConcept.REVENUE,
                UsFinancialConcept.NET_INCOME, UsFinancialConcept.TOTAL_ASSETS)) {
            all.addAll(merged.getOrDefault(anchor, Map.of()).keySet());
        }
        return all.descendingSet().stream().limit(years).sorted().toList();
    }

    @Override
    public UsCompanyProfile getCompanyProfile(String ticker) {
        SecSubmissionsResponse subs = getSubmissions(ticker);
        String primaryTicker = firstOrNull(subs.getTickers());
        return new UsCompanyProfile(
                subs.getName(),
                subs.getSic(),
                subs.getSicDescription(),
                subs.getFiscalYearEnd(),
                firstOrNull(subs.getExchanges()),
                primaryTicker != null ? primaryTicker : ticker.toUpperCase(),
                formatAddress(subs.getBusinessAddress()),
                firstNonBlank(subs.getWebsite(), subs.getInvestorWebsite()));
    }

    @Override
    public List<UsFiling> getRecentFilings(String ticker, int limit) {
        SecSubmissionsResponse subs = getSubmissions(ticker);
        SecSubmissionsResponse.Recent recent = subs.getFilings() == null ? null : subs.getFilings().getRecent();
        if (recent == null || recent.getForm() == null) {
            return List.of();
        }
        Long cik = secCikCache.getCik(ticker).orElse(null);
        List<UsFiling> filings = new ArrayList<>();
        for (int i = 0; i < recent.getForm().size() && filings.size() < limit; i++) {
            String accessionNumber = listValue(recent.getAccessionNumber(), i);
            String primaryDocument = listValue(recent.getPrimaryDocument(), i);
            filings.add(new UsFiling(
                    recent.getForm().get(i),
                    listValue(recent.getFilingDate(), i),
                    accessionNumber,
                    primaryDocument,
                    listValue(recent.getPrimaryDocDescription(), i),
                    viewerUrl(cik, accessionNumber, primaryDocument)));
        }
        return filings;
    }

    /** EDGAR 원문 문서 URL — https://www.sec.gov/Archives/edgar/data/{cik}/{접수번호 대시 제거}/{문서} */
    private String viewerUrl(Long cik, String accessionNumber, String primaryDocument) {
        if (cik == null || accessionNumber == null || primaryDocument == null) {
            return null;
        }
        return "https://www.sec.gov/Archives/edgar/data/" + cik + "/"
                + accessionNumber.replace("-", "") + "/" + primaryDocument;
    }

    private SecSubmissionsResponse getSubmissions(String ticker) {
        String key = ticker.toUpperCase();
        SecSubmissionsResponse cached = submissionsCache.getIfPresent(key);
        if (cached != null) {
            return cached;
        }
        Long cik = secCikCache.getCik(ticker)
                .orElseThrow(() -> new SecApiException(SecErrorType.CIK_NOT_FOUND,
                        "SEC CIK 매핑을 찾을 수 없습니다: " + ticker));
        SecSubmissionsResponse response = secApiClient.fetchSubmissions(SecCikCache.formatCik(cik));
        submissionsCache.put(key, response);
        return response;
    }

    private String formatAddress(SecSubmissionsResponse.Address address) {
        if (address == null) {
            return null;
        }
        List<String> parts = Arrays.asList(address.getStreet1(), address.getStreet2(),
                        address.getCity(), address.getStateOrCountry(), address.getZipCode()).stream()
                .filter(part -> part != null && !part.isBlank())
                .toList();
        return parts.isEmpty() ? null : String.join(", ", parts);
    }

    private String firstOrNull(List<String> values) {
        return values == null || values.isEmpty() ? null : values.get(0);
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second != null && !second.isBlank() ? second : null;
    }

    private String listValue(List<String> values, int index) {
        return values == null || index >= values.size() ? null : values.get(index);
    }

    /** 리포트 조달용 개념 → us-gaap/dei 태그 폴백 체인 (기업·연도별 태그 세대교체는 mergeChain으로 흡수) */
    private static final List<ConceptMapping> CONCEPT_MAPPINGS = List.of(
            new ConceptMapping(UsFinancialConcept.REVENUE, ConceptSource.USD, List.of(
                    "Revenues", "RevenueFromContractWithCustomerExcludingAssessedTax",
                    "SalesRevenueNet", "SalesRevenueGoodsNet", "SalesRevenueServicesNet")),
            new ConceptMapping(UsFinancialConcept.COST_OF_REVENUE, ConceptSource.USD, List.of(
                    "CostOfGoodsAndServicesSold", "CostOfRevenue", "CostOfGoodsSold")),
            new ConceptMapping(UsFinancialConcept.SGA_EXPENSE, ConceptSource.USD, List.of(
                    "SellingGeneralAndAdministrativeExpense")),
            new ConceptMapping(UsFinancialConcept.OPERATING_INCOME, ConceptSource.USD, List.of(
                    "OperatingIncomeLoss")),
            new ConceptMapping(UsFinancialConcept.PRETAX_INCOME, ConceptSource.USD, List.of(
                    "IncomeLossFromContinuingOperationsBeforeIncomeTaxesExtraordinaryItemsNoncontrollingInterest",
                    "IncomeLossFromContinuingOperationsBeforeIncomeTaxesMinorityInterestAndIncomeLossFromEquityMethodInvestments")),
            new ConceptMapping(UsFinancialConcept.NET_INCOME, ConceptSource.USD, List.of(
                    "NetIncomeLoss", "ProfitLoss")),
            new ConceptMapping(UsFinancialConcept.OPERATING_CF, ConceptSource.USD, List.of(
                    "NetCashProvidedByUsedInOperatingActivities",
                    "NetCashProvidedByUsedInOperatingActivitiesContinuingOperations")),
            new ConceptMapping(UsFinancialConcept.INVESTING_CF, ConceptSource.USD, List.of(
                    "NetCashProvidedByUsedInInvestingActivities",
                    "NetCashProvidedByUsedInInvestingActivitiesContinuingOperations")),
            new ConceptMapping(UsFinancialConcept.FINANCING_CF, ConceptSource.USD, List.of(
                    "NetCashProvidedByUsedInFinancingActivities",
                    "NetCashProvidedByUsedInFinancingActivitiesContinuingOperations")),
            new ConceptMapping(UsFinancialConcept.CAPEX, ConceptSource.USD, List.of(
                    "PaymentsToAcquirePropertyPlantAndEquipment", "PaymentsToAcquireProductiveAssets")),
            new ConceptMapping(UsFinancialConcept.DEPRECIATION_AMORTIZATION, ConceptSource.USD, List.of(
                    "DepreciationDepletionAndAmortization", "DepreciationAmortizationAndAccretionNet",
                    "Depreciation")),
            new ConceptMapping(UsFinancialConcept.TOTAL_ASSETS, ConceptSource.USD, List.of("Assets")),
            new ConceptMapping(UsFinancialConcept.CURRENT_ASSETS, ConceptSource.USD, List.of("AssetsCurrent")),
            new ConceptMapping(UsFinancialConcept.TOTAL_LIABILITIES, ConceptSource.USD, List.of("Liabilities")),
            new ConceptMapping(UsFinancialConcept.CURRENT_LIABILITIES, ConceptSource.USD, List.of(
                    "LiabilitiesCurrent")),
            new ConceptMapping(UsFinancialConcept.EQUITY, ConceptSource.USD, List.of(
                    "StockholdersEquity",
                    "StockholdersEquityIncludingPortionAttributableToNoncontrollingInterest")),
            new ConceptMapping(UsFinancialConcept.RETAINED_EARNINGS, ConceptSource.USD, List.of(
                    "RetainedEarningsAccumulatedDeficit")),
            new ConceptMapping(UsFinancialConcept.CAPITAL_STOCK, ConceptSource.USD, List.of(
                    "CommonStocksIncludingAdditionalPaidInCapital", "CommonStockValue")),
            new ConceptMapping(UsFinancialConcept.CASH, ConceptSource.USD, List.of(
                    "CashAndCashEquivalentsAtCarryingValue",
                    "CashCashEquivalentsRestrictedCashAndRestrictedCashEquivalents")),
            new ConceptMapping(UsFinancialConcept.SECURITIES_CURRENT, ConceptSource.USD, List.of(
                    "MarketableSecuritiesCurrent", "ShortTermInvestments", "AvailableForSaleSecuritiesCurrent")),
            new ConceptMapping(UsFinancialConcept.RECEIVABLES, ConceptSource.USD, List.of(
                    "AccountsReceivableNetCurrent", "ReceivablesNetCurrent")),
            new ConceptMapping(UsFinancialConcept.INVENTORY, ConceptSource.USD, List.of("InventoryNet")),
            new ConceptMapping(UsFinancialConcept.INVESTMENTS_NONCURRENT, ConceptSource.USD, List.of(
                    "MarketableSecuritiesNoncurrent", "LongTermInvestments",
                    "AvailableForSaleSecuritiesNoncurrent")),
            new ConceptMapping(UsFinancialConcept.TANGIBLE_ASSETS, ConceptSource.USD, List.of(
                    "PropertyPlantAndEquipmentNet")),
            new ConceptMapping(UsFinancialConcept.GOODWILL, ConceptSource.USD, List.of("Goodwill")),
            new ConceptMapping(UsFinancialConcept.INTANGIBLE_ASSETS, ConceptSource.USD, List.of(
                    "IntangibleAssetsNetExcludingGoodwill", "FiniteLivedIntangibleAssetsNet")),
            new ConceptMapping(UsFinancialConcept.DEBT_NONCURRENT, ConceptSource.USD, List.of(
                    "LongTermDebtNoncurrent")),
            new ConceptMapping(UsFinancialConcept.DEBT_CURRENT, ConceptSource.USD, List.of(
                    "LongTermDebtCurrent", "DebtCurrent")),
            new ConceptMapping(UsFinancialConcept.SHORT_TERM_BORROWINGS, ConceptSource.USD, List.of(
                    "CommercialPaper", "ShortTermBorrowings")),
            new ConceptMapping(UsFinancialConcept.EPS_DILUTED, ConceptSource.PER_SHARE, List.of(
                    "EarningsPerShareDiluted")),
            new ConceptMapping(UsFinancialConcept.EPS_BASIC, ConceptSource.PER_SHARE, List.of(
                    "EarningsPerShareBasic")),
            new ConceptMapping(UsFinancialConcept.DPS_DECLARED, ConceptSource.PER_SHARE, List.of(
                    "CommonStockDividendsPerShareDeclared", "CommonStockDividendsPerShareCashPaid")),
            new ConceptMapping(UsFinancialConcept.DIVIDENDS_PAID, ConceptSource.USD, List.of(
                    "PaymentsOfDividends", "PaymentsOfDividendsCommonStock")),
            new ConceptMapping(UsFinancialConcept.SHARES_OUTSTANDING, ConceptSource.SHARES, List.of(
                    "EntityCommonStockSharesOutstanding", "CommonStockSharesOutstanding")));

    private enum ConceptSource { USD, PER_SHARE, SHARES }

    private record ConceptMapping(UsFinancialConcept concept, ConceptSource source, List<String> tags) {}

    /**
     * 파싱된 Company Facts (캐시 대상)
     */
    record ParsedCompanyFacts(
            Map<String, Map<Integer, Double>> usdData,
            Map<String, Map<Integer, Double>> sharesData,
            Map<String, Map<Integer, Double>> shareCountData,
            List<Integer> recentYears,
            Map<String, Map<String, Double>> quarterlyUsdData,
            Map<String, Map<String, Double>> quarterlySharesData,
            List<String> recentQuarters
    ) {}
}
