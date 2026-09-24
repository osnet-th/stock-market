package com.thlee.stock.market.stockmarket.salary.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 하위 항목 스냅샷 세트 헤더 Entity. Effective Date 모델.
 *
 * <p>항목 행({@link SpendingItemEntity})은 {@code set_id}로 ID 참조한다.
 */
@Entity
@Table(
        name = "spending_item_set",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_spending_item_set_user_month",
                        columnNames = {"user_id", "effective_from_month"}
                )
        }
)
@Getter
public class SpendingItemSetEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 매월 1일로 정규화 (도메인 팩토리에서 강제) */
    @Column(name = "effective_from_month", nullable = false)
    private LocalDate effectiveFromMonth;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected SpendingItemSetEntity() {
    }

    public SpendingItemSetEntity(Long id, Long userId, LocalDate effectiveFromMonth,
                                 LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.effectiveFromMonth = effectiveFromMonth;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
