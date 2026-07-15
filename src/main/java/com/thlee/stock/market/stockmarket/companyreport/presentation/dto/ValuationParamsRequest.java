package com.thlee.stock.market.stockmarket.companyreport.presentation.dto;

import com.thlee.stock.market.stockmarket.companyreport.domain.model.ValuationParams;

import java.math.BigDecimal;

/**
 * 가치평가 파라미터 요청. 누락 필드는 시스템 기본값으로 채운다.
 */
public record ValuationParamsRequest(
        BigDecimal discountRate,
        BigDecimal optimisticGrowthRate,
        Integer optimisticGrowthYears,
        AdjustmentRatiosRequest adjustmentRatios
) {

    public record AdjustmentRatiosRequest(
            BigDecimal cash,
            BigDecimal securities,
            BigDecimal receivables,
            BigDecimal inventory,
            BigDecimal investments,
            BigDecimal tangible,
            BigDecimal intangible,
            BigDecimal otherCurrent
    ) {
        ValuationParams.AdjustmentRatios toRatios() {
            ValuationParams.AdjustmentRatios defaults = ValuationParams.AdjustmentRatios.defaults();
            return new ValuationParams.AdjustmentRatios(
                    orDefault(cash, defaults.cash()),
                    orDefault(securities, defaults.securities()),
                    orDefault(receivables, defaults.receivables()),
                    orDefault(inventory, defaults.inventory()),
                    orDefault(investments, defaults.investments()),
                    orDefault(tangible, defaults.tangible()),
                    orDefault(intangible, defaults.intangible()),
                    orDefault(otherCurrent, defaults.otherCurrent()));
        }
    }

    public static ValuationParams toParams(ValuationParamsRequest request) {
        ValuationParams defaults = ValuationParams.defaults();
        if (request == null) {
            return defaults;
        }
        return new ValuationParams(
                orDefault(request.discountRate(), defaults.discountRate()),
                orDefault(request.optimisticGrowthRate(), defaults.optimisticGrowthRate()),
                request.optimisticGrowthYears() != null
                        ? request.optimisticGrowthYears() : defaults.optimisticGrowthYears(),
                request.adjustmentRatios() != null
                        ? request.adjustmentRatios().toRatios() : defaults.adjustmentRatios());
    }

    private static BigDecimal orDefault(BigDecimal value, BigDecimal defaultValue) {
        return value != null ? value : defaultValue;
    }
}
