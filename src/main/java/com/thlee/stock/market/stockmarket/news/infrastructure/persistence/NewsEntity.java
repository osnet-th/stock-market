package com.thlee.stock.market.stockmarket.news.infrastructure.persistence;

import com.thlee.stock.market.stockmarket.news.domain.model.Region;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * News JPA Entity
 */
@Entity
@Table(
        name = "news",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "original_url")
        },
        indexes = {
                @Index(name = "idx_news_keyword_published",
                       columnList = "keyword_id, published_at DESC")
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

    @Column(name = "keyword_id", nullable = false)
    private Long keywordId;

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
                      Long keywordId,
                      Region region) {
        this.id = id;
        this.originalUrl = originalUrl;
        this.title = title;
        this.content = content;
        this.publishedAt = publishedAt;
        this.createdAt = createdAt;
        this.keywordId = keywordId;
        this.region = region;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
