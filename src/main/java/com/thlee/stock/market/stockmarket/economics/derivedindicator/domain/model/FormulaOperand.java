package com.thlee.stock.market.stockmarket.economics.derivedindicator.domain.model;

import lombok.Getter;

/**
 * 파생지표 수식의 피연산자.
 * <p>
 * INDICATOR: ECOS 원시 지표 참조 — (className, keystatName) 복합키로 식별. value는 null.
 * CONSTANT : 사용자 입력 상수 — value 보유. className/keystatName은 null.
 */
@Getter
public class FormulaOperand {

    public enum Type {
        INDICATOR,
        CONSTANT
    }

    private final Type type;
    private final String className;
    private final String keystatName;
    private final Double value;

    private FormulaOperand(Type type, String className, String keystatName, Double value) {
        this.type = type;
        this.className = className;
        this.keystatName = keystatName;
        this.value = value;
    }

    public static FormulaOperand indicator(String className, String keystatName) {
        return new FormulaOperand(Type.INDICATOR, className, keystatName, null);
    }

    public static FormulaOperand constant(double value) {
        return new FormulaOperand(Type.CONSTANT, null, null, value);
    }

    public boolean isIndicator() {
        return type == Type.INDICATOR;
    }

    /**
     * 원시 지표 식별 복합키. EcosIndicatorLatest.toCompareKey()와 동일 규칙(className::keystatName).
     */
    public String compareKey() {
        return className + "::" + keystatName;
    }
}
