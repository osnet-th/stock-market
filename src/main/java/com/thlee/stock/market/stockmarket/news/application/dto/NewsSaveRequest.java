package com.thlee.stock.market.stockmarket.news.application.dto;

import java.time.LocalDateTime;

/**
 * 뉴스 저장 요청 DTO
 */
public class NewsSaveRequest {
    private final String originalUrl;
    private final Long userId;
    private final String title;
    private final String content;
    private final LocalDateTime publishedAt;
    private final String searchKeyword;

    public NewsSaveRequest(String originalUrl,
                           Long userId,
                           String title,
                           String content,
                           LocalDateTime publishedAt,
                           String searchKeyword) {
        this.originalUrl = originalUrl;
        this.userId = userId;
        this.title = title;
        this.content = content;
        this.publishedAt = publishedAt;
        this.searchKeyword = searchKeyword;
    }

    public String getOriginalUrl() {
        return originalUrl;
    }

    public Long getUserId() {
        return userId;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public String getSearchKeyword() {
        return searchKeyword;
    }
}
