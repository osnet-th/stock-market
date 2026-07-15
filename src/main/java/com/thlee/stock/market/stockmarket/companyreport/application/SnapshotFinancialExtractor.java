package com.thlee.stock.market.stockmarket.companyreport.application;

import com.thlee.stock.market.stockmarket.companyreport.application.dto.ReportSnapshot.BreakdownTerm;
import com.thlee.stock.market.stockmarket.companyreport.application.dto.ReportSnapshot.MetricBreakdown;
import com.thlee.stock.market.stockmarket.companyreport.application.dto.ReportSnapshot.MetricRow;
import com.thlee.stock.market.stockmarket.companyreport.application.dto.ReportSnapshot.PriceMetrics;
import com.thlee.stock.market.stockmarket.companyreport.application.dto.ReportSnapshot.RatioRow;
import com.thlee.stock.market.stockmarket.companyreport.application.dto.ReportSnapshot.RiskSignals;
import com.thlee.stock.market.stockmarket.companyreport.application.dto.ReportSnapshot.UnclassifiedLine;
import com.thlee.stock.market.stockmarket.companyreport.application.dto.ReportSnapshot.ValuationInputs;
import com.thlee.stock.market.stockmarket.stock.application.dto.FinancialTimelineResponse;
import com.thlee.stock.market.stockmarket.stock.application.dto.FinancialTimelineResponse.TimelineColumn;
import com.thlee.stock.market.stockmarket.stock.application.dto.FinancialTimelineResponse.TimelineDetailGroup;
import com.thlee.stock.market.stockmarket.stock.application.dto.FinancialTimelineResponse.TimelineDetailNode;
import com.thlee.stock.market.stockmarket.stock.application.dto.FinancialTimelineResponse.TimelineRow;
import com.thlee.stock.market.stockmarket.stock.application.dto.StockQuantityResponse;
import com.thlee.stock.market.stockmarket.stock.application.dto.ValuationMetricResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * 타임라인 응답에서 스냅샷 구성 요소(실적/재무제표 요약/지표 판정/가치평가 입력/위험 시그널)를 추출한다.
 * 계정 매칭은 IFRS account_id 우선, 계정명(공백 제거) 폴백 — 미검출 값은 null (0 대체 금지).
 */
@Component
public class SnapshotFinancialExtractor {

    private static final String BS = "BS";
    private static final String CF = "CF";
    private static final String IS = "IS";

    private static final String CURRENT_ASSETS_ID = "ifrs-full_CurrentAssets";
    private static final String NONCURRENT_ASSETS_ID = "ifrs-full_NoncurrentAssets";
    private static final String CURRENT_LIABILITIES_ID = "ifrs-full_CurrentLiabilities";
    private static final String NONCURRENT_LIABILITIES_ID = "ifrs-full_NoncurrentLiabilities";
    private static final String OPERATING_CF_ID = "ifrs-full_CashFlowsFromUsedInOperatingActivities";
    private static final String INVESTING_CF_ID = "ifrs-full_CashFlowsFromUsedInInvestingActivities";
    private static final String FINANCING_CF_ID = "ifrs-full_CashFlowsFromUsedInFinancingActivities";
    private static final String COST_OF_SALES_ID = "ifrs-full_CostOfSales";

    /** 급증 판정: 매출 증가율 대비 초과 허용폭 (%p) */
    private static final BigDecimal SURGE_MARGIN = new BigDecimal("20");

    // === 컬럼/기준연도 ===

    public String latestAnnualYear(FinancialTimelineResponse timeline) {
        return timeline.getColumns().stream()
                .filter(column -> !column.isPartial())
                .map(TimelineColumn::getYear)
                .max(Comparator.naturalOrder())
                .orElseGet(() -> latestYear(timeline));
    }

    private String latestYear(FinancialTimelineResponse timeline) {
        return timeline.getColumns().stream()
                .map(TimelineColumn::getYear)
                .max(Comparator.naturalOrder())
                .orElse(null);
    }

    // === 실적 추이 ===

    public List<MetricRow> performanceRows(FinancialTimelineResponse timeline) {
        Map<String, BigDecimal> revenue = summarySeries(timeline, "매출액");
        Map<String, BigDecimal> operating = summarySeries(timeline, "영업이익");
        Map<String, BigDecimal> net = summarySeries(timeline, "당기순이익");
        Map<String, BigDecimal> operatingCf = detailSeries(timeline, CF, OPERATING_CF_ID, "영업활동");
        List<MetricRow> rows = new ArrayList<>(amountPerformanceRows(timeline, revenue, operating, net, operatingCf));
        rows.addAll(ratioPerformanceRows(timeline, revenue, operating, net));
        return rows;
    }

    private List<MetricRow> amountPerformanceRows(FinancialTimelineResponse timeline,
            Map<String, BigDecimal> revenue, Map<String, BigDecimal> operating,
            Map<String, BigDecimal> net, Map<String, BigDecimal> operatingCf) {
        return List.of(
                row("revenue", "매출액", revenue),
                row("operatingProfit", "영업이익", operating),
                row("netIncome", "당기순이익", net),
                row("operatingCf", "영업활동현금흐름", operatingCf),
                row("fcf", "잉여현금흐름(FCF)", fcfSeries(timeline)));
    }

    private List<MetricRow> ratioPerformanceRows(FinancialTimelineResponse timeline,
            Map<String, BigDecimal> revenue, Map<String, BigDecimal> operating, Map<String, BigDecimal> net) {
        return List.of(
                row("costRatio", "매출원가율(%)",
                        percentSeries(detailSeries(timeline, IS, COST_OF_SALES_ID, "매출원가"), revenue)),
                row("sgaRatio", "판매관리비율(%)",
                        percentSeries(detailSeries(timeline, IS, null, "판매비와관리비"), revenue)),
                row("operatingMargin", "영업이익률(%)", percentSeries(operating, revenue)),
                row("netMargin", "순이익률(%)", percentSeries(net, revenue)));
    }

    // === 재무제표 요약 ===

    public List<MetricRow> statementRows(FinancialTimelineResponse timeline) {
        List<MetricRow> rows = new ArrayList<>(balanceSheetRows(timeline));
        rows.addAll(incomeStatementRows(timeline));
        rows.addAll(cashFlowRows(timeline));
        return rows;
    }

    private List<MetricRow> balanceSheetRows(FinancialTimelineResponse timeline) {
        return List.of(
                summaryRow(timeline, "bs.currentAssets", "유동자산", "유동자산"),
                summaryRow(timeline, "bs.nonCurrentAssets", "비유동자산", "비유동자산"),
                summaryRow(timeline, "bs.totalAssets", "자산총계", "자산총계"),
                summaryRow(timeline, "bs.currentLiabilities", "유동부채", "유동부채"),
                summaryRow(timeline, "bs.nonCurrentLiabilities", "비유동부채", "비유동부채"),
                summaryRow(timeline, "bs.totalLiabilities", "부채총계", "부채총계"),
                summaryRow(timeline, "bs.capitalStock", "자본금", "자본금"),
                summaryRow(timeline, "bs.retainedEarnings", "이익잉여금", "이익잉여금"),
                summaryRow(timeline, "bs.totalEquity", "자본총계", "자본총계"));
    }

    private List<MetricRow> incomeStatementRows(FinancialTimelineResponse timeline) {
        return List.of(
                summaryRow(timeline, "is.revenue", "매출액", "매출액"),
                summaryRow(timeline, "is.operatingProfit", "영업이익", "영업이익"),
                summaryRow(timeline, "is.pretaxIncome", "법인세차감전 순이익", "법인세차감전순이익"),
                summaryRow(timeline, "is.netIncome", "당기순이익", "당기순이익"));
    }

    private List<MetricRow> cashFlowRows(FinancialTimelineResponse timeline) {
        return List.of(
                row("cf.operating", "영업활동현금흐름", detailSeries(timeline, CF, OPERATING_CF_ID, "영업활동")),
                row("cf.investing", "투자활동현금흐름", detailSeries(timeline, CF, INVESTING_CF_ID, "투자활동")),
                row("cf.financing", "재무활동현금흐름", detailSeries(timeline, CF, FINANCING_CF_ID, "재무활동")),
                row("cf.fcf", "잉여현금흐름(FCF)", fcfSeries(timeline)));
    }

    // === 재무지표 (기준치 판정 포함) ===

    public List<RatioRow> ratioRows(FinancialTimelineResponse timeline, String baseYear) {
        List<RatioRow> rows = new ArrayList<>(healthRatioRows(timeline, baseYear));
        rows.addAll(profitabilityRatioRows(timeline, baseYear));
        rows.addAll(growthRatioRows(timeline, baseYear));
        return rows;
    }

    private List<RatioRow> healthRatioRows(FinancialTimelineResponse timeline, String baseYear) {
        Map<String, BigDecimal> equityRatio =
                percentSeries(summarySeries(timeline, "자본총계"), summarySeries(timeline, "자산총계"));
        Map<String, BigDecimal> currentRatio =
                percentSeries(summarySeries(timeline, "유동자산"), summarySeries(timeline, "유동부채"));
        Map<String, BigDecimal> debtRatio =
                percentSeries(summarySeries(timeline, "부채총계"), summarySeries(timeline, "자본총계"));
        return List.of(
                ratioRow("equityRatio", "자기자본비율(%)", "health", equityRatio,
                        judge(equityRatio.get(baseYear), new BigDecimal("40"), new BigDecimal("20"))),
                ratioRow("currentRatio", "유동비율(%)", "health", currentRatio,
                        judge(currentRatio.get(baseYear), new BigDecimal("100"), new BigDecimal("100"))),
                ratioRow("debtRatio", "부채비율(%)", "health", debtRatio,
                        judgeInverse(debtRatio.get(baseYear), new BigDecimal("200"), new BigDecimal("300"))));
    }

    private List<RatioRow> profitabilityRatioRows(FinancialTimelineResponse timeline, String baseYear) {
        Map<String, BigDecimal> revenue = summarySeries(timeline, "매출액");
        Map<String, BigDecimal> net = summarySeries(timeline, "당기순이익");
        Map<String, BigDecimal> operatingMargin = percentSeries(summarySeries(timeline, "영업이익"), revenue);
        Map<String, BigDecimal> roe = percentSeries(net, summarySeries(timeline, "자본총계"));
        Map<String, BigDecimal> roa = percentSeries(net, summarySeries(timeline, "자산총계"));
        return List.of(
                ratioRow("operatingMargin", "영업이익률(%)", "profitability", operatingMargin,
                        judge(operatingMargin.get(baseYear), new BigDecimal("5"), BigDecimal.ZERO)),
                ratioRow("roe", "ROE(%)", "profitability", roe,
                        judge(roe.get(baseYear), new BigDecimal("10"), new BigDecimal("5"))),
                ratioRow("roa", "ROA(%)", "profitability", roa,
                        judge(roa.get(baseYear), new BigDecimal("5"), BigDecimal.ZERO)));
    }

    private List<RatioRow> growthRatioRows(FinancialTimelineResponse timeline, String baseYear) {
        Map<String, BigDecimal> revenueGrowth = growthSeries(summarySeries(timeline, "매출액"));
        Map<String, BigDecimal> operatingGrowth = growthSeries(summarySeries(timeline, "영업이익"));
        return List.of(
                ratioRow("revenueGrowth", "매출 성장률(%)", "growth", revenueGrowth,
                        judge(revenueGrowth.get(baseYear), new BigDecimal("10"), BigDecimal.ZERO)),
                ratioRow("operatingProfitGrowth", "영업이익 성장률(%)", "growth", operatingGrowth,
                        judge(operatingGrowth.get(baseYear), new BigDecimal("10"), BigDecimal.ZERO)));
    }

    /**
     * 높을수록 좋은 지표: goodMin 이상 good, riskMax 이하 risk, 사이 warn
     */
    private String judge(BigDecimal value, BigDecimal goodMin, BigDecimal riskMax) {
        if (value == null) {
            return null;
        }
        if (value.compareTo(goodMin) >= 0) {
            return "good";
        }
        return value.compareTo(riskMax) <= 0 ? "risk" : "warn";
    }

    /**
     * 낮을수록 좋은 지표(부채비율): goodMax 이하 good, riskMin 초과 risk, 사이 warn
     */
    private String judgeInverse(BigDecimal value, BigDecimal goodMax, BigDecimal riskMin) {
        if (value == null) {
            return null;
        }
        if (value.compareTo(goodMax) <= 0) {
            return "good";
        }
        return value.compareTo(riskMin) > 0 ? "risk" : "warn";
    }

    // === 주가지표 ===

    public PriceMetrics priceMetrics(FinancialTimelineResponse timeline, String baseYear,
            ValuationMetricResponse valuation, List<StockQuantityResponse> quantities) {
        BigDecimal shares = distributedShares(quantities);
        BigDecimal referencePrice = valuation == null ? null : valuation.getReferencePrice();
        BigDecimal marketCap = (referencePrice != null && shares != null) ? referencePrice.multiply(shares) : null;
        BigDecimal revenue = summarySeries(timeline, "매출액").get(baseYear);
        BigDecimal operatingCf = detailSeries(timeline, CF, OPERATING_CF_ID, "영업활동").get(baseYear);
        return buildPriceMetrics(timeline, baseYear, valuation, marketCap, revenue, operatingCf, shares);
    }

    private PriceMetrics buildPriceMetrics(FinancialTimelineResponse timeline, String baseYear,
            ValuationMetricResponse valuation, BigDecimal marketCap, BigDecimal revenue, BigDecimal operatingCf,
            BigDecimal shares) {
        BigDecimal referencePrice = valuation == null ? null : valuation.getReferencePrice();
        BigDecimal eps = valuation == null ? null : valuation.getEps();
        BigDecimal bps = valuation == null ? null : valuation.getBps();
        BigDecimal per = valuation == null ? null : valuation.getPer();
        BigDecimal pbr = valuation == null ? null : valuation.getPbr();
        // EPS/PER 폴백: 기존 ValuationMetricService가 계정명 정확일치 실패로 EPS를 못 구한 경우,
        // 재무제표 요약과 동일한 당기순이익(account_id 매칭)과 유통주식수로 보정한다.
        if (eps == null && shares != null && shares.signum() > 0) {
            BigDecimal net = summarySeries(timeline, "당기순이익").get(baseYear);
            if (net != null) {
                eps = net.divide(shares, 0, RoundingMode.HALF_UP);
            }
        }
        if (per == null) {
            per = divide(referencePrice, eps);
        }
        BigDecimal psr = divide(marketCap, revenue);
        BigDecimal pcfr = divide(marketCap, positiveOrNull(operatingCf));
        BigDecimal perTimesPbr = multiply(per, pbr);
        BigDecimal evEbitda = evEbitda(timeline, baseYear, marketCap);
        BigDecimal roic = roic(timeline, baseYear);
        BigDecimal accrualRatio = accrualRatio(timeline, baseYear, operatingCf);
        Map<String, MetricBreakdown> breakdowns = buildBreakdowns(timeline, baseYear, referencePrice, shares,
                marketCap, revenue, operatingCf, eps, bps, per, pbr, psr, pcfr, perTimesPbr, evEbitda, roic,
                accrualRatio);
        return new PriceMetrics(
                valuation == null ? null : valuation.getTermName(),
                valuation == null ? null : valuation.getReferencePriceDate(),
                referencePrice, marketCap, eps, bps, per, pbr, psr, pcfr, perTimesPbr,
                evEbitda, roic, accrualRatio,
                valuation == null ? List.of() : valuation.getWarnings(),
                breakdowns);
    }

    /**
     * 주가지표 계산 근거(툴팁 표시용) 조립. 근거를 완전히 구성할 수 없는 지표는 map에서 생략한다.
     */
    private Map<String, MetricBreakdown> buildBreakdowns(FinancialTimelineResponse timeline, String baseYear,
            BigDecimal referencePrice, BigDecimal shares, BigDecimal marketCap, BigDecimal revenue,
            BigDecimal operatingCf, BigDecimal eps, BigDecimal bps, BigDecimal per, BigDecimal pbr,
            BigDecimal psr, BigDecimal pcfr, BigDecimal perTimesPbr, BigDecimal evEbitda, BigDecimal roic,
            BigDecimal accrualRatio) {
        BigDecimal net = summarySeries(timeline, "당기순이익").get(baseYear);
        BigDecimal equity = summarySeries(timeline, "자본총계").get(baseYear);
        BigDecimal totalAssets = summarySeries(timeline, "자산총계").get(baseYear);
        BigDecimal operating = summarySeries(timeline, "영업이익").get(baseYear);
        BigDecimal amortization = depreciationAndAmortization(timeline, baseYear);
        BigDecimal borrowings = borrowings(timeline, baseYear);
        BigDecimal cashSec = cashAndSecurities(timeline, baseYear);

        Map<String, MetricBreakdown> map = new LinkedHashMap<>();
        putBreakdown(map, "marketCap", marketCap, Arrays.asList(
                term(null, "현재가", referencePrice, "price"),
                term("×", "유통주식수", shares, "shares")), null);
        putBreakdown(map, "eps", eps, Arrays.asList(
                term(null, "당기순이익", net, "amount"),
                term("÷", "유통주식수", shares, "shares")), null);
        putBreakdown(map, "bps", bps, Arrays.asList(
                term(null, "자본총계", equity, "amount"),
                term("÷", "유통주식수", shares, "shares")), null);
        putBreakdown(map, "per", per, Arrays.asList(
                term(null, "현재가", referencePrice, "price"),
                term("÷", "EPS", eps, "price")), null);
        putBreakdown(map, "pbr", pbr, Arrays.asList(
                term(null, "현재가", referencePrice, "price"),
                term("÷", "BPS", bps, "price")), null);
        putBreakdown(map, "psr", psr, Arrays.asList(
                term(null, "시가총액", marketCap, "amount"),
                term("÷", "매출액", revenue, "amount")), null);
        putBreakdown(map, "pcfr", pcfr, Arrays.asList(
                term(null, "시가총액", marketCap, "amount"),
                term("÷", "영업활동현금흐름", operatingCf, "amount")), null);
        putBreakdown(map, "perTimesPbr", perTimesPbr, Arrays.asList(
                term(null, "PER", per, "x"),
                term("×", "PBR", pbr, "x")), null);
        putEvEbitda(map, evEbitda, marketCap, borrowings, cashSec, operating, amortization);
        putRoic(map, roic, operating, equity, borrowings, cashSec, timeline, baseYear);
        putAccrual(map, accrualRatio, net, operatingCf, totalAssets);
        return map.isEmpty() ? null : map;
    }

    private BreakdownTerm term(String op, String label, BigDecimal value, String unit) {
        return value == null ? null : new BreakdownTerm(op, label, value, unit);
    }

    /**
     * result가 없거나 식 항 중 하나라도 결손이면 근거를 생략한다 (프론트는 공식만 표시).
     */
    private void putBreakdown(Map<String, MetricBreakdown> map, String key, BigDecimal result,
            List<BreakdownTerm> terms, List<BreakdownTerm> extras) {
        if (result == null || terms.stream().anyMatch(Objects::isNull)) {
            return;
        }
        List<BreakdownTerm> cleanExtras = extras == null ? null
                : extras.stream().filter(Objects::nonNull).toList();
        map.put(key, new MetricBreakdown(terms, result, cleanExtras));
    }

    private void putEvEbitda(Map<String, MetricBreakdown> map, BigDecimal evEbitda, BigDecimal marketCap,
            BigDecimal borrowings, BigDecimal cashSec, BigDecimal operating, BigDecimal amortization) {
        if (evEbitda == null || marketCap == null || operating == null || amortization == null) {
            return;
        }
        BigDecimal ev = marketCap.add(orZero(borrowings)).subtract(orZero(cashSec));
        BigDecimal ebitda = operating.add(amortization);
        map.put("evEbitda", new MetricBreakdown(
                Arrays.asList(term(null, "EV", ev, "amount"), term("÷", "EBITDA", ebitda, "amount")),
                evEbitda,
                Arrays.asList(term(null, "시가총액", marketCap, "amount"), term(null, "차입금", borrowings, "amount"),
                        term(null, "현금성·단기금융", cashSec, "amount"), term(null, "영업이익", operating, "amount"),
                        term(null, "상각비", amortization, "amount")).stream().filter(Objects::nonNull).toList()));
    }

    private void putRoic(Map<String, MetricBreakdown> map, BigDecimal roic, BigDecimal operating,
            BigDecimal equity, BigDecimal borrowings, BigDecimal cashSec,
            FinancialTimelineResponse timeline, String baseYear) {
        if (roic == null || operating == null || equity == null) {
            return;
        }
        BigDecimal taxRate = effectiveTaxRate(timeline, baseYear);
        BigDecimal nopat = operating.multiply(BigDecimal.ONE.subtract(taxRate));
        BigDecimal investedCapital = equity.add(orZero(borrowings)).subtract(orZero(cashSec));
        map.put("roic", new MetricBreakdown(
                Arrays.asList(term(null, "NOPAT", nopat, "amount"), term("÷", "투하자본", investedCapital, "amount")),
                roic,
                Arrays.asList(term(null, "영업이익", operating, "amount"),
                        term(null, "실효세율", taxRate.multiply(new BigDecimal("100")), "pct"),
                        term(null, "자본총계", equity, "amount"), term(null, "차입금", borrowings, "amount"),
                        term(null, "현금성·단기금융", cashSec, "amount")).stream().filter(Objects::nonNull).toList()));
    }

    private void putAccrual(Map<String, MetricBreakdown> map, BigDecimal accrualRatio,
            BigDecimal net, BigDecimal operatingCf, BigDecimal totalAssets) {
        if (accrualRatio == null || net == null || operatingCf == null || totalAssets == null) {
            return;
        }
        map.put("accrual", new MetricBreakdown(
                Arrays.asList(term(null, "당기순이익 − 영업현금흐름", net.subtract(operatingCf), "amount"),
                        term("÷", "자산총계", totalAssets, "amount")),
                accrualRatio,
                Arrays.asList(term(null, "당기순이익", net, "amount"),
                        term(null, "영업활동현금흐름", operatingCf, "amount"))));
    }

    private BigDecimal distributedShares(List<StockQuantityResponse> quantities) {
        if (quantities == null) {
            return null;
        }
        return findShares(quantities, "보통주")
                .or(() -> findShares(quantities, "합계"))
                .orElse(null);
    }

    private java.util.Optional<BigDecimal> findShares(List<StockQuantityResponse> quantities, String category) {
        return quantities.stream()
                .filter(quantity -> quantity.getCategory() != null && quantity.getCategory().contains(category))
                .map(quantity -> parse(quantity.getDistributedStockCount()))
                .filter(Objects::nonNull)
                .findFirst();
    }

    /**
     * EV/EBITDA(근사). EV = 시총 + 차입금 − (현금성 + 단기금융), EBITDA = 영업이익 + 상각비(CF 조정항목 합)
     */
    private BigDecimal evEbitda(FinancialTimelineResponse timeline, String baseYear, BigDecimal marketCap) {
        BigDecimal ebitda = ebitda(timeline, baseYear);
        if (marketCap == null || ebitda == null || ebitda.signum() <= 0) {
            return null;
        }
        BigDecimal ev = marketCap.add(orZero(borrowings(timeline, baseYear)))
                .subtract(orZero(cashAndSecurities(timeline, baseYear)));
        return ev.divide(ebitda, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal ebitda(FinancialTimelineResponse timeline, String baseYear) {
        BigDecimal operating = summarySeries(timeline, "영업이익").get(baseYear);
        BigDecimal amortization = depreciationAndAmortization(timeline, baseYear);
        if (operating == null || amortization == null) {
            return null;
        }
        return operating.add(amortization);
    }

    private BigDecimal depreciationAndAmortization(FinancialTimelineResponse timeline, String baseYear) {
        BigDecimal sum = detailRowsOf(timeline, CF).stream()
                .filter(this::isAmortizationRow)
                .map(row -> parse(row.getValues().get(baseYear)))
                .filter(Objects::nonNull)
                .map(BigDecimal::abs)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.signum() > 0 ? sum : null;
    }

    private boolean isAmortizationRow(TimelineRow row) {
        String name = normalize(row.getName());
        return name.contains("상각") && !name.contains("상환");
    }

    /**
     * ROIC(근사) = 영업이익 × (1 − 실효세율) / (자본총계 + 차입금 − 현금성·단기금융)
     */
    private BigDecimal roic(FinancialTimelineResponse timeline, String baseYear) {
        BigDecimal operating = summarySeries(timeline, "영업이익").get(baseYear);
        BigDecimal investedCapital = investedCapital(timeline, baseYear);
        if (operating == null || investedCapital == null || investedCapital.signum() <= 0) {
            return null;
        }
        BigDecimal nopat = operating.multiply(BigDecimal.ONE.subtract(effectiveTaxRate(timeline, baseYear)));
        return nopat.divide(investedCapital, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal investedCapital(FinancialTimelineResponse timeline, String baseYear) {
        BigDecimal equity = summarySeries(timeline, "자본총계").get(baseYear);
        if (equity == null) {
            return null;
        }
        return equity.add(orZero(borrowings(timeline, baseYear)))
                .subtract(orZero(cashAndSecurities(timeline, baseYear)));
    }

    /**
     * 실효세율 = (법인세차감전 − 당기순이익) / 법인세차감전. 0~50% 벗어나면 25% 대체
     */
    private BigDecimal effectiveTaxRate(FinancialTimelineResponse timeline, String baseYear) {
        BigDecimal pretax = summarySeries(timeline, "법인세차감전순이익").get(baseYear);
        BigDecimal net = summarySeries(timeline, "당기순이익").get(baseYear);
        if (pretax == null || net == null || pretax.signum() <= 0) {
            return new BigDecimal("0.25");
        }
        BigDecimal rate = pretax.subtract(net).divide(pretax, 4, RoundingMode.HALF_UP);
        boolean valid = rate.compareTo(BigDecimal.ZERO) >= 0 && rate.compareTo(new BigDecimal("0.5")) <= 0;
        return valid ? rate : new BigDecimal("0.25");
    }

    /**
     * 발생액/총자산(%) = (당기순이익 − 영업CF) / 자산총계 × 100
     */
    private BigDecimal accrualRatio(FinancialTimelineResponse timeline, String baseYear, BigDecimal operatingCf) {
        BigDecimal net = summarySeries(timeline, "당기순이익").get(baseYear);
        BigDecimal totalAssets = summarySeries(timeline, "자산총계").get(baseYear);
        if (net == null || operatingCf == null || totalAssets == null || totalAssets.signum() <= 0) {
            return null;
        }
        return net.subtract(operatingCf)
                .divide(totalAssets, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP);
    }

    // === 가치평가 입력 (청산가치/DCF 원시 데이터) ===

    public ValuationInputs valuationInputs(FinancialTimelineResponse timeline, String baseYear) {
        Map<Category, BigDecimal> sums = new EnumMap<>(Category.class);
        List<UnclassifiedLine> unclassified = new ArrayList<>();
        classifyChildren(timeline, baseYear, CURRENT_ASSETS_ID, this::classifyCurrent, sums, unclassified);
        classifyChildren(timeline, baseYear, NONCURRENT_ASSETS_ID, this::classifyNonCurrent, sums, unclassified);
        return buildValuationInputs(timeline, baseYear, sums, unclassified);
    }

    private ValuationInputs buildValuationInputs(FinancialTimelineResponse timeline, String baseYear,
            Map<Category, BigDecimal> sums, List<UnclassifiedLine> unclassified) {
        BigDecimal borrowings = borrowings(timeline, baseYear);
        BigDecimal netCash = netCash(sums, borrowings);
        return new ValuationInputs(baseYear,
                sums.get(Category.CASH), sums.get(Category.SECURITIES), sums.get(Category.RECEIVABLES),
                sums.get(Category.INVENTORY), sums.get(Category.INVESTMENTS), sums.get(Category.TANGIBLE),
                sums.get(Category.INTANGIBLE), sums.get(Category.OTHER_CURRENT), sums.get(Category.OTHER_NON_CURRENT),
                summarySeries(timeline, "부채총계").get(baseYear),
                borrowings, netCash, fcfSeries(timeline).get(baseYear), unclassified);
    }

    private BigDecimal netCash(Map<Category, BigDecimal> sums, BigDecimal borrowings) {
        BigDecimal cash = sums.get(Category.CASH);
        BigDecimal securities = sums.get(Category.SECURITIES);
        if (cash == null && securities == null) {
            return null;
        }
        return orZero(cash).add(orZero(securities)).subtract(orZero(borrowings));
    }

    private void classifyChildren(FinancialTimelineResponse timeline, String baseYear, String anchorId,
            Function<String, Category> classifier, Map<Category, BigDecimal> sums, List<UnclassifiedLine> unclassified) {
        for (TimelineRow child : anchorChildren(timeline, BS, anchorId)) {
            BigDecimal value = parse(child.getValues().get(baseYear));
            if (value == null) {
                continue;
            }
            accumulate(sums, unclassified, classifier.apply(normalize(child.getName())), child.getName(), value);
        }
    }

    private void accumulate(Map<Category, BigDecimal> sums, List<UnclassifiedLine> unclassified,
            Category category, String accountName, BigDecimal value) {
        sums.merge(category, value, BigDecimal::add);
        if (category == Category.OTHER_CURRENT || category == Category.OTHER_NON_CURRENT) {
            unclassified.add(new UnclassifiedLine(accountName, value, category.bucket));
        }
    }

    private Category classifyCurrent(String name) {
        if (name.contains("현금및현금성")) {
            return Category.CASH;
        }
        if (name.contains("단기금융") || name.contains("유가증권") || name.contains("단기투자") || name.contains("금융자산")) {
            return Category.SECURITIES;
        }
        if (name.contains("매출채권") || name.contains("수취채권")) {
            return Category.RECEIVABLES;
        }
        return name.contains("재고") ? Category.INVENTORY : Category.OTHER_CURRENT;
    }

    private Category classifyNonCurrent(String name) {
        if (name.contains("투자") || name.contains("관계기업") || name.contains("종속기업") || name.contains("공동기업")) {
            return Category.INVESTMENTS;
        }
        if (name.contains("유형자산")) {
            return Category.TANGIBLE;
        }
        if (name.contains("무형자산") || name.contains("영업권")) {
            return Category.INTANGIBLE;
        }
        return name.contains("금융자산") ? Category.INVESTMENTS : Category.OTHER_NON_CURRENT;
    }

    private enum Category {
        CASH(null), SECURITIES(null), RECEIVABLES(null), INVENTORY(null), INVESTMENTS(null),
        TANGIBLE(null), INTANGIBLE(null), OTHER_CURRENT("otherCurrent"), OTHER_NON_CURRENT("otherNonCurrent");

        private final String bucket;

        Category(String bucket) {
            this.bucket = bucket;
        }
    }

    /**
     * 차입금(근사) = 유동/비유동 부채 하위 계정 중 차입금·사채 합계
     */
    private BigDecimal borrowings(FinancialTimelineResponse timeline, String baseYear) {
        List<TimelineRow> children = new ArrayList<>(anchorChildren(timeline, BS, CURRENT_LIABILITIES_ID));
        children.addAll(anchorChildren(timeline, BS, NONCURRENT_LIABILITIES_ID));
        BigDecimal sum = children.stream()
                .filter(row -> isBorrowingRow(normalize(row.getName())))
                .map(row -> parse(row.getValues().get(baseYear)))
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.signum() != 0 ? sum : null;
    }

    private boolean isBorrowingRow(String name) {
        return name.contains("차입금") || name.contains("사채");
    }

    private BigDecimal cashAndSecurities(FinancialTimelineResponse timeline, String baseYear) {
        BigDecimal sum = anchorChildren(timeline, BS, CURRENT_ASSETS_ID).stream()
                .filter(row -> isCashLikeRow(normalize(row.getName())))
                .map(row -> parse(row.getValues().get(baseYear)))
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.signum() != 0 ? sum : null;
    }

    private boolean isCashLikeRow(String name) {
        return classifyCurrent(name) == Category.CASH || classifyCurrent(name) == Category.SECURITIES;
    }

    // === 위험 시그널 ===

    public RiskSignals riskSignals(FinancialTimelineResponse timeline, String baseYear, Boolean capitalIncrease) {
        BigDecimal currentAssets = summarySeries(timeline, "유동자산").get(baseYear);
        BigDecimal currentLiabilities = summarySeries(timeline, "유동부채").get(baseYear);
        BigDecimal equity = summarySeries(timeline, "자본총계").get(baseYear);
        BigDecimal equityRatio = percentSeries(summarySeries(timeline, "자본총계"),
                summarySeries(timeline, "자산총계")).get(baseYear);
        Map<String, String> evidence = new LinkedHashMap<>();
        return new RiskSignals(
                exceeds(currentLiabilities, currentAssets, "유동부채>유동자산", evidence),
                atMost(equityRatio, new BigDecimal("20"), "자기자본비율(%)", evidence),
                negative(equity, "자본총계", evidence),
                surge(timeline, baseYear, Category.RECEIVABLES, "매출채권", evidence),
                surge(timeline, baseYear, Category.INVENTORY, "재고자산", evidence),
                capitalIncrease,
                evidence);
    }

    private Boolean exceeds(BigDecimal left, BigDecimal right, String label, Map<String, String> evidence) {
        if (left == null || right == null) {
            return null;
        }
        evidence.put(label, left.toPlainString() + " / " + right.toPlainString());
        return left.compareTo(right) > 0;
    }

    private Boolean atMost(BigDecimal value, BigDecimal threshold, String label, Map<String, String> evidence) {
        if (value == null) {
            return null;
        }
        evidence.put(label, value.toPlainString());
        return value.compareTo(threshold) <= 0;
    }

    private Boolean negative(BigDecimal value, String label, Map<String, String> evidence) {
        if (value == null) {
            return null;
        }
        evidence.put(label, value.toPlainString());
        return value.signum() < 0;
    }

    /**
     * 급증 판정: 해당 자산 증가율이 매출 증가율보다 SURGE_MARGIN(%p) 초과로 높으면 true
     */
    private Boolean surge(FinancialTimelineResponse timeline, String baseYear,
            Category category, String label, Map<String, String> evidence) {
        BigDecimal assetGrowth = categoryGrowth(timeline, baseYear, category);
        BigDecimal revenueGrowth = growthSeries(summarySeries(timeline, "매출액")).get(baseYear);
        if (assetGrowth == null || revenueGrowth == null) {
            return null;
        }
        evidence.put(label + " 증가율(%) / 매출 증가율(%)",
                assetGrowth.toPlainString() + " / " + revenueGrowth.toPlainString());
        return assetGrowth.subtract(revenueGrowth).compareTo(SURGE_MARGIN) > 0;
    }

    private BigDecimal categoryGrowth(FinancialTimelineResponse timeline, String baseYear, Category category) {
        Map<String, BigDecimal> series = categorySeries(timeline, category);
        return growthSeries(series).get(baseYear);
    }

    private Map<String, BigDecimal> categorySeries(FinancialTimelineResponse timeline, Category category) {
        Map<String, BigDecimal> series = new LinkedHashMap<>();
        for (TimelineColumn column : timeline.getColumns()) {
            BigDecimal sum = sumCategoryAt(timeline, category, column.getYear());
            if (sum != null) {
                series.put(column.getYear(), sum);
            }
        }
        return series;
    }

    private BigDecimal sumCategoryAt(FinancialTimelineResponse timeline, Category category, String year) {
        BigDecimal sum = anchorChildren(timeline, BS, CURRENT_ASSETS_ID).stream()
                .filter(row -> classifyCurrent(normalize(row.getName())) == category)
                .map(row -> parse(row.getValues().get(year)))
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.signum() != 0 ? sum : null;
    }

    // === 시리즈/행 빌더 ===

    private MetricRow row(String key, String name, Map<String, BigDecimal> series) {
        Map<String, String> values = new LinkedHashMap<>();
        series.forEach((year, value) -> values.put(year, value.toPlainString()));
        return new MetricRow(key, name, values);
    }

    private RatioRow ratioRow(String key, String name, String category,
            Map<String, BigDecimal> series, String judgment) {
        Map<String, String> values = new LinkedHashMap<>();
        series.forEach((year, value) -> values.put(year, value.toPlainString()));
        return new RatioRow(key, name, category, values, judgment);
    }

    private MetricRow summaryRow(FinancialTimelineResponse timeline, String key, String name, String accountName) {
        return row(key, name, summarySeries(timeline, accountName));
    }

    /**
     * 요약 재무계정 시리즈 (계정명 공백 제거 후 startsWith 매칭)
     */
    private Map<String, BigDecimal> summarySeries(FinancialTimelineResponse timeline, String accountName) {
        List<TimelineRow> rows = timeline.getSummaryAccounts();
        if (rows == null) {
            return Map.of();
        }
        return rows.stream()
                .filter(row -> normalize(row.getName()).startsWith(accountName))
                .findFirst()
                .map(this::toSeries)
                .orElse(Map.of());
    }

    private Map<String, BigDecimal> detailSeries(FinancialTimelineResponse timeline,
            String statementDiv, String accountId, String nameContains) {
        return detailRowsOf(timeline, statementDiv).stream()
                .filter(row -> matches(row, accountId, nameContains))
                .findFirst()
                .map(this::toSeries)
                .orElse(Map.of());
    }

    private boolean matches(TimelineRow row, String accountId, String nameContains) {
        if (accountId != null && accountId.equals(row.getId())) {
            return true;
        }
        return nameContains != null && normalize(row.getName()).contains(nameContains.replace(" ", ""));
    }

    private Map<String, BigDecimal> toSeries(TimelineRow row) {
        Map<String, BigDecimal> series = new LinkedHashMap<>();
        row.getValues().forEach((year, value) -> {
            BigDecimal parsed = parse(value);
            if (parsed != null) {
                series.put(year, parsed);
            }
        });
        return series;
    }

    private Map<String, BigDecimal> fcfSeries(FinancialTimelineResponse timeline) {
        return timeline.getFcf() != null ? toSeries(timeline.getFcf()) : Map.of();
    }

    /**
     * 비율(%) 시리즈 = 분자/분모 × 100 (둘 다 있는 연도만).
     * 분모가 0 이하(예: 자본잠식 시 자본총계)면 비율이 무의미하므로 제외한다 — 부채비율/ROE 오판정 방지.
     */
    private Map<String, BigDecimal> percentSeries(Map<String, BigDecimal> numerator, Map<String, BigDecimal> denominator) {
        Map<String, BigDecimal> series = new LinkedHashMap<>();
        numerator.forEach((year, value) -> {
            BigDecimal denom = denominator.get(year);
            if (denom != null && denom.signum() > 0) {
                series.put(year, value.divide(denom, 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP));
            }
        });
        return series;
    }

    /**
     * 전년 대비 증가율(%) 시리즈 (전년 값이 있는 연도만)
     */
    private Map<String, BigDecimal> growthSeries(Map<String, BigDecimal> values) {
        Map<String, BigDecimal> series = new LinkedHashMap<>();
        values.forEach((year, value) -> {
            BigDecimal previous = previousYearValue(values, year);
            if (previous != null && previous.signum() != 0) {
                series.put(year, growth(value, previous));
            }
        });
        return series;
    }

    private BigDecimal growth(BigDecimal current, BigDecimal previous) {
        return current.subtract(previous)
                .divide(previous.abs(), 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal previousYearValue(Map<String, BigDecimal> values, String year) {
        try {
            return values.get(String.valueOf(Integer.parseInt(year) - 1));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // === 세부 계정 트리 접근 ===

    private List<TimelineRow> anchorChildren(FinancialTimelineResponse timeline, String statementDiv, String anchorId) {
        return nodesOf(timeline, statementDiv).stream()
                .filter(node -> anchorId.equals(node.getRow().getId()))
                .findFirst()
                .map(TimelineDetailNode::getChildren)
                .orElse(List.of());
    }

    private List<TimelineRow> detailRowsOf(FinancialTimelineResponse timeline, String statementDiv) {
        List<TimelineRow> rows = new ArrayList<>();
        for (TimelineDetailNode node : nodesOf(timeline, statementDiv)) {
            rows.add(node.getRow());
            rows.addAll(node.getChildren());
        }
        return rows;
    }

    private List<TimelineDetailNode> nodesOf(FinancialTimelineResponse timeline, String statementDiv) {
        if (timeline.getDetails() == null) {
            return List.of();
        }
        return timeline.getDetails().stream()
                .filter(group -> statementDiv.equals(group.getStatementDiv()))
                .findFirst()
                .map(TimelineDetailGroup::getNodes)
                .orElse(List.of());
    }

    // === 파싱 ===

    private BigDecimal parse(String amount) {
        if (amount == null || amount.isBlank() || "-".equals(amount.trim())) {
            return null;
        }
        try {
            return new BigDecimal(amount.replace(",", "").trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String normalize(String name) {
        return name == null ? "" : name.replace(" ", "");
    }

    private BigDecimal orZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private BigDecimal positiveOrNull(BigDecimal value) {
        return value != null && value.signum() > 0 ? value : null;
    }

    private BigDecimal divide(BigDecimal numerator, BigDecimal denominator) {
        if (numerator == null || denominator == null || denominator.signum() == 0) {
            return null;
        }
        return numerator.divide(denominator, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal multiply(BigDecimal left, BigDecimal right) {
        if (left == null || right == null) {
            return null;
        }
        return left.multiply(right).setScale(2, RoundingMode.HALF_UP);
    }
}
