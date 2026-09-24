package com.thlee.stock.market.stockmarket.salary.application.dto;

import com.thlee.stock.market.stockmarket.salary.domain.model.MonthlyIncome;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

/**
 * 특정 월의 월급 사용 현황 (상속값 적용).
 */
@Getter
public class MonthlySalaryResponse {

    private final YearMonth yearMonth;

    /** 월급 금액 (없으면 null). */
    private final BigDecimal income;

    /** 월급이 상속값이면 출처 월, 해당 월 직접 입력이면 null. */
    private final YearMonth incomeInheritedFromMonth;

    /** 표시 대상 카테고리 라인 (active ∪ 해당 월 금액 보유 inactive, sort 순). */
    private final List<SpendingLineResponse> spendings;

    private final BigDecimal totalSpending;

    /** {@code income - totalSpending}. income이 null이면 null. 음수 허용(초과 지출). */
    private final BigDecimal remaining;

    /** 저축율: savings 카테고리 합 / income, scale=4. income이 0이거나 null이면 null. */
    private final BigDecimal savingsRatio;

    /** 온보딩 화면 판정용. 월급 또는 지출이 하나라도 있으면 true. */
    private final boolean hasAnyData;

    /** 하위 항목 세트가 상속값이면 출처 월, 해당 월 직접 저장이면 null. */
    private final YearMonth itemsInheritedFromMonth;

    /** 전월 유효값 요약 (전월 기록이 전혀 없으면 null). */
    private final PreviousMonthResponse previous;

    /** 저축률 목표(%) — 사용자 설정, 미설정 시 기본 50. */
    private final int savingTarget;

    private MonthlySalaryResponse(YearMonth yearMonth, BigDecimal income, YearMonth incomeInheritedFromMonth,
                                  List<SpendingLineResponse> spendings, BigDecimal totalSpending,
                                  BigDecimal remaining, BigDecimal savingsRatio, boolean hasAnyData,
                                  YearMonth itemsInheritedFromMonth, PreviousMonthResponse previous,
                                  int savingTarget) {
        this.yearMonth = yearMonth;
        this.income = income;
        this.incomeInheritedFromMonth = incomeInheritedFromMonth;
        this.spendings = spendings;
        this.totalSpending = totalSpending;
        this.remaining = remaining;
        this.savingsRatio = savingsRatio;
        this.hasAnyData = hasAnyData;
        this.itemsInheritedFromMonth = itemsInheritedFromMonth;
        this.previous = previous;
        this.savingTarget = savingTarget;
    }

    public static MonthlySalaryResponse from(YearMonth yearMonth,
                                             MonthlyIncome income,
                                             List<SpendingLineResponse> spendings,
                                             YearMonth itemsInheritedFromMonth,
                                             PreviousMonthResponse previous,
                                             int savingTarget) {
        BigDecimal totalSpending = sumAmounts(spendings);
        BigDecimal incomeAmount = income != null ? income.getAmount() : null;
        YearMonth incomeInherited = inheritedFrom(income, yearMonth);
        BigDecimal remaining = incomeAmount != null ? incomeAmount.subtract(totalSpending) : null;
        BigDecimal savingsRatio = income != null ? income.calculateSavingsRatio(savingsAmount(spendings)) : null;
        boolean hasAnyData = income != null
                || spendings.stream().anyMatch(s -> s.getAmount().signum() > 0);

        return new MonthlySalaryResponse(yearMonth, incomeAmount, incomeInherited, spendings,
                totalSpending, remaining, savingsRatio, hasAnyData, itemsInheritedFromMonth,
                previous, savingTarget);
    }

    private static BigDecimal sumAmounts(List<SpendingLineResponse> spendings) {
        return spendings.stream()
                .map(SpendingLineResponse::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal savingsAmount(List<SpendingLineResponse> spendings) {
        return spendings.stream()
                .filter(SpendingLineResponse::isSavings)
                .map(SpendingLineResponse::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static YearMonth inheritedFrom(MonthlyIncome income, YearMonth yearMonth) {
        if (income == null || income.getEffectiveFromMonth().equals(yearMonth)) {
            return null;
        }
        return income.getEffectiveFromMonth();
    }
}
