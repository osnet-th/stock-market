# UserKeywordEntity 예시

```java
package com.thlee.stock.market.stockmarket.news.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "user_keyword",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_id", "keyword_id"})
        },
        indexes = {
                @Index(name = "idx_user_keyword_user_active", columnList = "user_id, active"),
                @Index(name = "idx_user_keyword_keyword", columnList = "keyword_id")
        }
)
@Getter
public class UserKeywordEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "keyword_id", nullable = false)
    private Long keywordId;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected UserKeywordEntity() {
    }

    public UserKeywordEntity(Long id, Long userId, Long keywordId, boolean active,
                              LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.keywordId = keywordId;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = LocalDateTime.now();
    }
}
```