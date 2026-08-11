package com.thlee.stock.market.stockmarket.portfolio.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "portfolio_snapshot",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_portfolio_snapshot_user_date",
                        columnNames = {"user_id", "snapshot_date"})
        },
        indexes = {
                @Index(name = "idx_portfolio_snapshot_user_id", columnList = "user_id")
        }
)
@Getter
public class PortfolioSnapshotEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;

    @Column(name = "total_evaluated", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalEvaluated;

    @Column(name = "total_invested", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalInvested;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected PortfolioSnapshotEntity() {
    }

    public PortfolioSnapshotEntity(Long id, Long userId, LocalDate snapshotDate,
                                   BigDecimal totalEvaluated, BigDecimal totalInvested,
                                   LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.snapshotDate = snapshotDate;
        this.totalEvaluated = totalEvaluated;
        this.totalInvested = totalInvested;
        this.createdAt = createdAt;
    }
}
