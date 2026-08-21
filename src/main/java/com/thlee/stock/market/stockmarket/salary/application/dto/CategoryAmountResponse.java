package com.thlee.stock.market.stockmarket.salary.application.dto;

import lombok.Getter;

import java.math.BigDecimal;

/**
 * 카테고리별 금액 한 쌍 — 추이 포인트 구성/전월 비교 공용. category는 사용자 카테고리 code.
 */
@Getter
public class CategoryAmountResponse {

    private final String category;
    private final BigDecimal amount;

    public CategoryAmountResponse(String category, BigDecimal amount) {
        this.category = category;
        this.amount = amount;
    }
}
