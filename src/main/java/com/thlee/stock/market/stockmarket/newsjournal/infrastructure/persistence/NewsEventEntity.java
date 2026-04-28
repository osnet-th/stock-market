package com.thlee.stock.market.stockmarket.newsjournal.infrastructure.persistence;

import com.thlee.stock.market.stockmarket.newsjournal.domain.model.EventCategory;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 뉴스 저널 사건 본체 Entity.
 *
 * <p>자식 링크({@link NewsEventLinkEntity})와 연관관계 없이 ID 참조만 사용한다 (CLAUDE.md 규칙).
 */
@Entity
@Table(
        name = "news_event",
        indexes = {
                @Index(name = "idx_news_event_user_date",
                        columnList = "user_id, occurred_date DESC"),
                @Index(name = "idx_news_event_user_category",
                        columnList = "user_id, category, occurred_date DESC")
        }
)
@Getter
public class NewsEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "occurred_date", nullable = false)
    private LocalDate occurredDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 20)
    private EventCategory category;

    @Column(name = "what", columnDefinition = "TEXT")
    private String what;

    @Column(name = "why", columnDefinition = "TEXT")
    private String why;

    @Column(name = "how", columnDefinition = "TEXT")
    private String how;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected NewsEventEntity() {
    }

    public NewsEventEntity(Long id, Long userId, String title, LocalDate occurredDate, EventCategory category,
                           String what, String why, String how,
                           LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.occurredDate = occurredDate;
        this.category = category;
        this.what = what;
        this.why = why;
        this.how = how;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}