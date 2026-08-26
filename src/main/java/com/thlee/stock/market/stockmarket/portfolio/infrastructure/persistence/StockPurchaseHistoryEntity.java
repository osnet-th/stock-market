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

    /** 매수 시점 적용 환율 (해외 주식 전용, 기록 이전 데이터는 null) — #110 */
    @Column(name = "fx_rate", precision = 12, scale = 4)
    private BigDecimal fxRate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected StockPurchaseHistoryEntity() {
    }

    public StockPurchaseHistoryEntity(Long id, Long portfolioItemId, int quantity,
                                       BigDecimal purchasePrice, LocalDate purchasedAt,
                                       String memo, BigDecimal fxRate, LocalDateTime createdAt) {
        this.id = id;
        this.portfolioItemId = portfolioItemId;
        this.quantity = quantity;
        this.purchasePrice = purchasePrice;
        this.purchasedAt = purchasedAt;
        this.memo = memo;
        this.fxRate = fxRate;
        this.createdAt = createdAt;
    }
}