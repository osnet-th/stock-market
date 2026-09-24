package com.thlee.stock.market.stockmarket.news.application.dto;

import com.thlee.stock.market.stockmarket.news.domain.model.NewsSearchResult;

import java.time.LocalDateTime;

/**
 * 뉴스 조회 응답 DTO
 */
public class NewsResultDto {
    private final String title;
    private final String url;
    private final String content;
    private final LocalDateTime publishedAt;
    private final String source;

    public NewsResultDto(String title, String url, String content, LocalDateTime publishedAt, String source) {
        this.title = title;
        this.url = url;
        this.content = content;
        this.publishedAt = publishedAt;
        this.source = source;
    }

    public static NewsResultDto from(NewsSearchResult result) {
        return new NewsResultDto(
                result.getTitle(),
                result.getUrl(),
                result.getContent(),
                result.getPublishedAt(),
                result.getSource()
        );
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

    public String getSource() {
        return source;
    }
}
