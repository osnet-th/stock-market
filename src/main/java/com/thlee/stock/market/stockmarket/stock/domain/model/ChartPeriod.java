package com.thlee.stock.market.stockmarket.stock.domain.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * 기간별시세 봉 주기.
 *
 * <p>KIS inquire-daily-itemchartprice(FHKST03010100)의 {@code FID_PERIOD_DIV_CODE} 값과
 * 페이징 창 크기(캘린더일)를 보유한다. KIS는 호출당 최대 100건(종료일 기준 최신)을 반환하므로,
 * 창 크기는 "약 100봉"에 해당하는 캘린더일로 잡아 슬라이딩 페이징 횟수를 최소화한다.
 */
@Getter
@RequiredArgsConstructor
public enum ChartPeriod {

    /** 일봉 — 100영업일 ≈ 140캘린더일 */
    DAILY("D", 140),

    /** 주봉 — 100주 ≈ 690캘린더일 */
    WEEKLY("W", 690),

    /** 월봉 — 100개월 ≈ 3000캘린더일 (40년치 ≈ 5회 호출) */
    MONTHLY("M", 3000);

    /** KIS FID_PERIOD_DIV_CODE 값. */
    private final String code;

    /** 페이징 슬라이딩 창 크기(캘린더일). */
    private final int chunkCalendarDays;

    public static ChartPeriod fromCode(String code) {
        return Arrays.stream(values())
            .filter(p -> p.code.equalsIgnoreCase(code))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 봉 주기: " + code));
    }
}
