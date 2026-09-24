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
    private final boolean read;
    private final boolean saved;

    public NewsDto(Long id,
                   String originalUrl,
                   String title,
                   String content,
                   LocalDateTime publishedAt,
                   LocalDateTime createdAt,
                   Long keywordId,
                   String source,
                   boolean read,
                   boolean saved) {
        this.id = id;
        this.originalUrl = originalUrl;
        this.title = title;
        this.content = content;
        this.publishedAt = publishedAt;
        this.createdAt = createdAt;
        this.keywordId = keywordId;
        this.source = source;
        this.read = read;
        this.saved = saved;
    }

    /** 읽음/저장 상태가 필요 없는 경로(수집 결과 등)에서 쓴다 — 둘 다 false 로 본다. */
    public static NewsDto from(News news) {
        return from(news, false, false);
    }

    public static NewsDto from(News news, boolean read, boolean saved) {
        return new NewsDto(
                news.getId(),
                news.getOriginalUrl(),
                news.getTitle(),
                news.getContent(),
                news.getPublishedAt(),
                news.getCreatedAt(),
                news.getKeywordId(),
                news.getSource(),
                read,
                saved
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
    /** 내가 읽었는지 (#115). 상태 행이 없으면 false. */
    public boolean isRead() { return read; }
    /** 내가 저장(★)했는지 (#115). */
    public boolean isSaved() { return saved; }
}
