package com.thlee.stock.market.stockmarket.newsjournal.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 뉴스 저널 사건 분류 Entity.
 *
 * <p>{@code (user_id, name)} unique 로 자동 등록 시 중복을 방지한다 (race 시
 * application 계층의 {@code DataIntegrityViolationException} 1회 retry 패턴과 결합).
 */
@Entity
@Table(
        name = "news_event_category",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_news_event_category_user_name",
                        columnNames = {"user_id", "name"})
        }
)
@Getter
public class NewsEventCategoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected NewsEventCategoryEntity() {
    }

    public NewsEventCategoryEntity(Long id, Long userId, String name, LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.createdAt = createdAt;
    }
}