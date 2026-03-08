package com.thlee.stock.market.stockmarket.portfolio.presentation.dto;

import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class GeneralItemAddRequest {
    private String assetType;
    private String itemName;
    private BigDecimal investedAmount;
    private String region;
    private String memo;
}