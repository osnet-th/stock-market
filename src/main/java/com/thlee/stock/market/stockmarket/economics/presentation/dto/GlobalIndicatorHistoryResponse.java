package com.thlee.stock.market.stockmarket.economics.presentation.dto;

import java.util.List;

/**
 * 글로벌 경제지표 히스토리 응답 (지표타입별)
 */
public record GlobalIndicatorHistoryResponse(
    String indicatorType,
    String displayName,
    String unit,
    List<CountryHistory> countries
) {
}