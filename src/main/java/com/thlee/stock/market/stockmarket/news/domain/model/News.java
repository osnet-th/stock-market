package com.thlee.stock.market.stockmarket.news.domain.model;

import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 뉴스 도메인 모델
 */
@Getter
public class News {
    private Long id;
    private String originalUrl;
    private String title;
    private String content;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;
    private Long keywordId;
    private Region region;
    /** 언론사명 (#115). 기존 데이터는 null 이라 표시 측에서 도메인으로 폴백한다. */
    private String source;

    private News(String originalUrl,
                 String title,
                 String content,
                 LocalDateTime publishedAt,
                 LocalDateTime createdAt,
                 Long keywordId,
                 Region region,
                 String source) {
        this.originalUrl = originalUrl;
        this.title = title;
        this.content = content;
        this.publishedAt = publishedAt;
        this.createdAt = createdAt;
        this.keywordId = keywordId;
        this.region = region;
        this.source = source;
    }

    /**
     * 재구성용 생성자 (Repository에서 조회 시 사용)
     */
    public News(Long id,
                String originalUrl,
                String title,
                String content,
                LocalDateTime publishedAt,
                LocalDateTime createdAt,
                Long keywordId,
                Region region,
                String source) {
        this.id = id;
        this.originalUrl = originalUrl;
        this.title = title;
        this.content = content;
        this.publishedAt = publishedAt;
        this.createdAt = createdAt;
        this.keywordId = keywordId;
        this.region = region;
        this.source = source;
    }

    /**
     * 뉴스 생성
     */
    public static News create(String originalUrl,
                              String title,
                              String content,
                              LocalDateTime publishedAt,
                              Long keywordId,
                              Region region,
                              String source) {
        validateOriginalUrl(originalUrl);
        validateTitle(title);
        validatePublishedAt(publishedAt);
        validateKeywordId(keywordId);

        return new News(
                originalUrl,
                title,
                content,
                publishedAt,
                LocalDateTime.now(),
                keywordId,
                region,
                source
        );
    }

    private static void validateOriginalUrl(String originalUrl) {
        if (originalUrl == null || originalUrl.isBlank()) {
            throw new IllegalArgumentException("originalUrl은 필수입니다.");
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

    private static void validateKeywordId(Long keywordId) {
        if (keywordId == null) {
            throw new IllegalArgumentException("keywordId는 필수입니다.");
        }
    }
}
