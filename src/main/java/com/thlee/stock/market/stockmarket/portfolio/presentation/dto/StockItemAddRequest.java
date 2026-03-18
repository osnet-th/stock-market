package com.thlee.stock.market.stockmarket.portfolio.presentation.dto;

import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class StockItemAddRequest {
    private String itemName;
    private String region;
    private String memo;
    private String subType;
    private String stockCode;
    private String market;
    private String exchangeCode;
    private String country;
    private Integer quantity;
    private BigDecimal purchasePrice;
    private BigDecimal dividendYield;
    private String priceCurrency;
    private BigDecimal investedAmountKrw;
    private Long cashItemId;
}