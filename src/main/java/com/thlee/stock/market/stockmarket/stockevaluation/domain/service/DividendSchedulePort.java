package com.thlee.stock.market.stockmarket.stockevaluation.domain.service;

import com.thlee.stock.market.stockmarket.stockevaluation.domain.model.DividendSchedule;

import java.time.LocalDate;
import java.util.List;

/**
 * 종목별 현금배당 일정 조회 포트.
 * 외부 요청과 응답 해석은 infrastructure 어댑터가 담당하고, 호출자는 이 계약에만 의존한다.
 */
public interface DividendSchedulePort {

    /**
     * 기간 안의 현금배당 일정을 조회한다.
     *
     * <p>빈 목록은 그 기간에 현금배당 일정이 없다는 뜻이다. 조회 자체가 실패하면 예외를 던진다.
     * 일정 없음과 조회 실패를 구분해야 호출자가 실패 시의 동작을 스스로 정할 수 있다.</p>
     *
     * <p>기간이 기준일과 지급일 중 무엇으로 걸리는지는 출처에 따라 다르다. 지급일 기준 집계가
     * 필요하면 넉넉한 기간으로 조회한 뒤 {@link DividendSchedule#isPaidBetween}으로 다시 거른다.</p>
     *
     * @param stockCode 종목코드 (KRX 6자리)
     * @param from      조회 시작일
     * @param to        조회 종료일
     */
    List<DividendSchedule> findCashDividends(String stockCode, LocalDate from, LocalDate to);
}
