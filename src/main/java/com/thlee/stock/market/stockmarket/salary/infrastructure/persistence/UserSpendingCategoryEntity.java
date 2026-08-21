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

import java.time.LocalDateTime;

/**
 * 사용자 정의 지출 카테고리 Entity.
 *
 * <p>code는 기존 enum 저장 행('FOOD' 등)과 그대로 매칭되도록 varchar(20)을 유지한다.
 */
@Entity
@Table(
        name = "user_spending_category",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_user_spending_category_user_code",
                        columnNames = {"user_id", "code"}
                )
        },
        indexes = {
                @Index(name = "idx_user_spending_category_user", columnList = "user_id")
        }
)
@Getter
public class UserSpendingCategoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "code", nullable = false, length = 20)
    private String code;

    @Column(name = "name", nullable = false, length = 40)
    private String name;

    @Column(name = "color", nullable = false, length = 9)
    private String color;

    @Column(name = "is_savings", nullable = false)
    private boolean savings;

    @Column(name = "is_system", nullable = false)
    private boolean system;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected UserSpendingCategoryEntity() {
    }

    public UserSpendingCategoryEntity(Long id, Long userId, String code, String name, String color,
                                      boolean savings, boolean system, boolean active, int sortOrder,
                                      LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.code = code;
        this.name = name;
        this.color = color;
        this.savings = savings;
        this.system = system;
        this.active = active;
        this.sortOrder = sortOrder;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
