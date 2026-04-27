package com.thlee.stock.market.stockmarket.portfolio.domain.model.enums;

public enum PortfolioItemStatus {
    ACTIVE("보유 중"),
    CLOSED("전량 매도 완료");

    private final String description;

    PortfolioItemStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}