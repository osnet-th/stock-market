package com.thlee.stock.market.stockmarket.salary.application;

import com.thlee.stock.market.stockmarket.salary.application.dto.CategoryAmountResponse;
import com.thlee.stock.market.stockmarket.salary.application.dto.MonthlySalaryResponse;
import com.thlee.stock.market.stockmarket.salary.application.dto.PreviousMonthResponse;
import com.thlee.stock.market.stockmarket.salary.application.dto.SalaryTrendResponse;
import com.thlee.stock.market.stockmarket.salary.application.dto.SpendingLineResponse;
import com.thlee.stock.market.stockmarket.salary.application.dto.UpsertResultResponse;
import com.thlee.stock.market.stockmarket.salary.domain.model.MonthlyIncome;
import com.thlee.stock.market.stockmarket.salary.domain.model.SpendingConfig;
import com.thlee.stock.market.stockmarket.salary.domain.model.SpendingItem;
import com.thlee.stock.market.stockmarket.salary.domain.model.SpendingItemSet;
import com.thlee.stock.market.stockmarket.salary.domain.model.enums.SpendingCategory;
import com.thlee.stock.market.stockmarket.salary.application.dto.SaveMonthlyCommand;
import com.thlee.stock.market.stockmarket.salary.domain.repository.MonthlyIncomeRepository;
import com.thlee.stock.market.stockmarket.salary.domain.repository.SpendingConfigRepository;
import com.thlee.stock.market.stockmarket.salary.domain.repository.SpendingItemSetRepository;
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
    private final SpendingItemSetRepository spendingItemSetRepository;

    // =========================================================================
    // 조회 (readOnly)
    // =========================================================================

    /** 특정 월의 월급 사용 현황 (상속 적용) + 전월 요약. */
    @Transactional(readOnly = true)
    public MonthlySalaryResponse getMonthly(Long userId, YearMonth yearMonth) {
        Optional<MonthlyIncome> incomeOpt = monthlyIncomeRepository.findEffectiveAsOf(userId, yearMonth);
        SpendingItemSet itemSet = spendingItemSetRepository.findEffectiveAsOf(userId, yearMonth).orElse(null);
        List<SpendingLineResponse> lines = buildLines(userId, yearMonth, itemSet);
        return MonthlySalaryResponse.from(yearMonth, incomeOpt.orElse(null), lines,
                itemsInheritedFrom(itemSet, yearMonth), buildPreviousSummary(userId, yearMonth.minusMonths(1)));
    }

    private List<SpendingLineResponse> buildLines(Long userId, YearMonth yearMonth, SpendingItemSet itemSet) {
        Map<SpendingCategory, SpendingConfig> byCategory = effectiveConfigsByCategory(userId, yearMonth);
        return ALL_CATEGORIES.stream()
                .map(cat -> SpendingLineResponse.from(cat, byCategory.get(cat), itemsOf(itemSet, cat), yearMonth))
                .collect(Collectors.toList());
    }

    private Map<SpendingCategory, SpendingConfig> effectiveConfigsByCategory(Long userId, YearMonth yearMonth) {
        return spendingConfigRepository.findEffectiveAsOf(userId, yearMonth).stream()
                .collect(Collectors.toMap(SpendingConfig::getCategory, Function.identity()));
    }

    private static List<SpendingItem> itemsOf(SpendingItemSet itemSet, SpendingCategory category) {
        return itemSet == null ? List.of() : itemSet.itemsOf(category);
    }

    private static YearMonth itemsInheritedFrom(SpendingItemSet itemSet, YearMonth targetMonth) {
        if (itemSet == null || itemSet.getEffectiveFromMonth().equals(targetMonth)) {
            return null;
        }
        return itemSet.getEffectiveFromMonth();
    }

    private PreviousMonthResponse buildPreviousSummary(Long userId, YearMonth prevMonth) {
        Optional<MonthlyIncome> income = monthlyIncomeRepository.findEffectiveAsOf(userId, prevMonth);
        List<SpendingConfig> configs = spendingConfigRepository.findEffectiveAsOf(userId, prevMonth);
        if (income.isEmpty() && configs.isEmpty()) {
            return null;
        }
        return PreviousMonthResponse.from(prevMonth, income.orElse(null), configs);
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
            points.add(buildTrendPoint(cursor, allIncomes, byCategory));
            cursor = cursor.plusMonths(1);
        }

        return SalaryTrendResponse.of(points);
    }

    private SalaryTrendResponse.TrendPoint buildTrendPoint(YearMonth cursor, List<MonthlyIncome> allIncomes,
                                                           Map<SpendingCategory, List<SpendingConfig>> byCategory) {
        MonthlyIncome income = latestIncomeAsOf(allIncomes, cursor);
        List<CategoryAmountResponse> categoryTotals = effectiveCategoryTotals(byCategory, cursor);
        BigDecimal totalSpending = sumAmounts(categoryTotals);
        BigDecimal savingsRatio = income == null
                ? null
                : income.calculateSavingsRatio(amountOf(categoryTotals, SpendingCategory.SAVINGS_INVESTMENT));
        return new SalaryTrendResponse.TrendPoint(cursor, income != null ? income.getAmount() : null,
                totalSpending, savingsRatio, categoryTotals);
    }

    /** 각 카테고리의 해당 월 유효 금액. 8개 카테고리 모두 포함(레코드 없으면 0). */
    private List<CategoryAmountResponse> effectiveCategoryTotals(
            Map<SpendingCategory, List<SpendingConfig>> byCategory, YearMonth cursor) {
        List<CategoryAmountResponse> totals = new ArrayList<>();
        for (SpendingCategory cat : SpendingCategory.values()) {
            SpendingConfig effective = latestConfigAsOf(byCategory.get(cat), cursor);
            totals.add(new CategoryAmountResponse(cat, effective != null ? effective.getAmount() : BigDecimal.ZERO));
        }
        return totals;
    }

    private static BigDecimal sumAmounts(List<CategoryAmountResponse> totals) {
        return totals.stream()
                .map(CategoryAmountResponse::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal amountOf(List<CategoryAmountResponse> totals, SpendingCategory category) {
        return totals.stream()
                .filter(t -> t.getCategory() == category)
                .map(CategoryAmountResponse::getAmount)
                .findFirst()
                .orElse(BigDecimal.ZERO);
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

        BigDecimal inheritedBudget = inherited.map(SpendingConfig::getBudget).orElse(null);
        SpendingConfig created = SpendingConfig.create(userId, category, yearMonth, referenceMonth,
                amount, memo, inheritedBudget);
        spendingConfigRepository.save(created);
        log.info("upsertSpending created: userId={}, category={}, yearMonth={}, amount={}",
                userId, category, yearMonth, amount);
        return UpsertResultResponse.created();
    }

    /**
     * 해당 월 일괄 저장 — 월급 + 카테고리(금액·예산) + 하위 항목 세트를 한 트랜잭션으로 upsert.
     * 개별 upsert와 동일한 NOOP 의미론(상속값과 동일하면 레코드 미생성)을 유지한다.
     * 항목이 있는 카테고리의 금액은 항목 합계 파생값으로 저장된다.
     */
    @Transactional
    public MonthlySalaryResponse saveMonthly(Long userId, YearMonth yearMonth, SaveMonthlyCommand command) {
        if (command.getIncome() != null) {
            upsertIncome(userId, yearMonth, command.getIncome());
        }
        upsertCategoryConfigs(userId, yearMonth, command.getCategories());
        upsertItemSet(userId, yearMonth, command.getCategories());
        return getMonthly(userId, yearMonth);
    }

    private void upsertCategoryConfigs(Long userId, YearMonth yearMonth,
                                       List<SaveMonthlyCommand.CategoryCommand> categories) {
        Map<SpendingCategory, SpendingConfig> effective = effectiveConfigsByCategory(userId, yearMonth);
        categories.forEach(c -> upsertCategoryConfig(userId, yearMonth, c, effective.get(c.getCategory())));
    }

    private void upsertCategoryConfig(Long userId, YearMonth yearMonth,
                                      SaveMonthlyCommand.CategoryCommand payload, SpendingConfig effective) {
        Optional<SpendingConfig> direct = spendingConfigRepository
                .findByUserIdAndCategoryAndEffectiveFromMonth(userId, payload.getCategory(), yearMonth);
        if (direct.isPresent()) {
            updateConfigIfChanged(direct.get(), payload.resolvedAmount(), payload.getBudget());
            return;
        }
        createConfigIfChanged(userId, yearMonth, payload, effective);
    }

    private void updateConfigIfChanged(SpendingConfig config, BigDecimal amount, BigDecimal budget) {
        if (config.isSameAmountAndBudgetAs(amount, budget)) {
            return;
        }
        config.updateAmountAndBudget(amount, budget);
        spendingConfigRepository.save(config);
    }

    /** 상속값과 다를 때만 새 레코드 생성. 상속 메모는 이어받아 보존한다. */
    private void createConfigIfChanged(Long userId, YearMonth yearMonth,
                                       SaveMonthlyCommand.CategoryCommand payload, SpendingConfig inherited) {
        BigDecimal amount = payload.resolvedAmount();
        if (inherited != null && inherited.isSameAmountAndBudgetAs(amount, payload.getBudget())) {
            return;
        }
        if (inherited == null && isBlankConfig(amount, payload.getBudget())) {
            return;
        }
        String memo = inherited != null ? inherited.getMemo() : null;
        spendingConfigRepository.save(SpendingConfig.create(userId, payload.getCategory(), yearMonth,
                YearMonth.now(), amount, memo, payload.getBudget()));
    }

    private static boolean isBlankConfig(BigDecimal amount, BigDecimal budget) {
        return amount.signum() == 0 && (budget == null || budget.signum() == 0);
    }

    private void upsertItemSet(Long userId, YearMonth yearMonth,
                               List<SaveMonthlyCommand.CategoryCommand> categories) {
        List<SpendingItem> newItems = toSpendingItems(categories);
        Optional<SpendingItemSet> direct = spendingItemSetRepository
                .findByUserIdAndEffectiveFromMonth(userId, yearMonth);
        if (direct.isPresent()) {
            replaceItemsIfChanged(direct.get(), newItems);
            return;
        }
        createItemSetIfChanged(userId, yearMonth, newItems);
    }

    private void replaceItemsIfChanged(SpendingItemSet itemSet, List<SpendingItem> newItems) {
        if (itemSet.hasSameItemsAs(newItems)) {
            return;
        }
        itemSet.replaceItems(newItems);
        spendingItemSetRepository.save(itemSet);
    }

    private void createItemSetIfChanged(Long userId, YearMonth yearMonth, List<SpendingItem> newItems) {
        Optional<SpendingItemSet> effective = spendingItemSetRepository.findEffectiveAsOf(userId, yearMonth);
        if (effective.isPresent() && effective.get().hasSameItemsAs(newItems)) {
            return;
        }
        if (effective.isEmpty() && newItems.isEmpty()) {
            return;
        }
        spendingItemSetRepository.save(SpendingItemSet.create(userId, yearMonth, YearMonth.now(), newItems));
    }

    /** 요청 순서를 유지한 채 전 카테고리 항목을 하나의 세트 스냅샷으로 펼친다. */
    private static List<SpendingItem> toSpendingItems(List<SaveMonthlyCommand.CategoryCommand> categories) {
        List<SpendingItem> items = new ArrayList<>();
        for (SaveMonthlyCommand.CategoryCommand c : categories) {
            c.getItems().forEach(i -> items.add(
                    SpendingItem.create(c.getCategory(), i.getName(), i.getAmount(), i.isFixed(), items.size())));
        }
        return items;
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