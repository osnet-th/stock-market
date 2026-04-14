package com.thlee.stock.market.stockmarket.portfolio.presentation.dto;

import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
public class CashItemAddRequest {
    private String itemName;
    private BigDecimal investedAmount;
    private String region;
    private String memo;
    private String cashType;
    private BigDecimal interestRate;
    private LocalDate startDate;
    private LocalDate maturityDate;
    private String taxType;
    private BigDecimal monthlyDepositAmount;
    private Integer depositDay;
}