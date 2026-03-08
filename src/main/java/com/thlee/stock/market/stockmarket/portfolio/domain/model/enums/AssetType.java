package com.thlee.stock.market.stockmarket.portfolio.domain.model.enums;

public enum AssetType {
    STOCK("주식"),
    BOND("채권"),
    REAL_ESTATE("부동산"),
    FUND("펀드"),
    CRYPTO("암호화폐"),
    GOLD("금"),
    COMMODITY("원자재"),
    CASH("현금성 자산"),
    OTHER("기타");

    private final String description;

    AssetType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
