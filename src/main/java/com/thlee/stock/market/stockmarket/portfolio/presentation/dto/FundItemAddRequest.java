package com.thlee.stock.market.stockmarket.portfolio.presentation.dto;

import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class FundItemAddRequest {
    private String itemName;
    private BigDecimal investedAmount;
    private String region;
    private String memo;
    private String subType;
    private BigDecimal managementFee;
    private BigDecimal monthlyDepositAmount;
    private Integer depositDay;
}