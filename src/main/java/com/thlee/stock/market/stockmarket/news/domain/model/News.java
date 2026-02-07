package com.thlee.stock.market.stockmarket.news.domain.model;

import java.time.LocalDateTime;

/**
 * 뉴스 도메인 모델
 */
public class News {
    private Long id;
    private String originalUrl;
    private Long userId;
    private String title;
    private String content;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;
    private NewsPurpose purpose;
    private String searchKeyword;

    private News(String originalUrl,
                 Long userId,
                 String title,
                 String content,
                 LocalDateTime publishedAt,
                 LocalDateTime createdAt,
                 NewsPurpose purpose,
                 String searchKeyword) {
        this.originalUrl = originalUrl;
        this.userId = userId;
        this.title = title;
        this.content = content;
        this.publishedAt = publishedAt;
        this.createdAt = createdAt;
        this.purpose = purpose;
        this.searchKeyword = searchKeyword;
    }

    /**
     * 재구성용 생성자 (Repository에서 조회 시 사용)
     */
    public News(Long id,
                String originalUrl,
                Long userId,
                String title,
                String content,
                LocalDateTime publishedAt,
                LocalDateTime createdAt,
                NewsPurpose purpose,
                String searchKeyword) {
        this.id = id;
        this.originalUrl = originalUrl;
        this.userId = userId;
        this.title = title;
        this.content = content;
        this.publishedAt = publishedAt;
        this.createdAt = createdAt;
        this.purpose = purpose;
        this.searchKeyword = searchKeyword;
    }

    /**
     * 뉴스 생성
     */
    public static News create(String originalUrl,
                              Long userId,
                              String title,
                              String content,
                              LocalDateTime publishedAt,
                              NewsPurpose purpose,
                              String searchKeyword) {
        validateOriginalUrl(originalUrl);
        validateUserId(userId);
        validateTitle(title);
        validatePublishedAt(publishedAt);
        validatePurpose(purpose);
        validateSearchKeyword(searchKeyword);

        return new News(
                originalUrl,
                userId,
                title,
                content,
                publishedAt,
                LocalDateTime.now(),
                purpose,
                searchKeyword
        );
    }

    private static void validateOriginalUrl(String originalUrl) {
        if (originalUrl == null || originalUrl.isBlank()) {
            throw new IllegalArgumentException("originalUrl은 필수입니다.");
        }
    }

    private static void validateUserId(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId는 필수입니다.");
        }
    }

    private static void validateTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("title은 필수입니다.");
        }
    }

    private static void validatePublishedAt(LocalDateTime publishedAt) {
        if (publishedAt == null) {
            throw new IllegalArgumentException("publishedAt은 필수입니다.");
        }
    }

    private static void validatePurpose(NewsPurpose purpose) {
        if (purpose == null) {
            throw new IllegalArgumentException("purpose는 필수입니다.");
        }
    }

    private static void validateSearchKeyword(String searchKeyword) {
        if (searchKeyword == null || searchKeyword.isBlank()) {
            throw new IllegalArgumentException("searchKeyword는 필수입니다.");
        }
    }

    public Long getId() {
        return id;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public NewsPurpose getPurpose() {
        return purpose;
    }

    public String getSearchKeyword() {
        return searchKeyword;
    }
}
