package com.thlee.stock.market.stockmarket.salary.application.dto;

import com.thlee.stock.market.stockmarket.salary.domain.model.MonthlyIncome;
import com.thlee.stock.market.stockmarket.salary.domain.model.SpendingConfig;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 전월 유효값 요약 — "지난달 대비" 카드와 저축률 delta 계산용.
 */
@Getter
public class PreviousMonthResponse {

    private final YearMonth yearMonth;
    private final BigDecimal income;
    private final BigDecimal totalSpending;

    /** 저축율 scale=4. income이 null이거나 0이면 null. */
    private final BigDecimal savingsRatio;

    private final List<CategoryAmountResponse> categoryTotals;

    private PreviousMonthResponse(YearMonth yearMonth, BigDecimal income, BigDecimal totalSpending,
                                  BigDecimal savingsRatio, List<CategoryAmountResponse> categoryTotals) {
        this.yearMonth = yearMonth;
        this.income = income;
        this.totalSpending = totalSpending;
        this.savingsRatio = savingsRatio;
        this.categoryTotals = categoryTotals;
    }

    /**
     * @param savingsCodes 저축률 산입 카테고리 code 집합 (savings 플래그)
     */
    public static PreviousMonthResponse from(YearMonth yearMonth, MonthlyIncome income,
                                             List<SpendingConfig> configs, Set<String> savingsCodes) {
        List<CategoryAmountResponse> totals = toCategoryTotals(configs);
        BigDecimal totalSpending = sumAmounts(totals);
        BigDecimal savingsRatio = income == null
                ? null
                : income.calculateSavingsRatio(savingsAmount(totals, savingsCodes));
        return new PreviousMonthResponse(yearMonth, income == null ? null : income.getAmount(),
                totalSpending, savingsRatio, totals);
    }

    private static List<CategoryAmountResponse> toCategoryTotals(List<SpendingConfig> configs) {
        return configs.stream()
                .map(c -> new CategoryAmountResponse(c.getCategory(), c.getAmount()))
                .collect(Collectors.toList());
    }

    private static BigDecimal sumAmounts(List<CategoryAmountResponse> totals) {
        return totals.stream()
                .map(CategoryAmountResponse::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal savingsAmount(List<CategoryAmountResponse> totals, Set<String> savingsCodes) {
        return totals.stream()
                .filter(t -> savingsCodes.contains(t.getCategory()))
                .map(CategoryAmountResponse::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
