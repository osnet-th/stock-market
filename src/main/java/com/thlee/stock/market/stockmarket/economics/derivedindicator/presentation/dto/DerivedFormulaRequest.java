package com.thlee.stock.market.stockmarket.economics.derivedindicator.presentation.dto;

import com.thlee.stock.market.stockmarket.economics.derivedindicator.domain.model.DerivedFormula;
import com.thlee.stock.market.stockmarket.economics.derivedindicator.domain.model.FormulaOperand;
import com.thlee.stock.market.stockmarket.economics.derivedindicator.domain.model.FormulaOperator;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 구조형 수식 요청. 2~3항 제한을 DTO에서 1차 강제(거대 배열 fail-fast/DoS 방지),
 * 지표 존재·카테고리·항 정합성은 도메인 Validator가 최종 수행.
 */
public record DerivedFormulaRequest(
        @NotEmpty @Size(min = 2, max = 3) @Valid List<FormulaOperandRequest> operands,
        @NotEmpty @Size(min = 1, max = 2) List<FormulaOperator> operators
) {
    public DerivedFormula toDomain() {
        List<FormulaOperand> domainOperands = operands.stream()
                .map(FormulaOperandRequest::toDomain)
                .toList();
        return new DerivedFormula(domainOperands, operators);
    }
}
