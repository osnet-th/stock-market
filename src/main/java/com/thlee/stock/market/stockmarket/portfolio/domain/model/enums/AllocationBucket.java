package com.thlee.stock.market.stockmarket.portfolio.domain.model.enums;

public enum AllocationBucket {
    SAFE("안전자산"),
    INVEST("투자자산");

    private final String description;

    AllocationBucket(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
