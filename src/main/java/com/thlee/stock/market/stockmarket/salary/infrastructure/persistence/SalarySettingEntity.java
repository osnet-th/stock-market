package com.thlee.stock.market.stockmarket.salary.infrastructure.persistence;

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
 * 월급 화면 사용자 설정 Entity (사용자당 1행).
 */
@Entity
@Table(
        name = "user_salary_setting",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_salary_setting_user", columnNames = {"user_id"})
        }
)
@Getter
public class SalarySettingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "saving_target_pct", nullable = false)
    private int savingTargetPct;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected SalarySettingEntity() {
    }

    public SalarySettingEntity(Long id, Long userId, int savingTargetPct,
                               LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.savingTargetPct = savingTargetPct;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
