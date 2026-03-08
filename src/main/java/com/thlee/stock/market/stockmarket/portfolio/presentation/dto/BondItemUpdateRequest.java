package com.thlee.stock.market.stockmarket.portfolio.presentation.dto;

import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
public class BondItemUpdateRequest {
    private String itemName;
    private BigDecimal investedAmount;
    private String memo;
    private String subType;
    private LocalDate maturityDate;
    private BigDecimal couponRate;
    private String creditRating;
}
