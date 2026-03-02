package com.thlee.stock.market.stockmarket.news.infrastructure.persistence;

import com.thlee.stock.market.stockmarket.news.domain.model.NewsPurpose;
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
        }
)
@Getter
public class NewsEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "original_url", nullable = false, length = 500)
    private String originalUrl;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "published_at", nullable = false)
    private LocalDateTime publishedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", nullable = false, length = 100)
    private NewsPurpose purpose;

    @Column(name = "search_keyword", nullable = false)
    private String searchKeyword;

    @Enumerated(EnumType.STRING)
    private Region region;

    protected NewsEntity() {
    }

    public NewsEntity(Long id,
                      String originalUrl,
                      Long userId,
                      String title,
                      String content,
                      LocalDateTime publishedAt,
                      LocalDateTime createdAt,
                      NewsPurpose purpose,
                      String searchKeyword,
                      Region region) {
        this.id = id;
        this.originalUrl = originalUrl;
        this.userId = userId;
        this.title = title;
        this.content = content;
        this.publishedAt = publishedAt;
        this.createdAt = createdAt;
        this.purpose = purpose;
        this.searchKeyword = searchKeyword;
        this.region = region;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
