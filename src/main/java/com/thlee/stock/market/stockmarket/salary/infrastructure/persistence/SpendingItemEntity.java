package com.thlee.stock.market.stockmarket.salary.infrastructure.persistence;

import com.thlee.stock.market.stockmarket.salary.domain.model.enums.SpendingCategory;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 카테고리 하위 지출 항목 Entity. {@code set_id}로 세트 헤더를 ID 참조한다.
 *
 * <p>세트 재저장 시 항목 행은 전체 삭제 후 재삽입된다 (스냅샷 교체).
 */
@Entity
@Table(
        name = "spending_item",
        indexes = {
                @Index(name = "idx_spending_item_set", columnList = "set_id")
        }
)
@Getter
public class SpendingItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "set_id", nullable = false)
    private Long setId;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 20)
    private SpendingCategory category;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /** KRW whole-number: precision=15, scale=0 */
    @Column(name = "amount", nullable = false, precision = 15, scale = 0)
    private BigDecimal amount;

    @Column(name = "is_fixed", nullable = false)
    private boolean fixed;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected SpendingItemEntity() {
    }

    public SpendingItemEntity(Long id, Long setId, SpendingCategory category, String name,
                              BigDecimal amount, boolean fixed, int sortOrder, LocalDateTime createdAt) {
        this.id = id;
        this.setId = setId;
        this.category = category;
        this.name = name;
        this.amount = amount;
        this.fixed = fixed;
        this.sortOrder = sortOrder;
        this.createdAt = createdAt;
    }
}
