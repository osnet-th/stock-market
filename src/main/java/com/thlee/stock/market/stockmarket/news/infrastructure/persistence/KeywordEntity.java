package com.thlee.stock.market.stockmarket.news.infrastructure.persistence;

import com.thlee.stock.market.stockmarket.news.domain.model.Region;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Keyword JPA Entity (공유 리소스)
 */
@Entity
@Table(name = "keyword",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"keyword", "region"})
        })
@Getter
public class KeywordEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "keyword", nullable = false, length = 100)
    private String keyword;

    @Enumerated(EnumType.STRING)
    @Column(name = "region", nullable = false, length = 20)
    private Region region;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected KeywordEntity() {
    }

    public KeywordEntity(Long id, String keyword, Region region, LocalDateTime createdAt) {
        this.id = id;
        this.keyword = keyword;
        this.region = region;
        this.createdAt = createdAt;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
