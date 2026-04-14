package com.thlee.stock.market.stockmarket.portfolio.presentation.dto;

import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
public class CashItemUpdateRequest {
    private String itemName;
    private BigDecimal investedAmount;
    private String memo;
    private BigDecimal interestRate;
    private LocalDate startDate;
    private LocalDate maturityDate;
    private String taxType;
    private BigDecimal monthlyDepositAmount;
    private Integer depositDay;
}