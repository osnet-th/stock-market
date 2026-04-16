package com.thlee.stock.market.stockmarket.salary.application.dto;

import lombok.Getter;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

/**
 * 최근 N개월 추이 (월급 / 총지출 / 저축율 3개 라인 구성용).
 * 기록이 시작된 월 이전은 포함하지 않는다(X축 빈 월 금지).
 */
@Getter
public class SalaryTrendResponse {

    private final List<TrendPoint> points;

    private SalaryTrendResponse(List<TrendPoint> points) {
        this.points = points;
    }

    public static SalaryTrendResponse of(List<TrendPoint> points) {
        return new SalaryTrendResponse(points);
    }

    public static SalaryTrendResponse empty() {
        return new SalaryTrendResponse(List.of());
    }

    @Getter
    public static class TrendPoint {
        private final YearMonth yearMonth;

        /** 월급 상속값 (없으면 null). */
        private final BigDecimal income;

        /** 카테고리 합계 상속값. */
        private final BigDecimal totalSpending;

        /** 저축율 scale=4. income이 null이거나 0이면 null. */
        private final BigDecimal savingsRatio;

        public TrendPoint(YearMonth yearMonth, BigDecimal income, BigDecimal totalSpending,
                          BigDecimal savingsRatio) {
            this.yearMonth = yearMonth;
            this.income = income;
            this.totalSpending = totalSpending;
            this.savingsRatio = savingsRatio;
        }
    }
}