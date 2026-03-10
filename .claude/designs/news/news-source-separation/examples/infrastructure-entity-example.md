# 인프라 Entity 예시

## NewsSourceEntity (신규)

```java
package com.thlee.stock.market.stockmarket.news.infrastructure.persistence;

import com.thlee.stock.market.stockmarket.news.domain.model.NewsPurpose;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * NewsSource JPA Entity
 * 사용자-뉴스 수집 출처 매핑 (region은 News에 포함)
 */
@Entity
@Table(
        name = "news_source",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"news_id", "user_id", "purpose", "source_id"})
        },
        indexes = {
                @Index(name = "idx_news_source_user_purpose_source",
                       columnList = "user_id, purpose, source_id, created_at DESC")
        }
)
@Getter
public class NewsSourceEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "news_id", nullable = false)
    private Long newsId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", nullable = false, length = 100)
    private NewsPurpose purpose;

    @Column(name = "source_id", nullable = false)
    private Long sourceId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected NewsSourceEntity() {
    }

    public NewsSourceEntity(Long id,
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

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
```

## NewsEntity (변경 후)

```java
package com.thlee.stock.market.stockmarket.news.infrastructure.persistence;

import com.thlee.stock.market.stockmarket.news.domain.model.Region;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * News JPA Entity
 * 뉴스 콘텐츠 + region 저장 (수집 출처는 NewsSourceEntity로 분리)
 */
@Entity
@Table(
        name = "news",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "original_url")
        }
)
@Getter
public class NewsEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "original_url", nullable = false, length = 500)
    private String originalUrl;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "published_at", nullable = false)
    private LocalDateTime publishedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    private Region region;

    protected NewsEntity() {
    }

    public NewsEntity(Long id,
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

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
```