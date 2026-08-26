package com.thlee.stock.market.stockmarket.companyreport.application;

import com.thlee.stock.market.stockmarket.companyreport.application.dto.CompanyReportResults.GradeSuggestion;
import com.thlee.stock.market.stockmarket.companyreport.application.dto.ReportSnapshot;
import com.thlee.stock.market.stockmarket.companyreport.application.dto.ReportSnapshot.PriceMetrics;
import com.thlee.stock.market.stockmarket.companyreport.application.dto.ReportSnapshot.RatioRow;
import com.thlee.stock.market.stockmarket.companyreport.application.dto.ReportSnapshot.RiskSignals;
import com.thlee.stock.market.stockmarket.companyreport.application.dto.ReportValuation;
import com.thlee.stock.market.stockmarket.companyreport.application.dto.ReportValuation.DcfValuation;
import com.thlee.stock.market.stockmarket.companyreport.application.dto.ReportValuation.LiquidationValuation;
import com.thlee.stock.market.stockmarket.companyreport.domain.model.ReportGrade;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * 투자판단 정량 5항목의 제안 등급(A~E)과 근거를 산출한다.
 * (스냅샷 × 파생 가치평가)의 순수 함수 — 저장하지 않고 조회 시 계산하며, 근거가 부족한 항목은 map에서 생략한다.
 * 두 신호(예: 청산가치·PBR)가 있으면 유리한 밴드를 취한다. 기준표: docs/brainstorms/2026-07-24-grade-suggestion-brainstorm.md
 * 표시용 기준표 사본: static/js/components/company-report.js crGradeCriteria — 기준 변경 시 함께 수정.
 */
@Component
public class GradeSuggestionCalculator {

    private static final BigDecimal EXCELLENT_ROE = new BigDecimal("15");
    private static final BigDecimal EXCELLENT_MARGIN = new BigDecimal("10");
    private static final BigDecimal EXCELLENT_GROWTH = new BigDecimal("10");

    public Map<String, GradeSuggestion> calculate(ReportSnapshot snapshot, ReportValuation valuation) {
        if (snapshot == null) {
            return Map.of();
        }
        Map<String, GradeSuggestion> map = new LinkedHashMap<>();
        put(map, "assetUndervalue", assetUndervalue(snapshot, valuation));
        put(map, "earningsUndervalue", earningsUndervalue(snapshot, valuation));
        put(map, "financialHealth", financialHealth(snapshot));
        put(map, "profitability", profitability(snapshot));
        put(map, "growth", growth(snapshot));
        return map;
    }

    private void put(Map<String, GradeSuggestion> map, String key, GradeSuggestion suggestion) {
        if (suggestion != null) {
            map.put(key, suggestion);
        }
    }

    // === ① 자산으로 본 저평가: 시총/청산가치 밴드 vs PBR 밴드 ===

    private GradeSuggestion assetUndervalue(ReportSnapshot snapshot, ReportValuation valuation) {
        BigDecimal liquidationRatio = liquidationRatio(valuation);
        BigDecimal pbr = metric(snapshot, PriceMetrics::pbr);
        ReportGrade grade = better(liquidationBand(liquidationRatio), pbrBand(pbr));
        return grade == null ? null : new GradeSuggestion(grade, assetBasis(liquidationRatio, pbr));
    }

    private BigDecimal liquidationRatio(ReportValuation valuation) {
        LiquidationValuation liquidation = valuation == null ? null : valuation.liquidation();
        return liquidation == null ? null : liquidation.marketCapToLiquidation();
    }

    private ReportGrade liquidationBand(BigDecimal ratio) {
        return band(ratio, at("1.0", ReportGrade.A), at("1.5", ReportGrade.B),
                at("2.5", ReportGrade.C), at("4.0", ReportGrade.D));
    }

    private ReportGrade pbrBand(BigDecimal pbr) {
        return band(pbr, at("0.6", ReportGrade.B), at("1.0", ReportGrade.C), at("2.0", ReportGrade.D));
    }

    private List<String> assetBasis(BigDecimal liquidationRatio, BigDecimal pbr) {
        List<String> basis = new ArrayList<>();
        add(basis, "시가총액/청산가치", liquidationRatio, "배");
        add(basis, "PBR", pbr, "배");
        return basis;
    }

    // === ② 수익 창출 능력으로 본 저평가: 시총/DCF 밴드 vs PER 밴드 ===

    private GradeSuggestion earningsUndervalue(ReportSnapshot snapshot, ReportValuation valuation) {
        DcfValuation dcf = valuation == null ? null : valuation.dcf();
        BigDecimal per = metric(snapshot, PriceMetrics::per);
        ReportGrade grade = better(dcfBand(dcf), perBand(per));
        return grade == null ? earningsFallback(dcf, per) : new GradeSuggestion(grade, earningsBasis(dcf, per));
    }

    /** 보수 시나리오로 A/B, 낙관 시나리오로 C/D. 보수 초과(E)는 낙관 밴드에 위임 */
    private ReportGrade dcfBand(DcfValuation dcf) {
        if (dcf == null) {
            return null;
        }
        ReportGrade byConservative = band(dcf.marketCapToConservative(), at("1.0", ReportGrade.A), at("1.5", ReportGrade.B));
        ReportGrade byOptimistic = band(dcf.marketCapToOptimistic(), at("1.0", ReportGrade.C), at("1.5", ReportGrade.D));
        return better(nullIfE(byConservative), byOptimistic);
    }

    private ReportGrade perBand(BigDecimal per) {
        return band(per, at("8", ReportGrade.B), at("15", ReportGrade.C), at("25", ReportGrade.D));
    }

    /** FCF 0 이하(DCF 계산 불가) + PER 없음(적자) → 수익 근거 없음 E. 그 외 결손은 제안 생략 */
    private GradeSuggestion earningsFallback(DcfValuation dcf, BigDecimal per) {
        if (per != null || dcf == null || dcf.calculable()) {
            return null;
        }
        if (dcf.fcf() == null || dcf.fcf().signum() > 0) {
            return null;
        }
        return new GradeSuggestion(ReportGrade.E, List.of("FCF 0 이하 · PER 산출 불가 (수익 근거 없음)"));
    }

    private List<String> earningsBasis(DcfValuation dcf, BigDecimal per) {
        List<String> basis = new ArrayList<>();
        add(basis, "시가총액/DCF보수", dcf == null ? null : dcf.marketCapToConservative(), "배");
        add(basis, "시가총액/DCF낙관", dcf == null ? null : dcf.marketCapToOptimistic(), "배");
        add(basis, "PER", per, "배");
        return basis;
    }

    // === ③ 재무건전성: 판정 3종 + 순현금 + 위험 시그널 ===

    private GradeSuggestion financialHealth(ReportSnapshot snapshot) {
        List<String> judgments = judgments(snapshot, "equityRatio", "currentRatio", "debtRatio");
        if (judgments.stream().allMatch(Objects::isNull)) {
            return null;
        }
        return new GradeSuggestion(healthGrade(snapshot, judgments), healthBasis(snapshot));
    }

    private ReportGrade healthGrade(ReportSnapshot snapshot, List<String> judgments) {
        if (count(judgments, "risk") >= 2 || Boolean.TRUE.equals(signal(snapshot, RiskSignals::negativeEquity))) {
            return ReportGrade.E;
        }
        return count(judgments, "risk") == 1 ? ReportGrade.D : healthUpper(snapshot, judgments);
    }

    /** 판정 결손은 warn으로 보수 처리 (good 아님) */
    private ReportGrade healthUpper(ReportSnapshot snapshot, List<String> judgments) {
        if (count(judgments, "good") < judgments.size()) {
            return ReportGrade.C;
        }
        return healthExcellent(snapshot) ? ReportGrade.A : ReportGrade.B;
    }

    /** A 승급: 순현금 > 0 그리고 위험 시그널(유상증자 제외) 전부 false */
    private boolean healthExcellent(ReportSnapshot snapshot) {
        BigDecimal netCash = netCash(snapshot);
        return netCash != null && netCash.signum() > 0 && noRiskSignals(snapshot);
    }

    private boolean noRiskSignals(ReportSnapshot snapshot) {
        RiskSignals signals = snapshot.riskSignals();
        if (signals == null) {
            return false;
        }
        return Stream.of(signals.currentLiabilitiesExceedCurrentAssets(), signals.lowEquityRatio(),
                        signals.negativeEquity(), signals.receivablesSurge(), signals.inventorySurge())
                .noneMatch(Boolean.TRUE::equals);
    }

    private List<String> healthBasis(ReportSnapshot snapshot) {
        List<String> basis = ratioBasis(snapshot, "equityRatio", "currentRatio", "debtRatio");
        addAmount(basis, "순현금", netCash(snapshot), snapshot.currency());
        return basis;
    }

    private BigDecimal netCash(ReportSnapshot snapshot) {
        return snapshot.valuationInputs() == null ? null : snapshot.valuationInputs().netCash();
    }

    // === ④ 수익성: 판정 3종 + 다년 지속성 ===

    private GradeSuggestion profitability(ReportSnapshot snapshot) {
        List<String> judgments = judgments(snapshot, "operatingMargin", "roe", "roa");
        if (judgments.stream().allMatch(Objects::isNull)) {
            return null;
        }
        return new GradeSuggestion(profitabilityGrade(snapshot, judgments),
                ratioBasis(snapshot, "operatingMargin", "roe", "roa"));
    }

    /** E: 영업이익률 risk(영업적자) 또는 기준연도 ROE 음수(순적자) */
    private ReportGrade profitabilityGrade(ReportSnapshot snapshot, List<String> judgments) {
        if ("risk".equals(judgment(snapshot, "operatingMargin")) || isNegative(baseYearValue(snapshot, "roe"))) {
            return ReportGrade.E;
        }
        return count(judgments, "risk") >= 1 ? ReportGrade.D : profitabilityUpper(snapshot, judgments);
    }

    private ReportGrade profitabilityUpper(ReportSnapshot snapshot, List<String> judgments) {
        if (count(judgments, "good") < judgments.size()) {
            return ReportGrade.C;
        }
        return sustainedProfitability(snapshot) ? ReportGrade.A : ReportGrade.B;
    }

    /** A 승급: 기준연도 포함 최근 3개 연도(값 쌍 존재 연도 ≥ 2) 모두 ROE ≥ 15% 그리고 영업이익률 ≥ 10% */
    private boolean sustainedProfitability(ReportSnapshot snapshot) {
        List<YearPair> pairs = presentPairs(snapshot, "roe", "operatingMargin", 3);
        return pairs.size() >= 2 && pairs.stream().allMatch(this::meetsProfitabilityBar);
    }

    private boolean meetsProfitabilityBar(YearPair pair) {
        return pair.first().compareTo(EXCELLENT_ROE) >= 0 && pair.second().compareTo(EXCELLENT_MARGIN) >= 0;
    }

    // === ⑤ 성장성: 매출·영업이익 성장률 (기준연도 + 추세) ===

    private GradeSuggestion growth(ReportSnapshot snapshot) {
        BigDecimal revenue = baseYearValue(snapshot, "revenueGrowth");
        BigDecimal operating = baseYearValue(snapshot, "operatingProfitGrowth");
        if (revenue == null || operating == null) {
            return null;
        }
        return new GradeSuggestion(growthGrade(snapshot, revenue, operating),
                ratioBasis(snapshot, "revenueGrowth", "operatingProfitGrowth"));
    }

    private ReportGrade growthGrade(ReportSnapshot snapshot, BigDecimal revenue, BigDecimal operating) {
        if (isNegative(revenue) || isNegative(operating)) {
            return bothDecliningTwoYears(snapshot, revenue, operating) ? ReportGrade.E : ReportGrade.D;
        }
        if (revenue.compareTo(EXCELLENT_GROWTH) < 0 || operating.compareTo(EXCELLENT_GROWTH) < 0) {
            return ReportGrade.C;
        }
        return sustainedGrowth(snapshot) ? ReportGrade.A : ReportGrade.B;
    }

    /** E 강등: 기준연도와 직전연도 모두 두 성장률이 음수 (다년 감소 추세) */
    private boolean bothDecliningTwoYears(ReportSnapshot snapshot, BigDecimal revenue, BigDecimal operating) {
        if (!isNegative(revenue) || !isNegative(operating)) {
            return false;
        }
        String previous = previousYear(baseYear(snapshot));
        return isNegative(valueAt(snapshot, "revenueGrowth", previous))
                && isNegative(valueAt(snapshot, "operatingProfitGrowth", previous));
    }

    /** A 승급: 최근 4개 연도 중 3개 연도 이상에서 두 성장률 모두 ≥ 10% */
    private boolean sustainedGrowth(ReportSnapshot snapshot) {
        List<YearPair> pairs = presentPairs(snapshot, "revenueGrowth", "operatingProfitGrowth", 4);
        return pairs.stream().filter(this::meetsGrowthBar).count() >= 3;
    }

    private boolean meetsGrowthBar(YearPair pair) {
        return pair.first().compareTo(EXCELLENT_GROWTH) >= 0 && pair.second().compareTo(EXCELLENT_GROWTH) >= 0;
    }

    // === 밴드/등급 공통 ===

    /** 오름차순 임계값 밴드: value ≤ max면 해당 등급, 모두 초과면 E, value 없으면 null */
    private ReportGrade band(BigDecimal value, Band... bands) {
        if (value == null) {
            return null;
        }
        for (Band band : bands) {
            if (value.compareTo(band.max()) <= 0) {
                return band.grade();
            }
        }
        return ReportGrade.E;
    }

    private record Band(BigDecimal max, ReportGrade grade) {}

    private Band at(String max, ReportGrade grade) {
        return new Band(new BigDecimal(max), grade);
    }

    /** 두 신호 중 유리한(더 높은) 등급. 한쪽이 없으면 있는 쪽 */
    private ReportGrade better(ReportGrade left, ReportGrade right) {
        if (left == null || right == null) {
            return left != null ? left : right;
        }
        return left.ordinal() <= right.ordinal() ? left : right;
    }

    private ReportGrade nullIfE(ReportGrade grade) {
        return grade == ReportGrade.E ? null : grade;
    }

    // === 스냅샷 접근 ===

    private BigDecimal metric(ReportSnapshot snapshot, Function<PriceMetrics, BigDecimal> getter) {
        return snapshot.priceMetrics() == null ? null : getter.apply(snapshot.priceMetrics());
    }

    private Boolean signal(ReportSnapshot snapshot, Function<RiskSignals, Boolean> getter) {
        return snapshot.riskSignals() == null ? null : getter.apply(snapshot.riskSignals());
    }

    private List<String> judgments(ReportSnapshot snapshot, String... keys) {
        return Arrays.stream(keys).map(key -> judgment(snapshot, key)).toList();
    }

    private String judgment(ReportSnapshot snapshot, String key) {
        RatioRow row = ratioRow(snapshot, key);
        return row == null ? null : row.judgment();
    }

    private RatioRow ratioRow(ReportSnapshot snapshot, String key) {
        if (snapshot.ratios() == null) {
            return null;
        }
        return snapshot.ratios().stream().filter(row -> key.equals(row.key())).findFirst().orElse(null);
    }

    private long count(List<String> judgments, String judgment) {
        return judgments.stream().filter(judgment::equals).count();
    }

    private String baseYear(ReportSnapshot snapshot) {
        return snapshot.valuationInputs() == null ? null : snapshot.valuationInputs().baseYear();
    }

    private BigDecimal baseYearValue(ReportSnapshot snapshot, String key) {
        return valueAt(snapshot, key, baseYear(snapshot));
    }

    private BigDecimal valueAt(ReportSnapshot snapshot, String key, String year) {
        RatioRow row = ratioRow(snapshot, key);
        if (row == null || row.values() == null || year == null) {
            return null;
        }
        return parse(row.values().get(year));
    }

    /** 기준연도 포함 최근 n개 연도 중 두 지표 값이 모두 존재하는 연도의 값 쌍 */
    private List<YearPair> presentPairs(ReportSnapshot snapshot, String firstKey, String secondKey, int years) {
        List<YearPair> pairs = new ArrayList<>();
        for (String year : recentYears(baseYear(snapshot), years)) {
            addPair(pairs, valueAt(snapshot, firstKey, year), valueAt(snapshot, secondKey, year));
        }
        return pairs;
    }

    private void addPair(List<YearPair> pairs, BigDecimal first, BigDecimal second) {
        if (first != null && second != null) {
            pairs.add(new YearPair(first, second));
        }
    }

    private record YearPair(BigDecimal first, BigDecimal second) {}

    private List<String> recentYears(String baseYear, int count) {
        try {
            int base = Integer.parseInt(baseYear);
            return IntStream.range(0, count).mapToObj(i -> String.valueOf(base - i)).toList();
        } catch (NumberFormatException e) {
            return List.of();
        }
    }

    private String previousYear(String baseYear) {
        List<String> years = recentYears(baseYear, 2);
        return years.size() < 2 ? null : years.get(1);
    }

    // === 근거/파싱 ===

    private List<String> ratioBasis(ReportSnapshot snapshot, String... keys) {
        List<String> basis = new ArrayList<>();
        for (String key : keys) {
            addRatioLine(basis, ratioRow(snapshot, key), baseYear(snapshot));
        }
        return basis;
    }

    private void addRatioLine(List<String> basis, RatioRow row, String baseYear) {
        if (row == null) {
            return;
        }
        String value = row.values() == null || baseYear == null ? null : row.values().get(baseYear);
        String judgment = row.judgment() == null ? "" : " (" + row.judgment() + ")";
        basis.add(row.name() + " " + (value == null ? "-" : value) + judgment);
    }

    private void add(List<String> basis, String label, BigDecimal value, String unit) {
        if (value != null) {
            basis.add(label + " " + value.toPlainString() + unit);
        }
    }

    /** 큰 금액 근거 표기 — KRW: 억원, USD: $B(10억 달러). currency 부재(v1 스냅샷)는 KRW로 해석 */
    private void addAmount(List<String> basis, String label, BigDecimal amount, String currency) {
        if (amount == null) {
            return;
        }
        if (ReportSnapshot.CURRENCY_USD.equals(currency)) {
            basis.add(label + " $" + amount.movePointLeft(9).setScale(1, RoundingMode.HALF_UP).toPlainString() + "B");
            return;
        }
        basis.add(label + " " + amount.movePointLeft(8).setScale(0, RoundingMode.HALF_UP).toPlainString() + "억원");
    }

    private BigDecimal parse(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private boolean isNegative(BigDecimal value) {
        return value != null && value.signum() < 0;
    }
}
