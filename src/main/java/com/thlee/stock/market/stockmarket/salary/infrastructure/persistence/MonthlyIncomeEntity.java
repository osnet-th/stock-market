package com.thlee.stock.market.stockmarket.salary.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * 월급 변경 이력 Entity. Effective Date 모델.
 */
@Entity
@Table(
        name = "monthly_income",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_monthly_income_user_month",
                        columnNames = {"user_id", "effective_from_month"}
                )
        },
        indexes = {
                @Index(
                        name = "idx_monthly_income_user_month",
                        columnList = "user_id, effective_from_month"
                )
        }
)
@Getter
public class MonthlyIncomeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 매월 1일로 정규화 (도메인 팩토리에서 강제) */
    @Column(name = "effective_from_month", nullable = false)
    private LocalDate effectiveFromMonth;

    /** KRW whole-number: precision=15, scale=0 */
    @Column(name = "amount", nullable = false, precision = 15, scale = 0)
    private BigDecimal amount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** upsert 추적 목적 (portfolio 도메인엔 없으나 본 도메인은 변경 이력 갱신 빈도가 있어 별도 컬럼 유지) */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected MonthlyIncomeEntity() {
    }

    public MonthlyIncomeEntity(Long id, Long userId, LocalDate effectiveFromMonth,
                               BigDecimal amount, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.effectiveFromMonth = effectiveFromMonth;
        this.amount = amount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}