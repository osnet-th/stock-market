package com.thlee.stock.market.stockmarket.stock.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 일봉(Daily) 가격 데이터 포인트.
 *
 * <p>stocknote 종목 차트(line chart + 기록점 scatter overlay) 렌더링의 소스로 사용된다.
 * 실제 공급은 후속 작업 (KIS {@code inquire-daily-itemchartprice}) 에서 연결 예정 — 현재 포트
 * 기본 구현은 빈 리스트를 반환한다.
 */
public record DailyPrice(
        LocalDate date,
        BigDecimal open,
        BigDecimal high,
        BigDecimal low,
        BigDecimal close,
        Long volume
) { }