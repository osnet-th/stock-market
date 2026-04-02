package com.thlee.stock.market.stockmarket.economics.presentation.dto;

/**
 * 히스토리 데이터 포인트 (cycle별 값)
 */
public record HistoryPoint(
    String cycle,
    String dataValue
) {
}