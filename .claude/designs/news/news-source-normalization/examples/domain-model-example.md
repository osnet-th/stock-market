# News 도메인 모델 예시

```java
package com.thlee.stock.market.stockmarket.news.domain.model;

/**
 * 뉴스 도메인 모델
 */
@Getter
public class News {
    private Long id;
    private String originalUrl;
    private Long userId;
    private String title;
    private String content;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;
    private NewsPurpose purpose;
    private Long sourceId;    // keyword.id 또는 향후 stock.id
    private Region region;

    private News(String originalUrl,
                 Long userId,
                 String title,
                 String content,
                 LocalDateTime publishedAt,
                 LocalDateTime createdAt,
                 NewsPurpose purpose,
                 Long sourceId,
                 Region region) {
        this.originalUrl = originalUrl;
        this.userId = userId;
        this.title = title;
        this.content = content;
        this.publishedAt = publishedAt;
        this.createdAt = createdAt;
        this.purpose = purpose;
        this.sourceId = sourceId;
        this.region = region;
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
                Long sourceId,
                Region region) {
        this.id = id;
        this.originalUrl = originalUrl;
        this.userId = userId;
        this.title = title;
        this.content = content;
        this.publishedAt = publishedAt;
        this.createdAt = createdAt;
        this.purpose = purpose;
        this.sourceId = sourceId;
        this.region = region;
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
                              Long sourceId,
                              Region region) {
        validateOriginalUrl(originalUrl);
        validateUserId(userId);
        validateTitle(title);
        validatePublishedAt(publishedAt);
        validatePurpose(purpose);
        validateSourceId(sourceId);

        return new News(
                originalUrl,
                userId,
                title,
                content,
                publishedAt,
                LocalDateTime.now(),
                purpose,
                sourceId,
                region
        );
    }

    // ... 기존 validate 메서드 유지

    private static void validateSourceId(Long sourceId) {
        if (sourceId == null) {
            throw new IllegalArgumentException("sourceId는 필수입니다.");
        }
    }
}
```
