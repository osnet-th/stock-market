package com.thlee.stock.market.stockmarket.economics.derivedindicator.presentation.dto;

import com.thlee.stock.market.stockmarket.economics.derivedindicator.application.dto.EvaluatedDerivedIndicator;
import com.thlee.stock.market.stockmarket.economics.derivedindicator.domain.model.UserDerivedIndicator;
import com.thlee.stock.market.stockmarket.economics.derivedindicator.domain.service.DerivedFormulaEvaluator;

/**
 * 파생지표 응답. 계산값은 computable=false면 null이며 reason으로 사유 전달("계산 불가").
 */
public record DerivedIndicatorResponse(
        Long id,
        String name,
        String unit,
        String description,
        String category,
        DerivedFormulaResponse formula,
        boolean computable,
        Double value,
        String reason
) {
    public static DerivedIndicatorResponse from(EvaluatedDerivedIndicator evaluated) {
        UserDerivedIndicator indicator = evaluated.indicator();
        DerivedFormulaEvaluator.EvaluationResult result = evaluated.result();
        return new DerivedIndicatorResponse(
                indicator.getId(),
                indicator.getName(),
                indicator.getUnit(),
                indicator.getDescription(),
                indicator.getCategory() == null ? null : indicator.getCategory().name(),
                DerivedFormulaResponse.from(indicator.getFormula()),
                result.computable(),
                result.value(),
                result.reason() == null ? null : result.reason().name()
        );
    }

    /** 계산값이 불필요한 단건 응답(생성/수정 직후). */
    public static DerivedIndicatorResponse of(UserDerivedIndicator indicator) {
        return new DerivedIndicatorResponse(
                indicator.getId(),
                indicator.getName(),
                indicator.getUnit(),
                indicator.getDescription(),
                indicator.getCategory() == null ? null : indicator.getCategory().name(),
                DerivedFormulaResponse.from(indicator.getFormula()),
                false,
                null,
                null
        );
    }
}
