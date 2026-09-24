package com.thlee.stock.market.stockmarket.stock.domain.service;

import java.time.LocalDate;

/**
 * 국내 증시 개장일 판별 포트.
 */
public interface MarketCalendarPort {

    /**
     * 해당 일자에 국내 증시가 열리는지 판별한다.
     *
     * <p>판별 자체가 불가능한 경우(응답 없음, 대상 일자 누락 등)에는 예외를 던진다.
     * 휴장(false)과 판별 실패를 구분해야, 호출자가 조회 실패 시의 동작을 스스로 정할 수 있다.</p>
     *
     * @param date 판별 대상 일자
     * @return 개장일이면 true, 휴장일이면 false
     */
    boolean isOpen(LocalDate date);
}
