package com.thlee.stock.market.stockmarket.companyreport.application;

import com.thlee.stock.market.stockmarket.companyreport.application.dto.ReportSnapshot.BreakdownTerm;
import com.thlee.stock.market.stockmarket.companyreport.application.dto.ReportSnapshot.ColumnMeta;
import com.thlee.stock.market.stockmarket.companyreport.application.dto.ReportSnapshot.DividendRow;
import com.thlee.stock.market.stockmarket.companyreport.application.dto.ReportSnapshot.MetricBreakdown;
import com.thlee.stock.market.stockmarket.companyreport.application.dto.ReportSnapshot.MetricRow;
import com.thlee.stock.market.stockmarket.companyreport.application.dto.ReportSnapshot.PriceMetrics;
import com.thlee.stock.market.stockmarket.companyreport.application.dto.ReportSnapshot.RatioRow;
import com.thlee.stock.market.stockmarket.companyreport.application.dto.ReportSnapshot.RiskSignals;
import com.thlee.stock.market.stockmarket.companyreport.application.dto.ReportSnapshot.UnclassifiedLine;
import com.thlee.stock.market.stockmarket.companyreport.application.dto.ReportSnapshot.ValuationInputs;
import com.thlee.stock.market.stockmarket.stock.domain.model.UsCompanyFacts;
import com.thlee.stock.market.stockmarket.stock.domain.model.UsFinancialConcept;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * SEC 연간 팩트(us-gaap/dei)에서 스냅샷 구성 요소를 추출한다.
 * 행 key·항목명·판정 임계값은 국내 {@link SnapshotFinancialExtractor}와 동일하게 유지해
 * 프론트가 국가 구분 없이 같은 렌더 경로를 쓴다. 미검출 값은 null (0 대체 금지).
 * 금액은 달러 단위 — 표시 포맷(억원 vs $B/M)은 프론트가 스냅샷 currency로 분기한다.
 */
@Component
public class UsSnapshotFinancialExtractor {

    /** 급증 판정: 매출 증가율 대비 초과 허용폭 (%p) — KR과 동일 */
    private static final BigDecimal SURGE_MARGIN = new BigDecimal("20");

    /** 발행주식수 증가(희석) 판정 임계값 (%) — 유상증자 시그널 대체 */
    private static final BigDecimal DILUTION_THRESHOLD = new BigDecimal("10");

    // === 컬럼/기준연도 ===

    public String latestAnnualYear(UsCompanyFacts facts) {
        List<String> years = facts.years();
        return years.isEmpty() ? null : years.get(years.size() - 1);
    }

    public List<ColumnMeta> columns(UsCompanyFacts facts) {
        return facts.years().stream()
                .map(year -> new ColumnMeta(year, "10-K", false))
                .toList();
    }

    // === 실적 추이 ===

    public List<MetricRow> performanceRows(UsCompanyFacts facts) {
        Map<String, BigDecimal> revenue = facts.series(UsFinancialConcept.REVENUE);
        Map<String, BigDecimal> operating = facts.series(UsFinancialConcept.OPERATING_INCOME);
        Map<String, BigDecimal> net = facts.series(UsFinancialConcept.NET_INCOME);
        Map<String, BigDecimal> operatingCf = facts.series(UsFinancialConcept.OPERATING_CF);
        List<MetricRow> rows = new ArrayList<>(List.of(
                row("revenue", "매출액", revenue),
                row("operatingProfit", "영업이익", operating),
                row("netIncome", "당기순이익", net),
                row("operatingCf", "영업활동현금흐름", operatingCf),
                row("fcf", "잉여현금흐름(FCF)", fcfSeries(facts))));
        rows.addAll(List.of(
                row("costRatio", "매출원가율(%)",
                        percentSeries(facts.series(UsFinancialConcept.COST_OF_REVENUE), revenue)),
                row("sgaRatio", "판매관리비율(%)",
                        percentSeries(facts.series(UsFinancialConcept.SGA_EXPENSE), revenue)),
                row("operatingMargin", "영업이익률(%)", percentSeries(operating, revenue)),
                row("netMargin", "순이익률(%)", percentSeries(net, revenue))));
        return rows;
    }

    // === 재무제표 요약 ===

    public List<MetricRow> statementRows(UsCompanyFacts facts) {
        Map<String, BigDecimal> totalAssets = facts.series(UsFinancialConcept.TOTAL_ASSETS);
        Map<String, BigDecimal> currentAssets = facts.series(UsFinancialConcept.CURRENT_ASSETS);
        Map<String, BigDecimal> totalLiabilities = facts.series(UsFinancialConcept.TOTAL_LIABILITIES);
        Map<String, BigDecimal> currentLiabilities = facts.series(UsFinancialConcept.CURRENT_LIABILITIES);
        List<MetricRow> rows = new ArrayList<>(List.of(
                row("bs.currentAssets", "유동자산", currentAssets),
                row("bs.nonCurrentAssets", "비유동자산", subtractSeries(totalAssets, currentAssets)),
                row("bs.totalAssets", "자산총계", totalAssets),
                row("bs.currentLiabilities", "유동부채", currentLiabilities),
                row("bs.nonCurrentLiabilities", "비유동부채", subtractSeries(totalLiabilities, currentLiabilities)),
                row("bs.totalLiabilities", "부채총계", totalLiabilities),
                row("bs.capitalStock", "자본금", facts.series(UsFinancialConcept.CAPITAL_STOCK)),
                row("bs.retainedEarnings", "이익잉여금", facts.series(UsFinancialConcept.RETAINED_EARNINGS)),
                row("bs.totalEquity", "자본총계", facts.series(UsFinancialConcept.EQUITY))));
        rows.addAll(List.of(
                row("is.revenue", "매출액", facts.series(UsFinancialConcept.REVENUE)),
                row("is.operatingProfit", "영업이익", facts.series(UsFinancialConcept.OPERATING_INCOME)),
                row("is.pretaxIncome", "법인세차감전 순이익", facts.series(UsFinancialConcept.PRETAX_INCOME)),
                row("is.netIncome", "당기순이익", facts.series(UsFinancialConcept.NET_INCOME))));
        rows.addAll(List.of(
                row("cf.operating", "영업활동현금흐름", facts.series(UsFinancialConcept.OPERATING_CF)),
                row("cf.investing", "투자활동현금흐름", facts.series(UsFinancialConcept.INVESTING_CF)),
                row("cf.financing", "재무활동현금흐름", facts.series(UsFinancialConcept.FINANCING_CF)),
                row("cf.fcf", "잉여현금흐름(FCF)", fcfSeries(facts))));
        return rows;
    }

    // === 재무지표 (기준치 판정 — KR과 동일 임계값) ===

    public List<RatioRow> ratioRows(UsCompanyFacts facts, String baseYear) {
        Map<String, BigDecimal> revenue = facts.series(UsFinancialConcept.REVENUE);
        Map<String, BigDecimal> net = facts.series(UsFinancialConcept.NET_INCOME);
        Map<String, BigDecimal> equity = facts.series(UsFinancialConcept.EQUITY);
        Map<String, BigDecimal> totalAssets = facts.series(UsFinancialConcept.TOTAL_ASSETS);

        Map<String, BigDecimal> equityRatio = percentSeries(equity, totalAssets);
        Map<String, BigDecimal> currentRatio = percentSeries(
                facts.series(UsFinancialConcept.CURRENT_ASSETS), facts.series(UsFinancialConcept.CURRENT_LIABILITIES));
        Map<String, BigDecimal> debtRatio = percentSeries(
                facts.series(UsFinancialConcept.TOTAL_LIABILITIES), equity);
        Map<String, BigDecimal> operatingMargin = percentSeries(
                facts.series(UsFinancialConcept.OPERATING_INCOME), revenue);
        Map<String, BigDecimal> roe = percentSeries(net, equity);
        Map<String, BigDecimal> roa = percentSeries(net, totalAssets);
        Map<String, BigDecimal> revenueGrowth = growthSeries(revenue);
        Map<String, BigDecimal> operatingGrowth = growthSeries(facts.series(UsFinancialConcept.OPERATING_INCOME));

        return List.of(
                ratioRow("equityRatio", "자기자본비율(%)", "health", equityRatio,
                        judge(equityRatio.get(baseYear), new BigDecimal("40"), new BigDecimal("20"))),
                ratioRow("currentRatio", "유동비율(%)", "health", currentRatio,
                        judge(currentRatio.get(baseYear), new BigDecimal("100"), new BigDecimal("100"))),
                ratioRow("debtRatio", "부채비율(%)", "health", debtRatio,
                        judgeInverse(debtRatio.get(baseYear), new BigDecimal("200"), new BigDecimal("300"))),
                ratioRow("operatingMargin", "영업이익률(%)", "profitability", operatingMargin,
                        judge(operatingMargin.get(baseYear), new BigDecimal("5"), BigDecimal.ZERO)),
                ratioRow("roe", "ROE(%)", "profitability", roe,
                        judge(roe.get(baseYear), new BigDecimal("10"), new BigDecimal("5"))),
                ratioRow("roa", "ROA(%)", "profitability", roa,
                        judge(roa.get(baseYear), new BigDecimal("5"), BigDecimal.ZERO)),
                ratioRow("revenueGrowth", "매출 성장률(%)", "growth", revenueGrowth,
                        judge(revenueGrowth.get(baseYear), new BigDecimal("10"), BigDecimal.ZERO)),
                ratioRow("operatingProfitGrowth", "영업이익 성장률(%)", "growth", operatingGrowth,
                        judge(operatingGrowth.get(baseYear), new BigDecimal("10"), BigDecimal.ZERO)));
    }

    // === 주가지표 ===

    /**
     * @param referencePrice     KIS 해외 현재가 (달러). 조회 실패 시 null — 시총·PER 등은 null로 남는다
     * @param referencePriceDate 현재가 기준일 (yyyy-MM-dd)
     */
    public PriceMetrics priceMetrics(UsCompanyFacts facts, String baseYear,
            BigDecimal referencePrice, String referencePriceDate) {
        BigDecimal shares = sharesAt(facts, baseYear);
        BigDecimal marketCap = (referencePrice != null && shares != null)
                ? referencePrice.multiply(shares).setScale(0, RoundingMode.HALF_UP) : null;
        BigDecimal revenue = facts.valueAt(UsFinancialConcept.REVENUE, baseYear);
        BigDecimal operatingCf = facts.valueAt(UsFinancialConcept.OPERATING_CF, baseYear);
        BigDecimal net = facts.valueAt(UsFinancialConcept.NET_INCOME, baseYear);
        BigDecimal equity = facts.valueAt(UsFinancialConcept.EQUITY, baseYear);

        BigDecimal eps = facts.valueAt(UsFinancialConcept.EPS_DILUTED, baseYear);
        if (eps == null && net != null && shares != null && shares.signum() > 0) {
            eps = net.divide(shares, 2, RoundingMode.HALF_UP);
        }
        BigDecimal bps = (equity != null && shares != null && shares.signum() > 0)
                ? equity.divide(shares, 2, RoundingMode.HALF_UP) : null;
        BigDecimal per = divide(referencePrice, eps);
        BigDecimal pbr = divide(referencePrice, bps);
        BigDecimal psr = divide(marketCap, revenue);
        BigDecimal pcfr = divide(marketCap, positiveOrNull(operatingCf));
        BigDecimal perTimesPbr = multiply(per, pbr);
        BigDecimal evEbitda = evEbitda(facts, baseYear, marketCap);
        BigDecimal roic = roic(facts, baseYear);
        BigDecimal accrualRatio = accrualRatio(facts, baseYear);

        Map<String, MetricBreakdown> breakdowns = buildBreakdowns(facts, baseYear, referencePrice, shares,
                marketCap, revenue, operatingCf, eps, bps, per, pbr, psr, pcfr, perTimesPbr,
                evEbitda, roic, accrualRatio);
        return new PriceMetrics(
                baseYear == null ? null : "FY" + baseYear,
                referencePriceDate,
                referencePrice, marketCap, eps, bps, per, pbr, psr, pcfr, perTimesPbr,
                evEbitda, roic, accrualRatio,
                List.of(),
                breakdowns);
    }

    private Map<String, MetricBreakdown> buildBreakdowns(UsCompanyFacts facts, String baseYear,
            BigDecimal referencePrice, BigDecimal shares, BigDecimal marketCap, BigDecimal revenue,
            BigDecimal operatingCf, BigDecimal eps, BigDecimal bps, BigDecimal per, BigDecimal pbr,
            BigDecimal psr, BigDecimal pcfr, BigDecimal perTimesPbr, BigDecimal evEbitda, BigDecimal roic,
            BigDecimal accrualRatio) {
        BigDecimal net = facts.valueAt(UsFinancialConcept.NET_INCOME, baseYear);
        BigDecimal equity = facts.valueAt(UsFinancialConcept.EQUITY, baseYear);
        BigDecimal totalAssets = facts.valueAt(UsFinancialConcept.TOTAL_ASSETS, baseYear);
        BigDecimal operating = facts.valueAt(UsFinancialConcept.OPERATING_INCOME, baseYear);
        BigDecimal amortization = facts.valueAt(UsFinancialConcept.DEPRECIATION_AMORTIZATION, baseYear);
        BigDecimal borrowings = borrowings(facts, baseYear);
        BigDecimal cashSec = cashAndSecurities(facts, baseYear);

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
        putRoic(map, roic, operating, equity, borrowings, cashSec, facts, baseYear);
        putAccrual(map, accrualRatio, net, operatingCf, totalAssets);
        return map.isEmpty() ? null : map;
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
            UsCompanyFacts facts, String baseYear) {
        if (roic == null || operating == null || equity == null) {
            return;
        }
        BigDecimal taxRate = effectiveTaxRate(facts, baseYear);
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

    private BreakdownTerm term(String op, String label, BigDecimal value, String unit) {
        return value == null ? null : new BreakdownTerm(op, label, value, unit);
    }

    private void putBreakdown(Map<String, MetricBreakdown> map, String key, BigDecimal result,
            List<BreakdownTerm> terms, List<BreakdownTerm> extras) {
        if (result == null || terms.stream().anyMatch(Objects::isNull)) {
            return;
        }
        List<BreakdownTerm> cleanExtras = extras == null ? null
                : extras.stream().filter(Objects::nonNull).toList();
        map.put(key, new MetricBreakdown(terms, result, cleanExtras));
    }

    /** 기준연도 발행주식수, 없으면 가장 최근 연도 값 (dei는 제출 시점 기준이라 연도 결측이 흔함) */
    private BigDecimal sharesAt(UsCompanyFacts facts, String baseYear) {
        Map<String, BigDecimal> shares = facts.series(UsFinancialConcept.SHARES_OUTSTANDING);
        if (shares.isEmpty()) {
            return null;
        }
        BigDecimal atBase = shares.get(baseYear);
        if (atBase != null) {
            return atBase;
        }
        return shares.entrySet().stream()
                .max(Map.Entry.comparingByKey())
                .map(Map.Entry::getValue)
                .orElse(null);
    }

    private BigDecimal evEbitda(UsCompanyFacts facts, String baseYear, BigDecimal marketCap) {
        BigDecimal operating = facts.valueAt(UsFinancialConcept.OPERATING_INCOME, baseYear);
        BigDecimal amortization = facts.valueAt(UsFinancialConcept.DEPRECIATION_AMORTIZATION, baseYear);
        if (marketCap == null || operating == null || amortization == null) {
            return null;
        }
        BigDecimal ebitda = operating.add(amortization);
        if (ebitda.signum() <= 0) {
            return null;
        }
        BigDecimal ev = marketCap.add(orZero(borrowings(facts, baseYear)))
                .subtract(orZero(cashAndSecurities(facts, baseYear)));
        return ev.divide(ebitda, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal roic(UsCompanyFacts facts, String baseYear) {
        BigDecimal operating = facts.valueAt(UsFinancialConcept.OPERATING_INCOME, baseYear);
        BigDecimal equity = facts.valueAt(UsFinancialConcept.EQUITY, baseYear);
        if (operating == null || equity == null) {
            return null;
        }
        BigDecimal investedCapital = equity.add(orZero(borrowings(facts, baseYear)))
                .subtract(orZero(cashAndSecurities(facts, baseYear)));
        if (investedCapital.signum() <= 0) {
            return null;
        }
        BigDecimal nopat = operating.multiply(BigDecimal.ONE.subtract(effectiveTaxRate(facts, baseYear)));
        return nopat.divide(investedCapital, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP);
    }

    /** 실효세율 = (법인세차감전 − 당기순이익) / 법인세차감전. 0~50% 벗어나면 25% 대체 — KR과 동일 */
    private BigDecimal effectiveTaxRate(UsCompanyFacts facts, String baseYear) {
        BigDecimal pretax = facts.valueAt(UsFinancialConcept.PRETAX_INCOME, baseYear);
        BigDecimal net = facts.valueAt(UsFinancialConcept.NET_INCOME, baseYear);
        if (pretax == null || net == null || pretax.signum() <= 0) {
            return new BigDecimal("0.25");
        }
        BigDecimal rate = pretax.subtract(net).divide(pretax, 4, RoundingMode.HALF_UP);
        boolean valid = rate.compareTo(BigDecimal.ZERO) >= 0 && rate.compareTo(new BigDecimal("0.5")) <= 0;
        return valid ? rate : new BigDecimal("0.25");
    }

    private BigDecimal accrualRatio(UsCompanyFacts facts, String baseYear) {
        BigDecimal net = facts.valueAt(UsFinancialConcept.NET_INCOME, baseYear);
        BigDecimal operatingCf = facts.valueAt(UsFinancialConcept.OPERATING_CF, baseYear);
        BigDecimal totalAssets = facts.valueAt(UsFinancialConcept.TOTAL_ASSETS, baseYear);
        if (net == null || operatingCf == null || totalAssets == null || totalAssets.signum() <= 0) {
            return null;
        }
        return net.subtract(operatingCf)
                .divide(totalAssets, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP);
    }

    // === 가치평가 입력 (청산가치/DCF 원시 데이터) ===

    public ValuationInputs valuationInputs(UsCompanyFacts facts, String baseYear) {
        BigDecimal cash = facts.valueAt(UsFinancialConcept.CASH, baseYear);
        BigDecimal securities = facts.valueAt(UsFinancialConcept.SECURITIES_CURRENT, baseYear);
        BigDecimal receivables = facts.valueAt(UsFinancialConcept.RECEIVABLES, baseYear);
        BigDecimal inventory = facts.valueAt(UsFinancialConcept.INVENTORY, baseYear);
        BigDecimal investments = facts.valueAt(UsFinancialConcept.INVESTMENTS_NONCURRENT, baseYear);
        BigDecimal tangible = facts.valueAt(UsFinancialConcept.TANGIBLE_ASSETS, baseYear);
        BigDecimal intangible = sumOrNull(
                facts.valueAt(UsFinancialConcept.GOODWILL, baseYear),
                facts.valueAt(UsFinancialConcept.INTANGIBLE_ASSETS, baseYear));
        BigDecimal borrowings = borrowings(facts, baseYear);

        List<UnclassifiedLine> unclassified = new ArrayList<>();
        BigDecimal otherCurrent = residual(
                facts.valueAt(UsFinancialConcept.CURRENT_ASSETS, baseYear),
                List.of(orZero(cash), orZero(securities), orZero(receivables), orZero(inventory)));
        if (otherCurrent != null) {
            unclassified.add(new UnclassifiedLine("기타 유동자산(잔여)", otherCurrent, "otherCurrent"));
        }
        BigDecimal nonCurrentAssets = subtract(
                facts.valueAt(UsFinancialConcept.TOTAL_ASSETS, baseYear),
                facts.valueAt(UsFinancialConcept.CURRENT_ASSETS, baseYear));
        BigDecimal otherNonCurrent = residual(nonCurrentAssets,
                List.of(orZero(investments), orZero(tangible), orZero(intangible)));
        if (otherNonCurrent != null) {
            unclassified.add(new UnclassifiedLine("기타 비유동자산(잔여)", otherNonCurrent, "otherNonCurrent"));
        }

        BigDecimal netCash = (cash == null && securities == null) ? null
                : orZero(cash).add(orZero(securities)).subtract(orZero(borrowings));
        return new ValuationInputs(baseYear,
                cash, securities, receivables, inventory, investments, tangible, intangible,
                otherCurrent, otherNonCurrent,
                facts.valueAt(UsFinancialConcept.TOTAL_LIABILITIES, baseYear),
                borrowings, netCash, fcfSeries(facts).get(baseYear), unclassified);
    }

    /** 앵커(합계)에서 분류된 항목을 뺀 잔여. 앵커가 없거나 잔여가 0 이하이면 null */
    private BigDecimal residual(BigDecimal anchor, List<BigDecimal> classified) {
        if (anchor == null) {
            return null;
        }
        BigDecimal remainder = anchor.subtract(classified.stream().reduce(BigDecimal.ZERO, BigDecimal::add));
        return remainder.signum() > 0 ? remainder : null;
    }

    /** 차입금 = 장기차입 + 유동성장기차입 + 단기차입(CP 포함). 전부 미검출이면 null */
    private BigDecimal borrowings(UsCompanyFacts facts, String baseYear) {
        return sumOrNull(
                facts.valueAt(UsFinancialConcept.DEBT_NONCURRENT, baseYear),
                sumOrNull(
                        facts.valueAt(UsFinancialConcept.DEBT_CURRENT, baseYear),
                        facts.valueAt(UsFinancialConcept.SHORT_TERM_BORROWINGS, baseYear)));
    }

    private BigDecimal cashAndSecurities(UsCompanyFacts facts, String baseYear) {
        return sumOrNull(
                facts.valueAt(UsFinancialConcept.CASH, baseYear),
                facts.valueAt(UsFinancialConcept.SECURITIES_CURRENT, baseYear));
    }

    // === 배당 ===

    /**
     * KR 배당 표(당기/전기/전전기)와 같은 3개 연도 형태로 구성 — 주주 섹션 미지원과 무관하게 배당은 제공
     */
    public List<DividendRow> dividendRows(UsCompanyFacts facts, String baseYear) {
        if (baseYear == null) {
            return List.of();
        }
        String previous = previousYear(baseYear, 1);
        String beforePrevious = previousYear(baseYear, 2);
        List<DividendRow> rows = new ArrayList<>();
        addDividendRow(rows, "주당배당금($)", facts.series(UsFinancialConcept.DPS_DECLARED),
                baseYear, previous, beforePrevious);
        addDividendRow(rows, "현금배당금총액($)", facts.series(UsFinancialConcept.DIVIDENDS_PAID),
                baseYear, previous, beforePrevious);
        return rows;
    }

    private void addDividendRow(List<DividendRow> rows, String category, Map<String, BigDecimal> series,
            String baseYear, String previous, String beforePrevious) {
        if (series.isEmpty()) {
            return;
        }
        rows.add(new DividendRow(category, "보통주",
                plainOrNull(series.get(baseYear)),
                plainOrNull(series.get(previous)),
                plainOrNull(series.get(beforePrevious))));
    }

    // === 위험 시그널 ===

    /**
     * KR 5종 시그널 + 유상증자 대체(발행주식수 증가율 기반 희석 판정).
     * 서식 타입(S-3/424B) 기반 탐지는 채권 발행과 구분이 안 돼 사용하지 않는다.
     */
    public RiskSignals riskSignals(UsCompanyFacts facts, String baseYear) {
        BigDecimal currentAssets = facts.valueAt(UsFinancialConcept.CURRENT_ASSETS, baseYear);
        BigDecimal currentLiabilities = facts.valueAt(UsFinancialConcept.CURRENT_LIABILITIES, baseYear);
        BigDecimal equity = facts.valueAt(UsFinancialConcept.EQUITY, baseYear);
        BigDecimal equityRatio = percentSeries(
                facts.series(UsFinancialConcept.EQUITY), facts.series(UsFinancialConcept.TOTAL_ASSETS)).get(baseYear);
        Map<String, String> evidence = new LinkedHashMap<>();
        return new RiskSignals(
                exceeds(currentLiabilities, currentAssets, "유동부채>유동자산", evidence),
                atMost(equityRatio, new BigDecimal("20"), "자기자본비율(%)", evidence),
                negative(equity, "자본총계", evidence),
                surge(facts, UsFinancialConcept.RECEIVABLES, baseYear, "매출채권", evidence),
                surge(facts, UsFinancialConcept.INVENTORY, baseYear, "재고자산", evidence),
                sharesDilution(facts, evidence),
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

    private Boolean surge(UsCompanyFacts facts, UsFinancialConcept concept, String baseYear,
            String label, Map<String, String> evidence) {
        BigDecimal assetGrowth = growthSeries(facts.series(concept)).get(baseYear);
        BigDecimal revenueGrowth = growthSeries(facts.series(UsFinancialConcept.REVENUE)).get(baseYear);
        if (assetGrowth == null || revenueGrowth == null) {
            return null;
        }
        evidence.put(label + " 증가율(%) / 매출 증가율(%)",
                assetGrowth.toPlainString() + " / " + revenueGrowth.toPlainString());
        return assetGrowth.subtract(revenueGrowth).compareTo(SURGE_MARGIN) > 0;
    }

    /**
     * 발행주식수 희석 판정: 보유 이력(최대 5개 연도) 중 최초→최신 증가율이 임계값 초과면 true.
     * 자사주 소각 기업(감소)은 false — 주주환원 신호로 오탐하지 않는다.
     */
    private Boolean sharesDilution(UsCompanyFacts facts, Map<String, String> evidence) {
        Map<String, BigDecimal> shares = facts.series(UsFinancialConcept.SHARES_OUTSTANDING);
        List<String> years = shares.keySet().stream().sorted().toList();
        if (years.size() < 2) {
            return null;
        }
        List<String> window = years.subList(Math.max(0, years.size() - 5), years.size());
        BigDecimal earliest = shares.get(window.get(0));
        BigDecimal latest = shares.get(window.get(window.size() - 1));
        if (earliest == null || latest == null || earliest.signum() <= 0) {
            return null;
        }
        BigDecimal growthPct = latest.subtract(earliest)
                .divide(earliest, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP);
        evidence.put("발행주식수 증가율(%) (" + window.get(0) + "→" + window.get(window.size() - 1) + ")",
                growthPct.toPlainString());
        return growthPct.compareTo(DILUTION_THRESHOLD) > 0;
    }

    // === 시리즈/행 빌더 ===

    private Map<String, BigDecimal> fcfSeries(UsCompanyFacts facts) {
        return subtractSeries(facts.series(UsFinancialConcept.OPERATING_CF),
                facts.series(UsFinancialConcept.CAPEX));
    }

    /** 두 시리즈의 차 (양쪽 값이 모두 있는 연도만) */
    private Map<String, BigDecimal> subtractSeries(Map<String, BigDecimal> left, Map<String, BigDecimal> right) {
        Map<String, BigDecimal> series = new LinkedHashMap<>();
        left.forEach((year, value) -> {
            BigDecimal other = right.get(year);
            if (other != null) {
                series.put(year, value.subtract(other));
            }
        });
        return series;
    }

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

    /**
     * 비율(%) 시리즈 = 분자/분모 × 100 (둘 다 있는 연도만, 분모 0 이하 제외 — KR과 동일)
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

    private Map<String, BigDecimal> growthSeries(Map<String, BigDecimal> values) {
        Map<String, BigDecimal> series = new LinkedHashMap<>();
        values.forEach((year, value) -> {
            BigDecimal previous = values.get(previousYear(year, 1));
            if (previous != null && previous.signum() != 0) {
                series.put(year, value.subtract(previous)
                        .divide(previous.abs(), 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP));
            }
        });
        return series;
    }

    /** 높을수록 좋은 지표 — KR과 동일 판정 */
    private String judge(BigDecimal value, BigDecimal goodMin, BigDecimal riskMax) {
        if (value == null) {
            return null;
        }
        if (value.compareTo(goodMin) >= 0) {
            return "good";
        }
        return value.compareTo(riskMax) <= 0 ? "risk" : "warn";
    }

    /** 낮을수록 좋은 지표(부채비율) — KR과 동일 판정 */
    private String judgeInverse(BigDecimal value, BigDecimal goodMax, BigDecimal riskMin) {
        if (value == null) {
            return null;
        }
        if (value.compareTo(goodMax) <= 0) {
            return "good";
        }
        return value.compareTo(riskMin) > 0 ? "risk" : "warn";
    }

    // === 공통 헬퍼 ===

    private String previousYear(String year, int back) {
        if (year == null) {
            return null;
        }
        try {
            return String.valueOf(Integer.parseInt(year) - back);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private BigDecimal sumOrNull(BigDecimal left, BigDecimal right) {
        if (left == null && right == null) {
            return null;
        }
        return orZero(left).add(orZero(right));
    }

    private BigDecimal subtract(BigDecimal left, BigDecimal right) {
        if (left == null || right == null) {
            return null;
        }
        return left.subtract(right);
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

    private String plainOrNull(BigDecimal value) {
        return value == null ? null : value.toPlainString();
    }
}
