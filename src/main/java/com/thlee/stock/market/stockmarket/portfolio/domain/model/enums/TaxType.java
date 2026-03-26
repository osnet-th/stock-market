package com.thlee.stock.market.stockmarket.portfolio.domain.model.enums;

public enum TaxType {
    GENERAL("일반과세"),
    TAX_FREE("비과세"),
    TAX_FAVORED("세금우대");

    private final String description;

    TaxType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}