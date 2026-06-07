package com.thlee.stock.market.stockmarket.stockevaluation.domain.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 재무 조회 단위. KIS FID_DIV_CLS_CODE (0:년, 1:분기).
 */
@Getter
@RequiredArgsConstructor
public enum FinanceDivCls {
    ANNUAL("0"),
    QUARTER("1");

    private final String code;
}
