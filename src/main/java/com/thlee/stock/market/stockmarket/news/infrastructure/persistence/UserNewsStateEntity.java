package com.thlee.stock.market.stockmarket.news.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 사용자별 뉴스 읽음/저장 상태 JPA Entity (#115)
 *
 * <p>컬럼명을 {@code is_read} / {@code is_saved} 로 둔 이유: {@code read} 는 SQL 키워드와
 * 겹칠 소지가 있어 방언에 따라 따옴표가 필요해진다.
 */
@Entity
@Table(
        name = "user_news_state",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_news_state", columnNames = {"user_id", "news_id"})
        },
        indexes = {
                @Index(name = "idx_uns_user_read", columnList = "user_id, is_read"),
                @Index(name = "idx_uns_user_saved", columnList = "user_id, is_saved")
        }
)
@Getter
public class UserNewsStateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "news_id", nullable = false)
    private Long newsId;

    @Column(name = "is_read", nullable = false)
    private boolean read;

    @Column(name = "is_saved", nullable = false)
    private boolean saved;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected UserNewsStateEntity() {
    }

    public UserNewsStateEntity(Long id, Long userId, Long newsId,
                               boolean read, boolean saved, LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.newsId = newsId;
        this.read = read;
        this.saved = saved;
        this.updatedAt = updatedAt;
    }
}
