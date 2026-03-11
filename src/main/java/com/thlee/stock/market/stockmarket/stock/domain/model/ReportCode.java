package com.thlee.stock.market.stockmarket.stock.domain.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * DART 보고서 구분 코드
 */
@Getter
@RequiredArgsConstructor
public enum ReportCode {

    ANNUAL("11011", "사업보고서"),
    SEMI_ANNUAL("11012", "반기보고서"),
    Q1("11013", "1분기보고서"),
    Q3("11014", "3분기보고서");

    private final String code;
    private final String label;
}