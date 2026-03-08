package com.thlee.stock.market.stockmarket.portfolio.presentation.dto;

import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class FundItemUpdateRequest {
    private String itemName;
    private BigDecimal investedAmount;
    private String memo;
    private String subType;
    private BigDecimal managementFee;
}
