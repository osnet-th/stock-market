package com.thlee.stock.market.stockmarket.portfolio.presentation.dto;

import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class StockPurchaseRequest {
    private Integer quantity;
    private BigDecimal purchasePrice;
    private BigDecimal investedAmountKrw;
}