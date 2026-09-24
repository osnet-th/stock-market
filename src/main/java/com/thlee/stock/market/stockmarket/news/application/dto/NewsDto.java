package com.thlee.stock.market.stockmarket.news.application.dto;

import com.thlee.stock.market.stockmarket.news.domain.model.News;

import java.time.LocalDateTime;

/**
 * 저장된 뉴스 응답 DTO
 */
public class NewsDto {
    private final Long id;
    private final String originalUrl;
    private final String title;
    private final String content;
    private final LocalDateTime publishedAt;
    private final LocalDateTime createdAt;
    private final Long keywordId;
    private final String source;

    public NewsDto(Long id,
                   String originalUrl,
                   String title,
                   String content,
                   LocalDateTime publishedAt,
                   LocalDateTime createdAt,
                   Long keywordId,
                   String source) {
        this.id = id;
        this.originalUrl = originalUrl;
        this.title = title;
        this.content = content;
        this.publishedAt = publishedAt;
        this.createdAt = createdAt;
        this.keywordId = keywordId;
        this.source = source;
    }

    public static NewsDto from(News news) {
        return new NewsDto(
                news.getId(),
                news.getOriginalUrl(),
                news.getTitle(),
                news.getContent(),
                news.getPublishedAt(),
                news.getCreatedAt(),
                news.getKeywordId(),
                news.getSource()
        );
    }

    public Long getId() { return id; }
    public String getOriginalUrl() { return originalUrl; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public LocalDateTime getPublishedAt() { return publishedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public Long getKeywordId() { return keywordId; }
    /** 언론사명. 기존 데이터는 null → 프론트가 originalUrl 도메인으로 폴백한다 (#115). */
    public String getSource() { return source; }
}
