package com.thlee.stock.market.stockmarket.portfolio.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "allocation_target",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_allocation_target_user_id", columnNames = "user_id")
        }
)
@Getter
public class AllocationTargetEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "safe_ratio", nullable = false, precision = 5, scale = 2)
    private BigDecimal safeRatio;

    @Column(name = "invest_ratio", nullable = false, precision = 5, scale = 2)
    private BigDecimal investRatio;

    @Column(name = "band_pct_point", nullable = false, precision = 4, scale = 2)
    private BigDecimal bandPctPoint;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected AllocationTargetEntity() {
    }

    public AllocationTargetEntity(Long id,
                                  Long userId,
                                  BigDecimal safeRatio,
                                  BigDecimal investRatio,
                                  BigDecimal bandPctPoint,
                                  LocalDateTime createdAt,
                                  LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.safeRatio = safeRatio;
        this.investRatio = investRatio;
        this.bandPctPoint = bandPctPoint;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
