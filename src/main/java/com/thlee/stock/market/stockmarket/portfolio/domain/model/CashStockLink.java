package com.thlee.stock.market.stockmarket.portfolio.domain.model;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class CashStockLink {
    private Long id;
    private Long cashItemId;
    private Long stockItemId;
    private LocalDateTime createdAt;

    /**
     * 재구성용 생성자 (Repository 조회 시)
     */
    public CashStockLink(Long id, Long cashItemId, Long stockItemId, LocalDateTime createdAt) {
        this.id = id;
        this.cashItemId = cashItemId;
        this.stockItemId = stockItemId;
        this.createdAt = createdAt;
    }

    /**
     * 새 연결 생성
     */
    public static CashStockLink create(Long cashItemId, Long stockItemId) {
        if (cashItemId == null) {
            throw new IllegalArgumentException("cashItemId는 필수입니다.");
        }
        if (stockItemId == null) {
            throw new IllegalArgumentException("stockItemId는 필수입니다.");
        }
        return new CashStockLink(null, cashItemId, stockItemId, LocalDateTime.now());
    }
}