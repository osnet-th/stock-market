package com.thlee.stock.market.stockmarket.stock.domain.model;

/**
 * SEC 투자 지표 (EPS, ROE, 부채비율, 영업이익률 등)
 * @param name 지표명
 * @param value 지표값 (null 가능)
 * @param unit 단위 ("$", "%", "x")
 * @param description 지표 설명
 */
public record SecInvestmentMetric(
        String name,
        Double value,
        String unit,
        String description
) {}