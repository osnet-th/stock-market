package com.thlee.stock.market.stockmarket.companyreport.application.dto;

import com.thlee.stock.market.stockmarket.companyreport.domain.model.ValuationParams;

import java.math.BigDecimal;
import java.util.List;

/**
 * 파생 계산된 기업가치 (스냅샷 원시 입력 × 파라미터). 저장하지 않고 조회 시 계산한다.
 */
public record ReportValuation(
        ValuationParams params,
        LiquidationValuation liquidation,
        DcfValuation dcf
) {

    /**
     * 청산가치 = 조정자산 합계 − 총부채
     */
    public record LiquidationValuation(
            String baseYear,
            List<CategoryLine> lines,
            BigDecimal adjustedAssetTotal,
            BigDecimal totalLiabilities,
            BigDecimal liquidationValue,
            BigDecimal marketCap,
            BigDecimal marketCapToLiquidation
    ) {}

    public record CategoryLine(
            String key,
            String name,
            BigDecimal bookValue,
            BigDecimal ratio,
            BigDecimal adjustedValue
    ) {}

    /**
     * DCF: 보수 = 순현금 + FCF/r, 낙관 = 순현금 + n년 g성장 후 무성장 영구가치 할인
     */
    public record DcfValuation(
            boolean calculable,
            String reason,
            BigDecimal netCash,
            BigDecimal fcf,
            BigDecimal conservativeValue,
            BigDecimal optimisticValue,
            BigDecimal marketCap,
            BigDecimal marketCapToConservative,
            BigDecimal marketCapToOptimistic
    ) {}
}
