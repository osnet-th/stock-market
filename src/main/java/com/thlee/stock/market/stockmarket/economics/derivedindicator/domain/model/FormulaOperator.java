package com.thlee.stock.market.stockmarket.economics.derivedindicator.domain.model;

/**
 * 파생지표 수식 연산자. 화이트리스트 — 이 4개만 허용한다.
 * precedence: ×÷(2)가 +−(1)보다 우선. 평가기는 우선순위가 높은 연산을 먼저 접는다.
 */
public enum FormulaOperator {

    ADD("+", 1),
    SUB("-", 1),
    MUL("×", 2),
    DIV("÷", 2);

    private final String symbol;
    private final int precedence;

    FormulaOperator(String symbol, int precedence) {
        this.symbol = symbol;
        this.precedence = precedence;
    }

    public String getSymbol() {
        return symbol;
    }

    public int getPrecedence() {
        return precedence;
    }
}
