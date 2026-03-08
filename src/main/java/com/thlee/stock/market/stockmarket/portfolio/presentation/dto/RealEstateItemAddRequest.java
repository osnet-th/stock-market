package com.thlee.stock.market.stockmarket.portfolio.presentation.dto;

import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class RealEstateItemAddRequest {
    private String itemName;
    private BigDecimal investedAmount;
    private String region;
    private String memo;
    private String subType;
    private String address;
    private BigDecimal area;
}