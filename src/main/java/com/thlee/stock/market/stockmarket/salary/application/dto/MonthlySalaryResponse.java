package com.thlee.stock.market.stockmarket.salary.application.dto;

import com.thlee.stock.market.stockmarket.salary.domain.model.MonthlyIncome;
import com.thlee.stock.market.stockmarket.salary.domain.model.enums.SpendingCategory;
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

    /** 8개 카테고리 모두 포함. 값 없는 카테고리는 {@code amount=0}. */
    private final List<SpendingLineResponse> spendings;

    private final BigDecimal totalSpending;

    /** {@code income - totalSpending}. income이 null이면 null. 음수 허용(초과 지출). */
    private final BigDecimal remaining;

    /** 저축율: SAVINGS_INVESTMENT / income, scale=4. income이 0이거나 null이면 null. */
    private final BigDecimal savingsRatio;

    /** 온보딩 화면 판정용. 월급 또는 지출이 하나라도 있으면 true. */
    private final boolean hasAnyData;

    private MonthlySalaryResponse(YearMonth yearMonth, BigDecimal income, YearMonth incomeInheritedFromMonth,
                                  List<SpendingLineResponse> spendings, BigDecimal totalSpending,
                                  BigDecimal remaining, BigDecimal savingsRatio, boolean hasAnyData) {
        this.yearMonth = yearMonth;
        this.income = income;
        this.incomeInheritedFromMonth = incomeInheritedFromMonth;
        this.spendings = spendings;
        this.totalSpending = totalSpending;
        this.remaining = remaining;
        this.savingsRatio = savingsRatio;
        this.hasAnyData = hasAnyData;
    }

    public static MonthlySalaryResponse from(YearMonth yearMonth,
                                             MonthlyIncome income,
                                             List<SpendingLineResponse> spendings) {
        BigDecimal totalSpending = spendings.stream()
                .map(SpendingLineResponse::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal incomeAmount = null;
        YearMonth incomeInherited = null;
        BigDecimal remaining = null;
        BigDecimal savingsRatio = null;

        if (income != null) {
            incomeAmount = income.getAmount();
            incomeInherited = income.getEffectiveFromMonth().equals(yearMonth)
                    ? null
                    : income.getEffectiveFromMonth();
            remaining = incomeAmount.subtract(totalSpending);

            BigDecimal savingsAmount = spendings.stream()
                    .filter(s -> s.getCategory() == SpendingCategory.SAVINGS_INVESTMENT)
                    .map(SpendingLineResponse::getAmount)
                    .findFirst()
                    .orElse(BigDecimal.ZERO);
            savingsRatio = income.calculateSavingsRatio(savingsAmount);
        }

        boolean hasAnyData = income != null
                || spendings.stream().anyMatch(s -> s.getAmount().signum() > 0);

        return new MonthlySalaryResponse(yearMonth, incomeAmount, incomeInherited, spendings,
                totalSpending, remaining, savingsRatio, hasAnyData);
    }
}