package com.thlee.stock.market.stockmarket.news.infrastructure.persistence;

import com.thlee.stock.market.stockmarket.news.domain.model.KeywordRegion;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Keyword JPA Entity
 */
@Entity
@Table(name = "keyword")
public class KeywordEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "keyword", nullable = false, length = 100)
    private String keyword;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Enumerated(EnumType.STRING)
    @Column(name = "region", nullable = false, length = 20)
    private KeywordRegion region;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected KeywordEntity() {
    }

    public KeywordEntity(Long id, String keyword, Long userId, boolean active, KeywordRegion region, LocalDateTime createdAt) {
        this.id = id;
        this.keyword = keyword;
        this.userId = userId;
        this.active = active;
        this.region = region;
        this.createdAt = createdAt;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    // Getters
    public Long getId() {
        return id;
    }

    public String getKeyword() {
        return keyword;
    }

    public Long getUserId() {
        return userId;
    }

    public boolean isActive() {
        return active;
    }

    public KeywordRegion getRegion() {
        return region;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    // Setters (for JPA)
    public void setId(Long id) {
        this.id = id;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void setRegion(KeywordRegion region) {
        this.region = region;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}