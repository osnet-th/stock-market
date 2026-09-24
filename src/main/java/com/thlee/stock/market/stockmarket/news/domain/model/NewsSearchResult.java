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
    private final String source;

    public NewsSearchResult(String title, String url, String content, LocalDateTime publishedAt, String source) {
        this.title = title;
        this.url = url;
        this.content = content;
        this.publishedAt = publishedAt;
        this.source = source;
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

    /**
     * 언론사명 (#115). 외부 API 가 주지 않으면 어댑터가 URL 도메인으로 파생한다.
     */
    public String getSource() {
        return source;
    }
}
