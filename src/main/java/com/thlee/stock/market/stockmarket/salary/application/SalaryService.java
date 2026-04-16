package com.thlee.stock.market.stockmarket.salary.application;

import com.thlee.stock.market.stockmarket.salary.application.dto.MonthlySalaryResponse;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 월급 사용 비율 도메인의 조회 / upsert / delete 유스케이스.
 *
 * <p>upsert는 Java 레벨 3단계 로직: 해당 월 직접 레코드 조회 → 상속값 비교 → insert.
 * {@code ON CONFLICT DO UPDATE} 네이티브 UPSERT는 사용하지 않는다 — "상속값과 동일이면 noop,
 * 다르면 새 row insert"라는 비즈니스 룰이 {@code ON CONFLICT UPDATE} 의미와 다르기 때문.
 * 동시성 레이스는 {@code UNIQUE} 제약 + {@code DataIntegrityViolationException} → 409 매핑으로 방어.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SalaryService {

    private static final List<SpendingCategory> ALL_CATEGORIES = List.of(SpendingCategory.values());

    private final MonthlyIncomeRepository monthlyIncomeRepository;
    private final SpendingConfigRepository spendingConfigRepository;

    // =========================================================================
    // 조회 (readOnly)
    // =========================================================================

    /** 특정 월의 월급 사용 현황 (상속 적용). */
    @Transactional(readOnly = true)
    public MonthlySalaryResponse getMonthly(Long userId, YearMonth yearMonth) {
        validateUserId(userId);
        requireYearMonth(yearMonth);

        Optional<MonthlyIncome> incomeOpt = monthlyIncomeRepository.findEffectiveAsOf(userId, yearMonth);
        List<SpendingConfig> configs = spendingConfigRepository.findEffectiveAsOf(userId, yearMonth);

        Map<SpendingCategory, SpendingConfig> byCategory = configs.stream()
                .collect(Collectors.toMap(SpendingConfig::getCategory, Function.identity()));

        List<SpendingLineResponse> lines = ALL_CATEGORIES.stream()
                .map(cat -> SpendingLineResponse.from(cat, byCategory.get(cat), yearMonth))
                .collect(Collectors.toList());

        return MonthlySalaryResponse.from(yearMonth, incomeOpt.orElse(null), lines);
    }

    /** 변경 레코드가 존재하는 월 목록 (최신 우선). 월급/지출 어느 쪽이든 있는 달은 포함. */
    @Transactional(readOnly = true)
    public List<YearMonth> getAvailableMonths(Long userId) {
        validateUserId(userId);
        TreeSet<YearMonth> merged = new TreeSet<>(Comparator.reverseOrder());
        merged.addAll(monthlyIncomeRepository.findDistinctMonths(userId));
        merged.addAll(spendingConfigRepository.findDistinctMonths(userId));
        return new ArrayList<>(merged);
    }

    // =========================================================================
    // 쓰기 (트랜잭션)
    // =========================================================================

    @Transactional
    public UpsertResultResponse upsertIncome(Long userId, YearMonth yearMonth, BigDecimal amount) {
        validateUserId(userId);
        requireYearMonth(yearMonth);
        YearMonth referenceMonth = YearMonth.now();

        // 1. 해당 월 직접 레코드 존재?
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

        // 2. 상속값과 비교 (상속값과 동일하면 불필요 레코드 생성 방지)
        Optional<MonthlyIncome> inherited = monthlyIncomeRepository.findEffectiveAsOf(userId, yearMonth);
        if (inherited.isPresent() && inherited.get().isSameAmountAs(amount)) {
            return UpsertResultResponse.noop(inherited.get().getEffectiveFromMonth());
        }

        // 3. 새 레코드 생성
        MonthlyIncome created = MonthlyIncome.create(userId, yearMonth, referenceMonth, amount);
        monthlyIncomeRepository.save(created);
        log.info("upsertIncome created: userId={}, yearMonth={}, amount={}", userId, yearMonth, amount);
        return UpsertResultResponse.created();
    }

    @Transactional
    public UpsertResultResponse upsertSpending(Long userId, SpendingCategory category,
                                               YearMonth yearMonth, BigDecimal amount, String memo) {
        validateUserId(userId);
        requireCategory(category);
        requireYearMonth(yearMonth);
        YearMonth referenceMonth = YearMonth.now();

        // 1. 해당 월·카테고리 직접 레코드 존재?
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

        // 2. 상속값과 비교 — 카테고리별 상속 조회 후 필터링
        Optional<SpendingConfig> inherited = spendingConfigRepository
                .findEffectiveAsOf(userId, yearMonth).stream()
                .filter(c -> c.getCategory() == category)
                .findFirst();
        if (inherited.isPresent() && inherited.get().isSameAs(amount, memo)) {
            return UpsertResultResponse.noop(inherited.get().getEffectiveFromMonth());
        }

        // 3. 새 레코드 생성
        SpendingConfig created = SpendingConfig.create(userId, category, yearMonth, referenceMonth, amount, memo);
        spendingConfigRepository.save(created);
        log.info("upsertSpending created: userId={}, category={}, yearMonth={}, amount={}",
                userId, category, yearMonth, amount);
        return UpsertResultResponse.created();
    }

    @Transactional
    public void deleteIncome(Long userId, YearMonth yearMonth) {
        validateUserId(userId);
        requireYearMonth(yearMonth);
        monthlyIncomeRepository.deleteByUserIdAndEffectiveFromMonth(userId, yearMonth);
    }

    @Transactional
    public void deleteSpending(Long userId, SpendingCategory category, YearMonth yearMonth) {
        validateUserId(userId);
        requireCategory(category);
        requireYearMonth(yearMonth);
        spendingConfigRepository.deleteByUserIdAndCategoryAndEffectiveFromMonth(userId, category, yearMonth);
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private static void validateUserId(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId는 필수입니다.");
        }
    }

    private static void requireYearMonth(YearMonth yearMonth) {
        if (yearMonth == null) {
            throw new IllegalArgumentException("yearMonth는 필수입니다.");
        }
    }

    private static void requireCategory(SpendingCategory category) {
        if (category == null) {
            throw new IllegalArgumentException("카테고리는 필수입니다.");
        }
    }
}