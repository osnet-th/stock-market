package com.thlee.stock.market.stockmarket.salary.infrastructure.persistence.mapper;

import java.time.LocalDate;
import java.time.YearMonth;

/**
 * {@link YearMonth} ↔ {@link LocalDate}(매월 1일) 변환 유틸.
 * Entity↔Domain 변환의 단일 지점으로, Mapper 클래스들이 공통 사용한다.
 */
public class YearMonthConverter {

    private YearMonthConverter() {
    }

    public static LocalDate toLocalDate(YearMonth ym) {
        return ym == null ? null : ym.atDay(1);
    }

    public static YearMonth toYearMonth(LocalDate date) {
        return date == null ? null : YearMonth.from(date);
    }
}