package com.thlee.stock.market.stockmarket.economics.derivedindicator.domain.model;

import lombok.Getter;

import java.util.List;

/**
 * 파생지표 수식 값 객체(VO). 2~3항 제한 구조형.
 * <p>
 * operands 개수 = 2 또는 3, operators 개수 = operands 개수 - 1.
 * 평가는 연산자 우선순위(×÷ 먼저)를 적용한다(괄호 없음). 자세한 검증은 DerivedFormulaValidator.
 */
@Getter
public class DerivedFormula {

    private final List<FormulaOperand> operands;
    private final List<FormulaOperator> operators;

    public DerivedFormula(List<FormulaOperand> operands, List<FormulaOperator> operators) {
        this.operands = operands == null ? List.of() : List.copyOf(operands);
        this.operators = operators == null ? List.of() : List.copyOf(operators);
    }

    public List<FormulaOperand> indicatorOperands() {
        return operands.stream().filter(FormulaOperand::isIndicator).toList();
    }
}
