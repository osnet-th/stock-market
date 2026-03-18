package com.thlee.stock.market.stockmarket.portfolio.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "cash_stock_link",
    uniqueConstraints = @UniqueConstraint(name = "uk_cash_stock_link_stock", columnNames = "stock_item_id"),
    indexes = @Index(name = "idx_cash_stock_link_cash", columnList = "cash_item_id")
)
@Getter
public class CashStockLinkEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cash_item_id", nullable = false)
    private Long cashItemId;

    @Column(name = "stock_item_id", nullable = false)
    private Long stockItemId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected CashStockLinkEntity() {
    }

    public CashStockLinkEntity(Long id, Long cashItemId, Long stockItemId, LocalDateTime createdAt) {
        this.id = id;
        this.cashItemId = cashItemId;
        this.stockItemId = stockItemId;
        this.createdAt = createdAt;
    }
}