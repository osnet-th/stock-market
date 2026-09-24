package com.thlee.stock.market.stockmarket.salary.application.dto;

import com.thlee.stock.market.stockmarket.salary.domain.model.SpendingItem;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * 카테고리 하위 항목 응답.
 */
@Getter
public class SpendingItemResponse {

    private final String name;
    private final BigDecimal amount;
    private final boolean fixed;

    private SpendingItemResponse(String name, BigDecimal amount, boolean fixed) {
        this.name = name;
        this.amount = amount;
        this.fixed = fixed;
    }

    public static SpendingItemResponse from(SpendingItem item) {
        return new SpendingItemResponse(item.getName(), item.getAmount(), item.isFixed());
    }
}
