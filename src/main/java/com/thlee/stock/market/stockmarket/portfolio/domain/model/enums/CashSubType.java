package com.thlee.stock.market.stockmarket.portfolio.domain.model.enums;

public enum CashSubType {
    DEPOSIT("예금"),
    SAVINGS("적금"),
    CMA("CMA");

    private final String description;

    CashSubType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
