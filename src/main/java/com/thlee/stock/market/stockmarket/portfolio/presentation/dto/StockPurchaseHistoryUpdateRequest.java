package com.thlee.stock.market.stockmarket.portfolio.presentation.dto;

import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
public class StockPurchaseHistoryUpdateRequest {
    private Integer quantity;
    private BigDecimal purchasePrice;
    private LocalDate purchasedAt;
    private String memo;
}