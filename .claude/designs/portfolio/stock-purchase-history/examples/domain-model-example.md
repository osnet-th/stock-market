# 도메인 모델 예시

## StockPurchaseHistory

```java
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
```

## PortfolioItem 추가 메서드

```java
// PortfolioItem.java에 추가

/**
 * 매수이력 기반 수량/평균단가/투자금 재계산
 * 이력 수정/삭제 후 호출
 */
public void recalculateFromPurchaseHistories(List<StockPurchaseHistory> histories) {
    if (this.assetType != AssetType.STOCK) {
        throw new IllegalArgumentException("주식 항목이 아닙니다.");
    }
    validateDetail(this.stockDetail, "stockDetail");
    if (histories.isEmpty()) {
        throw new IllegalArgumentException("매수 이력이 최소 1건 이상 있어야 합니다.");
    }

    int totalQuantity = 0;
    BigDecimal totalCost = BigDecimal.ZERO;
    for (StockPurchaseHistory h : histories) {
        totalQuantity += h.getQuantity();
        totalCost = totalCost.add(h.getTotalCost());
    }

    BigDecimal newAvgBuyPrice = totalCost.divide(BigDecimal.valueOf(totalQuantity), 2, RoundingMode.HALF_UP);

    this.stockDetail = new StockDetail(
            this.stockDetail.getSubType(),
            this.stockDetail.getStockCode(),
            this.stockDetail.getMarket(),
            this.stockDetail.getExchangeCode(),
            this.stockDetail.getCountry(),
            totalQuantity,
            newAvgBuyPrice,
            this.stockDetail.getDividendYield()
    );
    this.investedAmount = calcInvestedAmount(newAvgBuyPrice, totalQuantity);
    this.updatedAt = LocalDateTime.now();
}
```