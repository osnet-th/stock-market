package com.thlee.stock.market.stockmarket.economics.presentation.dto;

import java.util.List;

/**
 * 지표별 히스토리 응답 DTO
 */
public record IndicatorHistoryResponse(
    String keystatName,
    String className,
    String unitName,
    List<HistoryPoint> history
) {
}