package com.thlee.stock.market.stockmarket.economics.derivedindicator.presentation.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.thlee.stock.market.stockmarket.economics.derivedindicator.domain.model.FormulaOperand;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

/**
 * 수식 피연산자 요청. INDICATOR면 className/keystatName, CONSTANT면 value.
 * 타입별 필수 필드는 @AssertTrue로 검증(누락 시 무음 변환 방지). 지표 존재·카테고리는 도메인 Validator가 검증.
 */
public record FormulaOperandRequest(
        @NotNull FormulaOperand.Type type,
        String className,
        String keystatName,
        Double value
) {
    /** CONSTANT는 value 필수(null→0 무음 변환 금지), INDICATOR는 className/keystatName 필수. */
    @JsonIgnore
    @AssertTrue(message = "operand 타입별 필수 값이 누락되었습니다.")
    public boolean isOperandComplete() {
        if (type == null) {
            return false;
        }
        return switch (type) {
            case CONSTANT -> value != null;
            case INDICATOR -> className != null && !className.isBlank()
                    && keystatName != null && !keystatName.isBlank();
        };
    }

    public FormulaOperand toDomain() {
        if (type == FormulaOperand.Type.CONSTANT) {
            return FormulaOperand.constant(value);
        }
        return FormulaOperand.indicator(className, keystatName);
    }
}
