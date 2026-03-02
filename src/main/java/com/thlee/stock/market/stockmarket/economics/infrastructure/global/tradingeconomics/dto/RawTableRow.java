package com.thlee.stock.market.stockmarket.economics.infrastructure.global.tradingeconomics.dto;

/**
 * 파싱된 raw 테이블 행
 * 모든 값은 원문 문자열 그대로 보관
 */
public record RawTableRow(
    String country,
    String last,
    String previous,
    String reference,
    String unit
) {}