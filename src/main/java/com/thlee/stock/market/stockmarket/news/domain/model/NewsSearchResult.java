package com.thlee.stock.market.stockmarket.news.domain.model;

import java.time.LocalDateTime;

/**
 * 뉴스 조회 전용 모델
 */
public class NewsSearchResult {
    private final String title;
    private final String url;
    private final String content;
    private final LocalDateTime publishedAt;

    public NewsSearchResult(String title, String url, String content, LocalDateTime publishedAt) {
        this.title = title;
        this.url = url;
        this.content = content;
        this.publishedAt = publishedAt;
    }

    public String getTitle() {
        return title;
    }

    public String getUrl() {
        return url;
    }

    public String getContent() {
        return content;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }
}
