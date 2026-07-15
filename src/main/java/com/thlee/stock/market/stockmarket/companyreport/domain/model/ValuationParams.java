package com.thlee.stock.market.stockmarket.companyreport.domain.model;

import java.math.BigDecimal;

/**
 * 기업가치 평가 파라미터 (리포트별 조정 가능, JSON 컬럼으로 저장).
 * 비율은 모두 소수(0.10 = 10%)로 표현한다.
 */
public record ValuationParams(
        BigDecimal discountRate,
        BigDecimal optimisticGrowthRate,
        int optimisticGrowthYears,
        AdjustmentRatios adjustmentRatios
) {

    /**
     * 청산가치 계산용 조정자산 비율표 (장부가 대비 회수율)
     */
    public record AdjustmentRatios(
            BigDecimal cash,
            BigDecimal securities,
            BigDecimal receivables,
            BigDecimal inventory,
            BigDecimal investments,
            BigDecimal tangible,
            BigDecimal intangible,
            BigDecimal otherCurrent
    ) {
        public static AdjustmentRatios defaults() {
            return new AdjustmentRatios(
                    new BigDecimal("1.00"),
                    new BigDecimal("1.00"),
                    new BigDecimal("0.85"),
                    new BigDecimal("0.50"),
                    new BigDecimal("0.50"),
                    new BigDecimal("0.50"),
                    BigDecimal.ZERO,
                    BigDecimal.ZERO
            );
        }
    }

    public static ValuationParams defaults() {
        return new ValuationParams(
                new BigDecimal("0.10"),
                new BigDecimal("0.20"),
                5,
                AdjustmentRatios.defaults()
        );
    }
}
