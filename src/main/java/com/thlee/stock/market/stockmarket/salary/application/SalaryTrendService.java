package com.thlee.stock.market.stockmarket.salary.application;

import com.thlee.stock.market.stockmarket.salary.application.dto.SalaryTrendResponse;
import com.thlee.stock.market.stockmarket.salary.domain.model.MonthlyIncome;
import com.thlee.stock.market.stockmarket.salary.domain.model.SpendingConfig;
import com.thlee.stock.market.stockmarket.salary.domain.model.enums.SpendingCategory;
import com.thlee.stock.market.stockmarket.salary.domain.repository.MonthlyIncomeRepository;
import com.thlee.stock.market.stockmarket.salary.domain.repository.SpendingConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 최근 N개월 추이 계산. 카테고리별로 정렬된 변경 이력을 메모리에서 롤링 포워드하여
 * 각 월의 유효값을 O(N) 선형 스캔으로 계산한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SalaryTrendService {

    private static final int MIN_MONTHS = 1;
    private static final int MAX_MONTHS = 60;

    private final MonthlyIncomeRepository monthlyIncomeRepository;
    private final SpendingConfigRepository spendingConfigRepository;

    public SalaryTrendResponse getTrend(Long userId, int months) {
        if (userId == null) {
            throw new IllegalArgumentException("userId는 필수입니다.");
        }
        if (months < MIN_MONTHS || months > MAX_MONTHS) {
            throw new IllegalArgumentException(
                    "months는 " + MIN_MONTHS + " 이상 " + MAX_MONTHS + " 이하여야 합니다.");
        }

        YearMonth today = YearMonth.now();
        YearMonth windowStart = today.minusMonths((long) months - 1);

        List<MonthlyIncome> allIncomes = monthlyIncomeRepository.findAllUpTo(userId, today);
        List<SpendingConfig> allConfigs = spendingConfigRepository.findAllUpTo(userId, today);

        if (allIncomes.isEmpty() && allConfigs.isEmpty()) {
            return SalaryTrendResponse.empty();
        }

        // 기록 시작 월 이전은 X축에 포함하지 않는다 (계획서 규칙)
        YearMonth firstRecorded = earliestRecordedMonth(allIncomes, allConfigs);
        YearMonth rangeStart = firstRecorded.isAfter(windowStart) ? firstRecorded : windowStart;

        // 카테고리별로 분류 (effective_from ASC 정렬 유지)
        Map<SpendingCategory, List<SpendingConfig>> byCategory = new EnumMap<>(SpendingCategory.class);
        for (SpendingCategory cat : SpendingCategory.values()) {
            byCategory.put(cat, new ArrayList<>());
        }
        for (SpendingConfig c : allConfigs) {
            byCategory.get(c.getCategory()).add(c);
        }

        List<SalaryTrendResponse.TrendPoint> points = new ArrayList<>();
        YearMonth cursor = rangeStart;
        while (!cursor.isAfter(today)) {
            MonthlyIncome income = latestIncomeAsOf(allIncomes, cursor);
            BigDecimal incomeAmount = income != null ? income.getAmount() : null;

            BigDecimal totalSpending = BigDecimal.ZERO;
            BigDecimal savingsAmount = BigDecimal.ZERO;
            for (SpendingCategory cat : SpendingCategory.values()) {
                SpendingConfig effective = latestConfigAsOf(byCategory.get(cat), cursor);
                if (effective == null) {
                    continue;
                }
                totalSpending = totalSpending.add(effective.getAmount());
                if (cat == SpendingCategory.SAVINGS_INVESTMENT) {
                    savingsAmount = effective.getAmount();
                }
            }

            BigDecimal savingsRatio = income != null ? income.calculateSavingsRatio(savingsAmount) : null;
            points.add(new SalaryTrendResponse.TrendPoint(cursor, incomeAmount, totalSpending, savingsRatio));
            cursor = cursor.plusMonths(1);
        }

        return SalaryTrendResponse.of(points);
    }

    /** {@code list}는 effectiveFromMonth ASC로 정렬되어 있다고 가정. */
    private MonthlyIncome latestIncomeAsOf(List<MonthlyIncome> list, YearMonth target) {
        MonthlyIncome result = null;
        for (MonthlyIncome m : list) {
            if (m.getEffectiveFromMonth().isAfter(target)) {
                break;
            }
            result = m;
        }
        return result;
    }

    /** {@code list}는 effectiveFromMonth ASC로 정렬되어 있다고 가정. */
    private SpendingConfig latestConfigAsOf(List<SpendingConfig> list, YearMonth target) {
        SpendingConfig result = null;
        for (SpendingConfig c : list) {
            if (c.getEffectiveFromMonth().isAfter(target)) {
                break;
            }
            result = c;
        }
        return result;
    }

    private YearMonth earliestRecordedMonth(List<MonthlyIncome> incomes, List<SpendingConfig> configs) {
        YearMonth min = null;
        for (MonthlyIncome m : incomes) {
            if (min == null || m.getEffectiveFromMonth().isBefore(min)) {
                min = m.getEffectiveFromMonth();
            }
        }
        for (SpendingConfig c : configs) {
            if (min == null || c.getEffectiveFromMonth().isBefore(min)) {
                min = c.getEffectiveFromMonth();
            }
        }
        return min;
    }
}