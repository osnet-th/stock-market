package com.thlee.stock.market.stockmarket.portfolio.domain.model;

import lombok.Getter;

import java.math.BigDecimal;

/**
 * 금 항목 상세 (보유 중량 기준 시세 평가용)
 */
@Getter
public class GoldDetail {

    private final BigDecimal quantityGrams;

    public GoldDetail(BigDecimal quantityGrams) {
        if (quantityGrams == null || quantityGrams.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("보유 중량(g)은 0보다 커야 합니다.");
        }
        this.quantityGrams = quantityGrams;
    }
}
