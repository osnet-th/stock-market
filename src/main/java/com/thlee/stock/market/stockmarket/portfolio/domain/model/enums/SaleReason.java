package com.thlee.stock.market.stockmarket.portfolio.domain.model.enums;

public enum SaleReason {
    TARGET_PRICE_REACHED("목표가 도달"),
    STOP_LOSS("손절"),
    CASH_NEEDED("현금 확보"),
    REBALANCING("리밸런싱"),
    OTHER("기타");

    private final String description;

    SaleReason(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}