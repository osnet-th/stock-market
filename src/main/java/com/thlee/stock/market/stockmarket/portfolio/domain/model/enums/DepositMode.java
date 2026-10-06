package com.thlee.stock.market.stockmarket.portfolio.domain.model.enums;

/**
 * 자동납입 처리 방식.
 * NOTIFY 는 납입일 알림을 보고 사용자가 직접 기록하고, AUTO 는 납입일에 시스템이 기록한다.
 */
public enum DepositMode {
    NOTIFY,
    AUTO;

    /**
     * 요청 값을 처리 방식으로 바꾼다. 비어 있으면 알림 확인(NOTIFY)이다.
     */
    public static DepositMode from(String value) {
        if (value == null || value.isBlank()) {
            return NOTIFY;
        }
        try {
            return valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("납입 처리 방식이 올바르지 않습니다: " + value);
        }
    }
}
