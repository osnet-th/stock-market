package com.thlee.stock.market.stockmarket.stock.domain.model;

/**
 * 상장 종목 정보 (API 출처에 무관한 순수 도메인 모델)
 */
public record Stock(
    String stockCode,
    String stockName,
    String marketType,
    String corpName
) {
}