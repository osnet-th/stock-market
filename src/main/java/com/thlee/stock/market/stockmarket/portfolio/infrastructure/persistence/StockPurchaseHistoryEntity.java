package com.thlee.stock.market.stockmarket.portfolio.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "stock_purchase_history",
    indexes = {
        @Index(name = "idx_purchase_history_item_id", columnList = "portfolio_item_id")
    }
)
@Getter
public class StockPurchaseHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "portfolio_item_id", nullable = false)
    private Long portfolioItemId;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "purchase_price", nullable = false, precision = 18, scale = 2)
    private BigDecimal purchasePrice;

    @Column(name = "purchased_at", nullable = false)
    private LocalDate purchasedAt;

    @Column(name = "memo", length = 200)
    private String memo;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected StockPurchaseHistoryEntity() {
    }

    public StockPurchaseHistoryEntity(Long id, Long portfolioItemId, int quantity,
                                       BigDecimal purchasePrice, LocalDate purchasedAt,
                                       String memo, LocalDateTime createdAt) {
        this.id = id;
        this.portfolioItemId = portfolioItemId;
        this.quantity = quantity;
        this.purchasePrice = purchasePrice;
        this.purchasedAt = purchasedAt;
        this.memo = memo;
        this.createdAt = createdAt;
    }
}