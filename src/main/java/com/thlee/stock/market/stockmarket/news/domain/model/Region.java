package com.thlee.stock.market.stockmarket.news.domain.model;

/**
 * 키워드 지역 구분
 */
public enum Region {
    DOMESTIC("국내"),
    INTERNATIONAL("해외");

    private final String description;

    Region(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}