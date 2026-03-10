# 도메인 모델 예시

## NewsSource 도메인 모델 (신규)

```java
package com.thlee.stock.market.stockmarket.news.domain.model;

import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 뉴스 수집 출처 도메인 모델
 * 사용자가 어떤 목적(keyword/portfolio)으로 뉴스를 수집했는지 관리
 * region은 News에 포함 (API 검색 출처는 뉴스 콘텐츠의 고유 속성)
 */
@Getter
public class NewsSource {
    private Long id;
    private Long newsId;
    private Long userId;
    private NewsPurpose purpose;
    private Long sourceId;
    private LocalDateTime createdAt;

    private NewsSource(Long newsId,
                       Long userId,
                       NewsPurpose purpose,
                       Long sourceId,
                       LocalDateTime createdAt) {
        this.newsId = newsId;
        this.userId = userId;
        this.purpose = purpose;
        this.sourceId = sourceId;
        this.createdAt = createdAt;
    }

    /**
     * 재구성용 생성자 (Repository에서 조회 시 사용)
     */
    public NewsSource(Long id,
                      Long newsId,
                      Long userId,
                      NewsPurpose purpose,
                      Long sourceId,
                      LocalDateTime createdAt) {
        this.id = id;
        this.newsId = newsId;
        this.userId = userId;
        this.purpose = purpose;
        this.sourceId = sourceId;
        this.createdAt = createdAt;
    }

    public static NewsSource create(Long newsId,
                                    Long userId,
                                    NewsPurpose purpose,
                                    Long sourceId) {
        validateNewsId(newsId);
        validateUserId(userId);
        validatePurpose(purpose);
        validateSourceId(sourceId);

        return new NewsSource(newsId, userId, purpose, sourceId, LocalDateTime.now());
    }

    private static void validateNewsId(Long newsId) {
        if (newsId == null) {
            throw new IllegalArgumentException("newsId는 필수입니다.");
        }
    }

    private static void validateUserId(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId는 필수입니다.");
        }
    }

    private static void validatePurpose(NewsPurpose purpose) {
        if (purpose == null) {
            throw new IllegalArgumentException("purpose는 필수입니다.");
        }
    }

    private static void validateSourceId(Long sourceId) {
        if (sourceId == null) {
            throw new IllegalArgumentException("sourceId는 필수입니다.");
        }
    }
}
```

## News 도메인 모델 (변경 후)

```java
package com.thlee.stock.market.stockmarket.news.domain.model;

import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 뉴스 도메인 모델
 * 뉴스 콘텐츠 + region(API 검색 출처) 관리
 * 수집 출처 정보(userId, purpose, sourceId)는 NewsSource로 분리
 */
@Getter
public class News {
    private Long id;
    private String originalUrl;
    private String title;
    private String content;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;
    private Region region;

    private News(String originalUrl,
                 String title,
                 String content,
                 LocalDateTime publishedAt,
                 LocalDateTime createdAt,
                 Region region) {
        this.originalUrl = originalUrl;
        this.title = title;
        this.content = content;
        this.publishedAt = publishedAt;
        this.createdAt = createdAt;
        this.region = region;
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
                Region region) {
        this.id = id;
        this.originalUrl = originalUrl;
        this.title = title;
        this.content = content;
        this.publishedAt = publishedAt;
        this.createdAt = createdAt;
        this.region = region;
    }

    public static News create(String originalUrl,
                              String title,
                              String content,
                              LocalDateTime publishedAt,
                              Region region) {
        validateOriginalUrl(originalUrl);
        validateTitle(title);
        validatePublishedAt(publishedAt);

        return new News(originalUrl, title, content, publishedAt, LocalDateTime.now(), region);
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
}
```