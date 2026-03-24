package com.thlee.stock.market.stockmarket.economics.presentation.dto;

import com.thlee.stock.market.stockmarket.economics.domain.model.EcosIndicatorMetadata;
import com.thlee.stock.market.stockmarket.economics.domain.model.KeyStatIndicator;

/**
 * 경제지표 단건 응답 DTO
 */
public record IndicatorResponse(
    String className,
    String keystatName,
    String dataValue,
    String previousDataValue,
    String cycle,
    String unitName,
    String description
) {

    public static IndicatorResponse from(KeyStatIndicator indicator,
                                          EcosIndicatorMetadata meta) {
        return new IndicatorResponse(
            indicator.className(),
            indicator.keystatName(),
            indicator.dataValue(),
            indicator.previousDataValue(),
            indicator.cycle(),
            indicator.unitName(),
            meta != null ? meta.getDescription() : null
        );
    }
}