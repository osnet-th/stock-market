package com.thlee.stock.market.stockmarket.companyreport.domain.model;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;

/** 외부 의존성이 없는 S-RIM 계산. 시계는 호출자가 제공한다. */
public final class SrimCalculator {
    private static final MathContext MC = MathContext.DECIMAL128;
    private SrimCalculator() {}

    public static SrimValuation calculate(SrimInput input, boolean draft, String calculatedAt) {
        SrimValidator.validate(input, draft);
        List<SrimValuation.YearResult> rows = input.years().stream().map(row -> calculateYear(input, row)).toList();
        return new SrimValuation(1, 1, input, rows, calculatedAt);
    }

    private static SrimValuation.YearResult calculateYear(SrimInput input, SrimInput.Year row) {
        BigDecimal average = average(row);
        BigDecimal roe = roe(row, average);
        if (row.year() == null || roe == null || !hasCommon(input))
            return new SrimValuation.YearResult(row.year(), roe, average, null, null, null);
        return completed(input, row.year(), roe, average);
    }

    private static SrimValuation.YearResult completed(SrimInput input, Integer year, BigDecimal roe, BigDecimal average) {
        BigDecimal excess = input.equity().multiply(roe.subtract(input.requiredReturn()), MC);
        return new SrimValuation.YearResult(year, roe, average,
                scenario(input, excess, BigDecimal.ONE), scenario(input, excess, new BigDecimal("0.9")),
                scenario(input, excess, new BigDecimal("0.8")));
    }

    private static boolean hasCommon(SrimInput input) {
        return input.equity() != null && input.shares() != null && input.requiredReturn() != null;
    }

    private static BigDecimal average(SrimInput.Year row) {
        if (!"CALCULATED".equals(row.mode()) || !SrimValidator.hasCalculationValues(row)) return null;
        return row.previousEquity().add(row.expectedEquity()).divide(BigDecimal.valueOf(2), MC);
    }

    private static BigDecimal roe(SrimInput.Year row, BigDecimal average) {
        if ("DIRECT".equals(row.mode())) return row.directRoe();
        return average == null ? null : row.expectedIncome().divide(average, MC);
    }

    private static SrimValuation.Scenario scenario(SrimInput input, BigDecimal excess, BigDecimal w) {
        BigDecimal denominator = BigDecimal.ONE.add(input.requiredReturn()).subtract(w);
        BigDecimal value = input.equity().add(excess.multiply(w, MC).divide(denominator, MC));
        BigDecimal price = value.divide(input.shares(), MC).setScale(6, RoundingMode.HALF_UP);
        return new SrimValuation.Scenario(price, difference(price, input.referencePrice()));
    }

    private static BigDecimal difference(BigDecimal price, BigDecimal referencePrice) {
        if (referencePrice == null) return null;
        return price.divide(referencePrice, MC).subtract(BigDecimal.ONE)
                .multiply(BigDecimal.valueOf(100)).setScale(6, RoundingMode.HALF_UP);
    }
}
