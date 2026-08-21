package com.thlee.stock.market.stockmarket.salary.application.dto;

import com.thlee.stock.market.stockmarket.salary.domain.model.enums.SpendingCategory;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * 카테고리별 금액 한 쌍 — 추이 포인트 구성/전월 비교 공용.
 */
@Getter
public class CategoryAmountResponse {

    private final SpendingCategory category;
    private final BigDecimal amount;

    public CategoryAmountResponse(SpendingCategory category, BigDecimal amount) {
        this.category = category;
        this.amount = amount;
    }
}
