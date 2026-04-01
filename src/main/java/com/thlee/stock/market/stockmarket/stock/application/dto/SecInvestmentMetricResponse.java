package com.thlee.stock.market.stockmarket.stock.application.dto;

import com.thlee.stock.market.stockmarket.stock.domain.model.SecInvestmentMetric;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class SecInvestmentMetricResponse {
    private final String name;
    private final Double value;
    private final String unit;
    private final String description;

    public static SecInvestmentMetricResponse from(SecInvestmentMetric metric) {
        return new SecInvestmentMetricResponse(
                metric.name(),
                metric.value(),
                metric.unit(),
                metric.description()
        );
    }
}