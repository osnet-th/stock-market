package com.thlee.stock.market.stockmarket.economics.derivedindicator.presentation.dto;

import com.thlee.stock.market.stockmarket.economics.derivedindicator.domain.model.DerivedFormula;
import com.thlee.stock.market.stockmarket.economics.derivedindicator.domain.model.FormulaOperand;
import com.thlee.stock.market.stockmarket.economics.derivedindicator.domain.model.FormulaOperator;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * 구조형 수식 요청. operands/operators 개수·항 검증은 도메인 Validator가 최종 수행.
 */
public record DerivedFormulaRequest(
        @NotEmpty @Valid List<FormulaOperandRequest> operands,
        @NotEmpty List<FormulaOperator> operators
) {
    public DerivedFormula toDomain() {
        List<FormulaOperand> domainOperands = operands.stream()
                .map(FormulaOperandRequest::toDomain)
                .toList();
        return new DerivedFormula(domainOperands, operators);
    }
}
