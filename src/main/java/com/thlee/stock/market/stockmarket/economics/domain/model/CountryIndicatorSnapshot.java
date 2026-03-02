package com.thlee.stock.market.stockmarket.economics.domain.model;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class CountryIndicatorSnapshot {
    private final String countryName;
    private final GlobalEconomicIndicatorType indicatorType;
    private final IndicatorValue lastValue;
    private final IndicatorValue previousValue;
    private final String referenceText;
    private final LocalDateTime collectedAt;
}