package com.thlee.stock.market.stockmarket.portfolio.application.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

@Getter
@RequiredArgsConstructor
public class AllocationResponse {
    private final String assetType;
    private final String assetTypeName;
    private final BigDecimal totalAmount;
    private final BigDecimal percentage;
}