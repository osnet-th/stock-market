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
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 카테고리별 지출 변경 이력 Entity. Effective Date 모델.
 */
@Entity
@Table(
        name = "spending_config",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_spending_config_user_category_month",
                        columnNames = {"user_id", "category", "effective_from_month"}
                )
        },
        indexes = {
                // DISTINCT ON (category) ORDER BY category, effective_from_month DESC 최적화
                @Index(
                        name = "idx_spending_config_user_category_month_desc",
                        columnList = "user_id, category, effective_from_month DESC"
                )
        }
)
@Getter
public class SpendingConfigEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 20)
    private SpendingCategory category;

    /** 매월 1일로 정규화 (도메인 팩토리에서 강제) */
    @Column(name = "effective_from_month", nullable = false)
    private LocalDate effectiveFromMonth;

    /** KRW whole-number: precision=15, scale=0 */
    @Column(name = "amount", nullable = false, precision = 15, scale = 0)
    private BigDecimal amount;

    @Column(name = "memo", length = 200)
    private String memo;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected SpendingConfigEntity() {
    }

    public SpendingConfigEntity(Long id, Long userId, SpendingCategory category,
                                LocalDate effectiveFromMonth, BigDecimal amount, String memo,
                                LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.category = category;
        this.effectiveFromMonth = effectiveFromMonth;
        this.amount = amount;
        this.memo = memo;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}