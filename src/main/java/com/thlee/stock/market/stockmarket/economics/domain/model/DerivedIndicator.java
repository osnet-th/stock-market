package com.thlee.stock.market.stockmarket.economics.domain.model;

/**
 * 파생지표 (원시 지표들의 조합으로 계산된 분석 지표)
 */
public record DerivedIndicator(
        String name,
        Double value,
        String unit,
        String formula,
        String description
) {}