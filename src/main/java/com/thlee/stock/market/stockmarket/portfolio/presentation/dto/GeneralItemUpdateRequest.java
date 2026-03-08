package com.thlee.stock.market.stockmarket.portfolio.presentation.dto;

import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class GeneralItemUpdateRequest {
    private String itemName;
    private BigDecimal investedAmount;
    private String memo;
}
