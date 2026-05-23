package com.thlee.stock.market.stockmarket.realestate.domain.service;

import java.time.LocalDate;

/**
 * 외부 출처 호출 시 조회 기간 표현 VO.
 * <p>
 * 일배치 호출 시 from~to 기간을 명시한다. 출처별로 일/월 단위 해석이 다르므로
 * 어댑터 내부에서 해당 출처 포맷으로 변환한다.
 */
public record FetchWindow(LocalDate from, LocalDate to) {

    public FetchWindow {
        if (from == null || to == null) {
            throw new IllegalArgumentException("FetchWindow from/to must not be null");
        }
        if (to.isBefore(from)) {
            throw new IllegalArgumentException("FetchWindow to must not be before from");
        }
    }

    public static FetchWindow lastNDays(LocalDate today, int days) {
        return new FetchWindow(today.minusDays(days), today);
    }

    public static FetchWindow lastNMonths(LocalDate today, int months) {
        return new FetchWindow(today.minusMonths(months), today);
    }
}
