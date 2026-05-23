package com.thlee.stock.market.stockmarket.realestate.presentation.dto;

/**
 * 조회 기간 옵션. 일배치 데이터를 N일 단위로 조회한다.
 */
public enum PeriodOption {

    ONE_MONTH(30),
    THREE_MONTHS(90),
    SIX_MONTHS(180),
    ONE_YEAR(365);

    private final int days;

    PeriodOption(int days) {
        this.days = days;
    }

    public int days() {
        return days;
    }
}