package com.thlee.stock.market.stockmarket.economics.presentation.dto;

/**
 * 글로벌 지표 카테고리 내 개별 지표 요약 응답 DTO
 */
public record GlobalIndicatorSummaryResponse(
    String key,
    String displayName
) {
}