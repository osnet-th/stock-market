package com.thlee.stock.market.stockmarket.economics.domain.model;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * 기준수익률 한 칸. 수익률은 % 단위이고, null이면 출처가 제공하지 않은 값(미제공)이다.
 * 등급은 회사채에만 있다.
 */
public record BondYield(BondYieldType type, BondCreditGrade grade, int maturityMonths, BigDecimal rate) {

    public BondYield {
        Objects.requireNonNull(type, "type");
        if (type.isGraded() != (grade != null)) {
            throw new IllegalArgumentException("등급은 회사채에만 지정한다: type=" + type + ", grade=" + grade);
        }
        if (maturityMonths <= 0) {
            throw new IllegalArgumentException("만기는 1개월 이상이어야 한다: " + maturityMonths);
        }
    }

    public boolean isProvided() {
        return rate != null;
    }
}
