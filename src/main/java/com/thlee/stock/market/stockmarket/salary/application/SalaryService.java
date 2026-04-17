package com.thlee.stock.market.stockmarket.salary.application;

import com.thlee.stock.market.stockmarket.salary.application.dto.MonthlySalaryResponse;
import com.thlee.stock.market.stockmarket.salary.application.dto.SalaryTrendResponse;
import com.thlee.stock.market.stockmarket.salary.application.dto.SpendingLineResponse;
import com.thlee.stock.market.stockmarket.salary.application.dto.UpsertResultResponse;
import com.thlee.stock.market.stockmarket.salary.domain.model.MonthlyIncome;
import com.thlee.stock.market.stockmarket.salary.domain.model.SpendingConfig;
import com.thlee.stock.market.stockmarket.salary.domain.model.enums.SpendingCategory;
import com.thlee.stock.market.stockmarket.salary.domain.repository.MonthlyIncomeRepository;
import com.thlee.stock.market.stockmarket.salary.domain.repository.SpendingConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 월급 사용 비율 도메인 — 조회 / upsert / delete / 추이 유스케이스 통합.
 *
 * <p>upsert는 Java 레벨 3단계 로직: 해당 월 직접 레코드 조회 → 상속값 비교 → insert.
 * 동시성 레이스는 {@code UNIQUE} 제약 + {@code DataIntegrityViolationException} → 409 매핑으로 방어.
 *
 * <p>파라미터 null 검증은 Controller {@code @RequestParam}/@PathVariable이 보장하므로
 * Service에서는 중복하지 않는다. 도메인 팩토리({@code create()})가 비즈니스 규칙을 검증한다.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SalaryService {

    private static final List<SpendingCategory> ALL_CATEGORIES = List.of(SpendingCategory.values());
    private static final int TREND_MIN_MONTHS = 1;
    private static final int TREND_MAX_MONTHS = 60;

    private final MonthlyIncomeRepository monthlyIncomeRepository;
    private final SpendingConfigRepository spendingConfigRepository;

    // =========================================================================
    // 조회 (readOnly)
    // =========================================================================

    /** 특정 월의 월급 사용 현황 (상속 적용). */
    @Transactional(readOnly = true)
    public MonthlySalaryResponse getMonthly(Long userId, YearMonth yearMonth) {
        Optional<MonthlyIncome> incomeOpt = monthlyIncomeRepository.findEffectiveAsOf(userId, yearMonth);
        List<SpendingConfig> configs = spendingConfigRepository.findEffectiveAsOf(userId, yearMonth);

        Map<SpendingCategory, SpendingConfig> byCategory = configs.stream()
                .collect(Collectors.toMap(SpendingConfig::getCategory, Function.identity()));

        List<SpendingLineResponse> lines = ALL_CATEGORIES.stream()
                .map(cat -> SpendingLineResponse.from(cat, byCategory.get(cat), yearMonth))
                .collect(Collectors.toList());

        return MonthlySalaryResponse.from(yearMonth, incomeOpt.orElse(null), lines);
    }

    /** 변경 레코드가 존재하는 월 목록 (최신 우선). */
    @Transactional(readOnly = true)
    public List<YearMonth> getAvailableMonths(Long userId) {
        TreeSet<YearMonth> merged = new TreeSet<>(Comparator.reverseOrder());
        merged.addAll(monthlyIncomeRepository.findDistinctMonths(userId));
        merged.addAll(spendingConfigRepository.findDistinctMonths(userId));
        return new ArrayList<>(merged);
    }

    /**
     * 최근 N개월 추이. 카테고리별 변경 이력을 메모리에서 롤링 포워드하여 각 월 유효값을 계산.
     * 기록 시작 이전 월은 X축에 포함하지 않는다.
     */
    @Transactional(readOnly = true)
    public SalaryTrendResponse getTrend(Long userId, int months) {
        if (months < TREND_MIN_MONTHS || months > TREND_MAX_MONTHS) {
            throw new IllegalArgumentException(
                    "months는 " + TREND_MIN_MONTHS + " 이상 " + TREND_MAX_MONTHS + " 이하여야 합니다.");
        }

        YearMonth today = YearMonth.now();
        YearMonth windowStart = today.minusMonths((long) months - 1);

        List<MonthlyIncome> allIncomes = monthlyIncomeRepository.findAllUpTo(userId, today);
        List<SpendingConfig> allConfigs = spendingConfigRepository.findAllUpTo(userId, today);

        if (allIncomes.isEmpty() && allConfigs.isEmpty()) {
            return SalaryTrendResponse.empty();
        }

        YearMonth firstRecorded = earliestRecordedMonth(allIncomes, allConfigs);
        YearMonth rangeStart = firstRecorded.isAfter(windowStart) ? firstRecorded : windowStart;

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
                if (effective == null) continue;
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

    // =========================================================================
    // 쓰기 (트랜잭션)
    // =========================================================================

    @Transactional
    public UpsertResultResponse upsertIncome(Long userId, YearMonth yearMonth, BigDecimal amount) {
        YearMonth referenceMonth = YearMonth.now();

        Optional<MonthlyIncome> existing = monthlyIncomeRepository
                .findByUserIdAndEffectiveFromMonth(userId, yearMonth);
        if (existing.isPresent()) {
            MonthlyIncome income = existing.get();
            if (income.isSameAmountAs(amount)) {
                return UpsertResultResponse.noop(income.getEffectiveFromMonth());
            }
            income.updateAmount(amount);
            monthlyIncomeRepository.save(income);
            return UpsertResultResponse.updated();
        }

        Optional<MonthlyIncome> inherited = monthlyIncomeRepository.findEffectiveAsOf(userId, yearMonth);
        if (inherited.isPresent() && inherited.get().isSameAmountAs(amount)) {
            return UpsertResultResponse.noop(inherited.get().getEffectiveFromMonth());
        }

        MonthlyIncome created = MonthlyIncome.create(userId, yearMonth, referenceMonth, amount);
        monthlyIncomeRepository.save(created);
        log.info("upsertIncome created: userId={}, yearMonth={}, amount={}", userId, yearMonth, amount);
        return UpsertResultResponse.created();
    }

    @Transactional
    public UpsertResultResponse upsertSpending(Long userId, SpendingCategory category,
                                               YearMonth yearMonth, BigDecimal amount, String memo) {
        YearMonth referenceMonth = YearMonth.now();

        Optional<SpendingConfig> existing = spendingConfigRepository
                .findByUserIdAndCategoryAndEffectiveFromMonth(userId, category, yearMonth);
        if (existing.isPresent()) {
            SpendingConfig config = existing.get();
            if (config.isSameAs(amount, memo)) {
                return UpsertResultResponse.noop(config.getEffectiveFromMonth());
            }
            config.updateAmountAndMemo(amount, memo);
            spendingConfigRepository.save(config);
            return UpsertResultResponse.updated();
        }

        Optional<SpendingConfig> inherited = spendingConfigRepository
                .findEffectiveAsOf(userId, yearMonth).stream()
                .filter(c -> c.getCategory() == category)
                .findFirst();
        if (inherited.isPresent() && inherited.get().isSameAs(amount, memo)) {
            return UpsertResultResponse.noop(inherited.get().getEffectiveFromMonth());
        }

        SpendingConfig created = SpendingConfig.create(userId, category, yearMonth, referenceMonth, amount, memo);
        spendingConfigRepository.save(created);
        log.info("upsertSpending created: userId={}, category={}, yearMonth={}, amount={}",
                userId, category, yearMonth, amount);
        return UpsertResultResponse.created();
    }

    @Transactional
    public void deleteIncome(Long userId, YearMonth yearMonth) {
        monthlyIncomeRepository.deleteByUserIdAndEffectiveFromMonth(userId, yearMonth);
    }

    @Transactional
    public void deleteSpending(Long userId, SpendingCategory category, YearMonth yearMonth) {
        spendingConfigRepository.deleteByUserIdAndCategoryAndEffectiveFromMonth(userId, category, yearMonth);
    }

    // =========================================================================
    // 추이 계산 private 헬퍼
    // =========================================================================

    private MonthlyIncome latestIncomeAsOf(List<MonthlyIncome> list, YearMonth target) {
        MonthlyIncome result = null;
        for (MonthlyIncome m : list) {
            if (m.getEffectiveFromMonth().isAfter(target)) break;
            result = m;
        }
        return result;
    }

    private SpendingConfig latestConfigAsOf(List<SpendingConfig> list, YearMonth target) {
        SpendingConfig result = null;
        for (SpendingConfig c : list) {
            if (c.getEffectiveFromMonth().isAfter(target)) break;
            result = c;
        }
        return result;
    }

    private YearMonth earliestRecordedMonth(List<MonthlyIncome> incomes, List<SpendingConfig> configs) {
        YearMonth min = null;
        for (MonthlyIncome m : incomes) {
            if (min == null || m.getEffectiveFromMonth().isBefore(min)) min = m.getEffectiveFromMonth();
        }
        for (SpendingConfig c : configs) {
            if (min == null || c.getEffectiveFromMonth().isBefore(min)) min = c.getEffectiveFromMonth();
        }
        return min;
    }
}