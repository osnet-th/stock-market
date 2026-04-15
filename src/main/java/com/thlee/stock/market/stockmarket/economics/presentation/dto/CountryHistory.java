package com.thlee.stock.market.stockmarket.economics.presentation.dto;

import java.util.List;

/**
 * 국가별 히스토리 데이터
 */
public record CountryHistory(
    String countryName,
    List<HistoryPoint> history
) {
}