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
    /** 매수 시점 적용 환율. 해외 주식만 값이 있고, 기록 이전 데이터는 null 이라 환차손익 산출에서 제외한다 (#110) */
    private BigDecimal fxRate;
    private LocalDateTime createdAt;

    /**
     * 재구성용 생성자 (Repository 조회 시)
     */
    public StockPurchaseHistory(Long id, Long portfolioItemId, int quantity,
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

    /**
     * 새 매수이력 생성
     */
    public static StockPurchaseHistory create(Long portfolioItemId, int quantity,
                                               BigDecimal purchasePrice, LocalDate purchasedAt,
                                               String memo, BigDecimal fxRate) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("매수 수량은 0보다 커야 합니다.");
        }
        if (purchasePrice == null || purchasePrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("매수가는 0보다 커야 합니다.");
        }
        if (fxRate != null && fxRate.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("적용 환율은 0보다 커야 합니다.");
        }
        return new StockPurchaseHistory(null, portfolioItemId, quantity, purchasePrice,
                purchasedAt != null ? purchasedAt : LocalDate.now(), memo, fxRate, LocalDateTime.now());
    }

    /**
     * 매수이력 수정
     */
    public void update(int quantity, BigDecimal purchasePrice, LocalDate purchasedAt,
                       String memo, BigDecimal fxRate) {
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
        // null 을 넘기면 기록을 지운다 (잘못 입력한 환율을 되돌릴 수 있어야 한다 — #110 review L2)
        if (fxRate != null && fxRate.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("적용 환율은 0보다 커야 합니다.");
        }
        this.fxRate = fxRate;
    }

    /**
     * 이 매수건의 총 투자금액
     */
    public BigDecimal getTotalCost() {
        return purchasePrice.multiply(BigDecimal.valueOf(quantity));
    }
}