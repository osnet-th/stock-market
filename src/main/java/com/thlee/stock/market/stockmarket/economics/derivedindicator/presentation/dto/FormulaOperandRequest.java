package com.thlee.stock.market.stockmarket.economics.derivedindicator.presentation.dto;

import com.thlee.stock.market.stockmarket.economics.derivedindicator.domain.model.FormulaOperand;
import jakarta.validation.constraints.NotNull;

/**
 * 수식 피연산자 요청. INDICATOR면 className/keystatName, CONSTANT면 value.
 * 상세 정합성(타입별 필드 충족, 지표 존재 등)은 도메인 Validator가 검증.
 */
public record FormulaOperandRequest(
        @NotNull FormulaOperand.Type type,
        String className,
        String keystatName,
        Double value
) {
    public FormulaOperand toDomain() {
        if (type == FormulaOperand.Type.CONSTANT) {
            return FormulaOperand.constant(value == null ? 0.0 : value);
        }
        return FormulaOperand.indicator(className, keystatName);
    }
}
