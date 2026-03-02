package com.thlee.stock.market.stockmarket.economics.domain.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

@Getter
@RequiredArgsConstructor
public class IndicatorValue {
    private final String rawText;
    private final BigDecimal numericValue;
    private final String unit;
}