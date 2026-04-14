package com.thlee.stock.market.stockmarket.portfolio.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "deposit_history",
    indexes = {
        @Index(name = "idx_deposit_history_item_id", columnList = "portfolio_item_id")
    }
)
@Getter
public class DepositHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "portfolio_item_id", nullable = false)
    private Long portfolioItemId;

    @Column(name = "deposit_date", nullable = false)
    private LocalDate depositDate;

    @Column(name = "amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(name = "units", precision = 18, scale = 6)
    private BigDecimal units;

    @Column(name = "memo", length = 200)
    private String memo;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected DepositHistoryEntity() {
    }

    public DepositHistoryEntity(Long id, Long portfolioItemId, LocalDate depositDate,
                                 BigDecimal amount, BigDecimal units,
                                 String memo, LocalDateTime createdAt) {
        this.id = id;
        this.portfolioItemId = portfolioItemId;
        this.depositDate = depositDate;
        this.amount = amount;
        this.units = units;
        this.memo = memo;
        this.createdAt = createdAt;
    }
}