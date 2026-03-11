package com.thlee.stock.market.stockmarket.stock.domain.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * DART 재무지표 분류 코드
 */
@Getter
@RequiredArgsConstructor
public enum IndexClassCode {

    PROFITABILITY("M210000", "수익성지표"),
    STABILITY("M220000", "안정성지표"),
    GROWTH("M230000", "성장성지표"),
    ACTIVITY("M240000", "활동성지표");

    private final String code;
    private final String label;
}