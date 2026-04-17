package com.thlee.stock.market.stockmarket.stock.application.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Getter
@RequiredArgsConstructor
public class ValuationMetricResponse {
    private final String termName;
    private final BigDecimal eps;
    private final BigDecimal bps;
    private final BigDecimal per;
    private final BigDecimal pbr;
    private final String referencePriceDate;
    private final BigDecimal referencePrice;
    private final List<String> warnings;
}