package com.thlee.stock.market.stockmarket.economics.presentation.dto;

import com.thlee.stock.market.stockmarket.economics.domain.model.KeyStatIndicator;

/**
 * 경제지표 단건 응답 DTO
 */
public record IndicatorResponse(
    String className,
    String keystatName,
    String dataValue,
    String cycle,
    String unitName
) {

    public static IndicatorResponse from(KeyStatIndicator indicator) {
        return new IndicatorResponse(
            indicator.className(),
            indicator.keystatName(),
            indicator.dataValue(),
            indicator.cycle(),
            indicator.unitName()
        );
    }
}
