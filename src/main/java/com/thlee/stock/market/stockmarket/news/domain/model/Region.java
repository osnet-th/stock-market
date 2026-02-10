package com.thlee.stock.market.stockmarket.news.domain.model;

/**
 * 키워드 지역 구분
 */
public enum KeywordRegion {
    DOMESTIC("국내"),
    INTERNATIONAL("해외");

    private final String description;

    KeywordRegion(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}