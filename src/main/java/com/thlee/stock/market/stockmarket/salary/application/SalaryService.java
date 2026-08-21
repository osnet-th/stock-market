package com.thlee.stock.market.stockmarket.salary.application;

import com.thlee.stock.market.stockmarket.salary.application.dto.CategoryAmountResponse;
import com.thlee.stock.market.stockmarket.salary.application.dto.CategoryMetaResponse;
import com.thlee.stock.market.stockmarket.salary.application.dto.MonthlySalaryResponse;
import com.thlee.stock.market.stockmarket.salary.application.dto.PreviousMonthResponse;
import com.thlee.stock.market.stockmarket.salary.application.dto.SalaryTrendResponse;
import com.thlee.stock.market.stockmarket.salary.application.dto.SaveMonthlyCommand;
import com.thlee.stock.market.stockmarket.salary.application.dto.SpendingLineResponse;
import com.thlee.stock.market.stockmarket.salary.application.dto.UpsertResultResponse;
import com.thlee.stock.market.stockmarket.salary.domain.model.MonthlyIncome;
import com.thlee.stock.market.stockmarket.salary.domain.model.SalarySetting;
import com.thlee.stock.market.stockmarket.salary.domain.model.SpendingConfig;
import com.thlee.stock.market.stockmarket.salary.domain.model.SpendingItem;
import com.thlee.stock.market.stockmarket.salary.domain.model.SpendingItemSet;
import com.thlee.stock.market.stockmarket.salary.domain.model.UserSpendingCategory;
import com.thlee.stock.market.stockmarket.salary.domain.repository.MonthlyIncomeRepository;
import com.thlee.stock.market.stockmarket.salary.domain.repository.SalarySettingRepository;
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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 월급 사용 비율 도메인 — 조회 / upsert / delete / 일괄 저장 / 추이 유스케이스 통합.
 *
 * <p>upsert는 Java 레벨 3단계 로직: 해당 월 직접 레코드 조회 → 상속값 비교 → insert.
 * 동시성 레이스는 {@code UNIQUE} 제약 + {@code DataIntegrityViolationException} → 409 매핑으로 방어.
 *
 * <p>카테고리는 사용자 정의({@link UserSpendingCategory})다. 카테고리 구조(생성/삭제/이름변경)는
 * {@link SalaryCategoryService}가 담당하고, 본 서비스는 금액·예산·항목·설정의 Effective Date
 * 상속과 NOOP 의미론을 담당한다.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SalaryService {

    private static final int TREND_MIN_MONTHS = 1;
    private static final int TREND_MAX_MONTHS = 60;

    private final MonthlyIncomeRepository monthlyIncomeRepository;
    private final SpendingConfigRepository spendingConfigRepository;
    private final SpendingItemSetRepository spendingItemSetRepository;
    private final SalarySettingRepository salarySettingRepository;
    private final SalaryCategoryService salaryCategoryService;

    // =========================================================================
    // 조회 (readOnly)
    // =========================================================================

    /** 특정 월의 월급 사용 현황 (상속 적용) + 전월 요약. */
    @Transactional(readOnly = true)
    public MonthlySalaryResponse getMonthly(Long userId, YearMonth yearMonth) {
        List<UserSpendingCategory> categories = salaryCategoryService.loadOrDefaults(userId);
        Optional<MonthlyIncome> incomeOpt = monthlyIncomeRepository.findEffectiveAsOf(userId, yearMonth);
        SpendingItemSet itemSet = spendingItemSetRepository.findEffectiveAsOf(userId, yearMonth).orElse(null);
        List<SpendingLineResponse> lines = buildLines(userId, yearMonth, itemSet, categories);
        PreviousMonthResponse previous = buildPreviousSummary(
                userId, yearMonth.minusMonths(1), salaryCategoryService.savingsCodes(categories));
        return MonthlySalaryResponse.from(yearMonth, incomeOpt.orElse(null), lines,
                itemsInheritedFrom(itemSet, yearMonth), previous, savingTarget(userId));
    }

    private List<SpendingLineResponse> buildLines(Long userId, YearMonth yearMonth, SpendingItemSet itemSet,
                                                  List<UserSpendingCategory> categories) {
        Map<String, SpendingConfig> byCode = effectiveConfigsByCategory(userId, yearMonth);
        return visibleMetas(categories, byCode, itemSet).stream()
                .map(meta -> SpendingLineResponse.from(
                        meta, byCode.get(meta.getCode()), itemsOf(itemSet, meta.getCode()), yearMonth))
                .collect(Collectors.toList());
    }

    /**
     * 표시 대상 카테고리: 활성 전체 + 그 월에 금액/항목이 남아 있는 비활성(과거 이력 보존 표시).
     * 메타가 없는 code의 잔존 레코드는 방어적으로 unknown 메타로 노출한다.
     */
    private List<CategoryMetaResponse> visibleMetas(List<UserSpendingCategory> categories,
                                                    Map<String, SpendingConfig> byCode, SpendingItemSet itemSet) {
        List<CategoryMetaResponse> metas = new ArrayList<>();
        Set<String> known = new HashSet<>();
        for (UserSpendingCategory cat : categories) {
            known.add(cat.getCode());
            if (cat.isActive() || hasMonthData(byCode.get(cat.getCode()), itemSet, cat.getCode())) {
                metas.add(CategoryMetaResponse.from(cat));
            }
        }
        appendUnknownMetas(metas, known, byCode);
        return metas;
    }

    private static void appendUnknownMetas(List<CategoryMetaResponse> metas, Set<String> known,
                                           Map<String, SpendingConfig> byCode) {
        byCode.values().stream()
                .filter(c -> !known.contains(c.getCategory()) && c.getAmount().signum() > 0)
                .forEach(c -> metas.add(CategoryMetaResponse.unknown(c.getCategory())));
    }

    private static boolean hasMonthData(SpendingConfig config, SpendingItemSet itemSet, String code) {
        if (config != null && config.getAmount().signum() > 0) {
            return true;
        }
        return itemSet != null && !itemSet.itemsOf(code).isEmpty();
    }

    private Map<String, SpendingConfig> effectiveConfigsByCategory(Long userId, YearMonth yearMonth) {
        return spendingConfigRepository.findEffectiveAsOf(userId, yearMonth).stream()
                .collect(Collectors.toMap(SpendingConfig::getCategory, Function.identity(),
                        (a, b) -> a, LinkedHashMap::new));
    }

    private static List<SpendingItem> itemsOf(SpendingItemSet itemSet, String category) {
        return itemSet == null ? List.of() : itemSet.itemsOf(category);
    }

    private static YearMonth itemsInheritedFrom(SpendingItemSet itemSet, YearMonth targetMonth) {
        if (itemSet == null || itemSet.getEffectiveFromMonth().equals(targetMonth)) {
            return null;
        }
        return itemSet.getEffectiveFromMonth();
    }

    private PreviousMonthResponse buildPreviousSummary(Long userId, YearMonth prevMonth, Set<String> savingsCodes) {
        Optional<MonthlyIncome> income = monthlyIncomeRepository.findEffectiveAsOf(userId, prevMonth);
        List<SpendingConfig> configs = spendingConfigRepository.findEffectiveAsOf(userId, prevMonth);
        if (income.isEmpty() && configs.isEmpty()) {
            return null;
        }
        return PreviousMonthResponse.from(prevMonth, income.orElse(null), configs, savingsCodes);
    }

    private int savingTarget(Long userId) {
        return salarySettingRepository.findByUserId(userId)
                .map(SalarySetting::getSavingTargetPct)
                .orElse(SalarySetting.DEFAULT_SAVING_TARGET);
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
        validateTrendMonths(months);
        YearMonth today = YearMonth.now();
        List<MonthlyIncome> allIncomes = monthlyIncomeRepository.findAllUpTo(userId, today);
        List<SpendingConfig> allConfigs = spendingConfigRepository.findAllUpTo(userId, today);
        if (allIncomes.isEmpty() && allConfigs.isEmpty()) {
            return SalaryTrendResponse.empty();
        }
        return buildTrend(userId, today, today.minusMonths((long) months - 1), allIncomes, allConfigs);
    }

    private static void validateTrendMonths(int months) {
        if (months < TREND_MIN_MONTHS || months > TREND_MAX_MONTHS) {
            throw new IllegalArgumentException(
                    "months는 " + TREND_MIN_MONTHS + " 이상 " + TREND_MAX_MONTHS + " 이하여야 합니다.");
        }
    }

    private SalaryTrendResponse buildTrend(Long userId, YearMonth today, YearMonth windowStart,
                                           List<MonthlyIncome> allIncomes, List<SpendingConfig> allConfigs) {
        YearMonth firstRecorded = earliestRecordedMonth(allIncomes, allConfigs);
        YearMonth cursor = firstRecorded.isAfter(windowStart) ? firstRecorded : windowStart;
        Map<String, List<SpendingConfig>> byCategory = groupByCategory(allConfigs);
        List<UserSpendingCategory> categories = salaryCategoryService.loadOrDefaults(userId);
        Set<String> savingsCodes = salaryCategoryService.savingsCodes(categories);

        List<SalaryTrendResponse.TrendPoint> points = new ArrayList<>();
        while (!cursor.isAfter(today)) {
            points.add(buildTrendPoint(cursor, allIncomes, byCategory, savingsCodes));
            cursor = cursor.plusMonths(1);
        }
        return SalaryTrendResponse.of(points, trendMetas(byCategory.keySet(), categories));
    }

    private static Map<String, List<SpendingConfig>> groupByCategory(List<SpendingConfig> allConfigs) {
        Map<String, List<SpendingConfig>> byCategory = new LinkedHashMap<>();
        for (SpendingConfig c : allConfigs) {
            byCategory.computeIfAbsent(c.getCategory(), k -> new ArrayList<>()).add(c);
        }
        return byCategory;
    }

    /** 추이에 등장하는 code의 메타 (카테고리 정렬 순, 잔존 code는 unknown) */
    private static List<CategoryMetaResponse> trendMetas(Set<String> codes,
                                                         List<UserSpendingCategory> categories) {
        List<CategoryMetaResponse> metas = new ArrayList<>();
        Set<String> known = new HashSet<>();
        for (UserSpendingCategory cat : categories) {
            if (codes.contains(cat.getCode())) {
                metas.add(CategoryMetaResponse.from(cat));
                known.add(cat.getCode());
            }
        }
        codes.stream().filter(code -> !known.contains(code))
                .forEach(code -> metas.add(CategoryMetaResponse.unknown(code)));
        return metas;
    }

    private SalaryTrendResponse.TrendPoint buildTrendPoint(YearMonth cursor, List<MonthlyIncome> allIncomes,
                                                           Map<String, List<SpendingConfig>> byCategory,
                                                           Set<String> savingsCodes) {
        MonthlyIncome income = latestIncomeAsOf(allIncomes, cursor);
        List<CategoryAmountResponse> categoryTotals = effectiveCategoryTotals(byCategory, cursor);
        BigDecimal totalSpending = sumAmounts(categoryTotals);
        BigDecimal savingsRatio = income == null
                ? null
                : income.calculateSavingsRatio(savingsAmount(categoryTotals, savingsCodes));
        return new SalaryTrendResponse.TrendPoint(cursor, income != null ? income.getAmount() : null,
                totalSpending, savingsRatio, categoryTotals);
    }

    /** 각 카테고리의 해당 월 유효 금액 (레코드가 존재하는 카테고리만). */
    private List<CategoryAmountResponse> effectiveCategoryTotals(
            Map<String, List<SpendingConfig>> byCategory, YearMonth cursor) {
        List<CategoryAmountResponse> totals = new ArrayList<>();
        byCategory.forEach((code, configs) -> {
            SpendingConfig effective = latestConfigAsOf(configs, cursor);
            if (effective != null) {
                totals.add(new CategoryAmountResponse(code, effective.getAmount()));
            }
        });
        return totals;
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
    public UpsertResultResponse upsertSpending(Long userId, String category,
                                               YearMonth yearMonth, BigDecimal amount, String memo) {
        salaryCategoryService.requireKnownCategory(userId, category);
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
                .filter(c -> c.getCategory().equals(category))
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
     * 해당 월 일괄 저장 — 월급 + 카테고리 구조/금액·예산 + 하위 항목 세트 + 저축률 목표를
     * 한 트랜잭션으로 upsert. 개별 upsert와 동일한 NOOP 의미론을 유지한다.
     * 항목이 있는 카테고리의 금액은 항목 합계 파생값으로 저장된다.
     */
    @Transactional
    public MonthlySalaryResponse saveMonthly(Long userId, YearMonth yearMonth, SaveMonthlyCommand command) {
        if (command.getIncome() != null) {
            upsertIncome(userId, yearMonth, command.getIncome());
        }
        if (command.getSavingTarget() != null) {
            upsertSavingTarget(userId, command.getSavingTarget());
        }
        SalaryCategoryService.StructureResult structure =
                salaryCategoryService.applyStructure(userId, command.getCategories());
        upsertCategoryConfigs(userId, yearMonth, command.getCategories(), structure);
        upsertItemSet(userId, yearMonth, toSpendingItems(command.getCategories(), structure.getResolvedCodes()));
        return getMonthly(userId, yearMonth);
    }

    private void upsertSavingTarget(Long userId, int target) {
        SalarySetting setting = salarySettingRepository.findByUserId(userId).orElse(null);
        if (setting == null) {
            salarySettingRepository.save(SalarySetting.create(userId, target));
            return;
        }
        if (setting.getSavingTargetPct() == target) {
            return;
        }
        setting.updateSavingTarget(target);
        salarySettingRepository.save(setting);
    }

    private void upsertCategoryConfigs(Long userId, YearMonth yearMonth,
                                       List<SaveMonthlyCommand.CategoryCommand> categories,
                                       SalaryCategoryService.StructureResult structure) {
        Map<String, SpendingConfig> effective = effectiveConfigsByCategory(userId, yearMonth);
        for (int i = 0; i < categories.size(); i++) {
            String code = structure.getResolvedCodes().get(i);
            upsertCategoryConfig(userId, yearMonth, code, categories.get(i).resolvedAmount(),
                    categories.get(i).getBudget(), effective.get(code));
        }
        structure.getDeactivatedCodes().forEach(code -> upsertCategoryConfig(
                userId, yearMonth, code, BigDecimal.ZERO, null, effective.get(code)));
    }

    private void upsertCategoryConfig(Long userId, YearMonth yearMonth, String code,
                                      BigDecimal amount, BigDecimal budget, SpendingConfig effective) {
        Optional<SpendingConfig> direct = spendingConfigRepository
                .findByUserIdAndCategoryAndEffectiveFromMonth(userId, code, yearMonth);
        if (direct.isPresent()) {
            updateConfigIfChanged(direct.get(), amount, budget);
            return;
        }
        createConfigIfChanged(userId, yearMonth, code, amount, budget, effective);
    }

    private void updateConfigIfChanged(SpendingConfig config, BigDecimal amount, BigDecimal budget) {
        if (config.isSameAmountAndBudgetAs(amount, budget)) {
            return;
        }
        config.updateAmountAndBudget(amount, budget);
        spendingConfigRepository.save(config);
    }

    /** 상속값과 다를 때만 새 레코드 생성. 상속 메모는 이어받아 보존한다. */
    private void createConfigIfChanged(Long userId, YearMonth yearMonth, String code,
                                       BigDecimal amount, BigDecimal budget, SpendingConfig inherited) {
        if (inherited != null && inherited.isSameAmountAndBudgetAs(amount, budget)) {
            return;
        }
        if (inherited == null && isBlankConfig(amount, budget)) {
            return;
        }
        String memo = inherited != null ? inherited.getMemo() : null;
        spendingConfigRepository.save(SpendingConfig.create(userId, code, yearMonth,
                YearMonth.now(), amount, memo, budget));
    }

    private static boolean isBlankConfig(BigDecimal amount, BigDecimal budget) {
        return amount.signum() == 0 && (budget == null || budget.signum() == 0);
    }

    private void upsertItemSet(Long userId, YearMonth yearMonth, List<SpendingItem> newItems) {
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
    private static List<SpendingItem> toSpendingItems(List<SaveMonthlyCommand.CategoryCommand> categories,
                                                      List<String> resolvedCodes) {
        List<SpendingItem> items = new ArrayList<>();
        for (int i = 0; i < categories.size(); i++) {
            String code = resolvedCodes.get(i);
            categories.get(i).getItems().forEach(it -> items.add(
                    SpendingItem.create(code, it.getName(), it.getAmount(), it.isFixed(), items.size())));
        }
        return items;
    }

    @Transactional
    public void deleteIncome(Long userId, YearMonth yearMonth) {
        monthlyIncomeRepository.deleteByUserIdAndEffectiveFromMonth(userId, yearMonth);
    }

    @Transactional
    public void deleteSpending(Long userId, String category, YearMonth yearMonth) {
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
