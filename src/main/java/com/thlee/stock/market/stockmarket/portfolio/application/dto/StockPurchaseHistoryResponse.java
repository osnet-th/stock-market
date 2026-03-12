package com.thlee.stock.market.stockmarket.portfolio.application.dto;

import com.thlee.stock.market.stockmarket.portfolio.domain.model.StockPurchaseHistory;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
public class StockPurchaseHistoryResponse {
    private final Long id;
    private final Long portfolioItemId;
    private final int quantity;
    private final BigDecimal purchasePrice;
    private final BigDecimal totalCost;
    private final LocalDate purchasedAt;
    private final String memo;
    private final LocalDateTime createdAt;

    private StockPurchaseHistoryResponse(Long id, Long portfolioItemId, int quantity,
                                          BigDecimal purchasePrice, BigDecimal totalCost,
                                          LocalDate purchasedAt, String memo, LocalDateTime createdAt) {
        this.id = id;
        this.portfolioItemId = portfolioItemId;
        this.quantity = quantity;
        this.purchasePrice = purchasePrice;
        this.totalCost = totalCost;
        this.purchasedAt = purchasedAt;
        this.memo = memo;
        this.createdAt = createdAt;
    }

    public static StockPurchaseHistoryResponse from(StockPurchaseHistory history) {
        return new StockPurchaseHistoryResponse(
                history.getId(),
                history.getPortfolioItemId(),
                history.getQuantity(),
                history.getPurchasePrice(),
                history.getTotalCost(),
                history.getPurchasedAt(),
                history.getMemo(),
                history.getCreatedAt()
        );
    }
}