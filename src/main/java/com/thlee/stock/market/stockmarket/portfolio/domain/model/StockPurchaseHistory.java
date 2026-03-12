package com.thlee.stock.market.stockmarket.portfolio.domain.model;

import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
public class StockPurchaseHistory {
    private Long id;
    private Long portfolioItemId;
    private int quantity;
    private BigDecimal purchasePrice;
    private LocalDate purchasedAt;
    private String memo;
    private LocalDateTime createdAt;

    /**
     * 재구성용 생성자 (Repository 조회 시)
     */
    public StockPurchaseHistory(Long id, Long portfolioItemId, int quantity,
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

    /**
     * 새 매수이력 생성
     */
    public static StockPurchaseHistory create(Long portfolioItemId, int quantity,
                                               BigDecimal purchasePrice, LocalDate purchasedAt,
                                               String memo) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("매수 수량은 0보다 커야 합니다.");
        }
        if (purchasePrice == null || purchasePrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("매수가는 0보다 커야 합니다.");
        }
        return new StockPurchaseHistory(null, portfolioItemId, quantity, purchasePrice,
                purchasedAt != null ? purchasedAt : LocalDate.now(), memo, LocalDateTime.now());
    }

    /**
     * 매수이력 수정
     */
    public void update(int quantity, BigDecimal purchasePrice, LocalDate purchasedAt, String memo) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("매수 수량은 0보다 커야 합니다.");
        }
        if (purchasePrice == null || purchasePrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("매수가는 0보다 커야 합니다.");
        }
        this.quantity = quantity;
        this.purchasePrice = purchasePrice;
        if (purchasedAt != null) {
            this.purchasedAt = purchasedAt;
        }
        this.memo = memo;
    }

    /**
     * 이 매수건의 총 투자금액
     */
    public BigDecimal getTotalCost() {
        return purchasePrice.multiply(BigDecimal.valueOf(quantity));
    }
}