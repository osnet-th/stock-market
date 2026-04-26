package com.thlee.stock.market.stockmarket.stocknote.domain.model;

import com.thlee.stock.market.stockmarket.stocknote.domain.model.enums.VsAverageLevel;

import java.math.BigDecimal;

/**
 * 기록 작성 시점의 밸류에이션 지표 묶음 (StockNote 의 1:1 흡수 그룹).
 * 모든 필드 nullable — 사용자가 일부만 입력 가능.
 */
public record Valuation(
        BigDecimal per,
        BigDecimal pbr,
        BigDecimal evEbitda,
        VsAverageLevel vsAverage
) {
    public static Valuation empty() {
        return new Valuation(null, null, null, null);
    }
}
