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

    /** 포인트에 등장하는 모든 카테고리 메타 (inactive 포함) — `구성` 모드 렌더용. */
    private final List<CategoryMetaResponse> categories;

    private SalaryTrendResponse(List<TrendPoint> points, List<CategoryMetaResponse> categories) {
        this.points = points;
        this.categories = categories;
    }

    public static SalaryTrendResponse of(List<TrendPoint> points, List<CategoryMetaResponse> categories) {
        return new SalaryTrendResponse(points, categories);
    }

    public static SalaryTrendResponse empty() {
        return new SalaryTrendResponse(List.of(), List.of());
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

        /** 카테고리별 유효 금액 — 추이 `구성` 모드용. 레코드가 존재하는 카테고리만 포함. */
        private final List<CategoryAmountResponse> categoryTotals;

        public TrendPoint(YearMonth yearMonth, BigDecimal income, BigDecimal totalSpending,
                          BigDecimal savingsRatio, List<CategoryAmountResponse> categoryTotals) {
            this.yearMonth = yearMonth;
            this.income = income;
            this.totalSpending = totalSpending;
            this.savingsRatio = savingsRatio;
            this.categoryTotals = categoryTotals;
        }
    }
}