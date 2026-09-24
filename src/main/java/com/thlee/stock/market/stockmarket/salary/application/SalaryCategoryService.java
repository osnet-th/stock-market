package com.thlee.stock.market.stockmarket.salary.application;

import com.thlee.stock.market.stockmarket.salary.application.dto.SaveMonthlyCommand;
import com.thlee.stock.market.stockmarket.salary.domain.model.UserSpendingCategory;
import com.thlee.stock.market.stockmarket.salary.domain.repository.UserSpendingCategoryRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 사용자 정의 카테고리 구조 관리 — lazy 시드, 일괄 저장의 생성/재활성/이름변경/비활성 반영.
 *
 * <p>읽기 경로는 {@link #loadOrDefaults}로 시드 없이 조회한다(readOnly 트랜잭션 호환).
 * 시드 영속화는 쓰기 경로({@link #applyStructure})에서만 일어난다.
 */
@Service
@RequiredArgsConstructor
public class SalaryCategoryService {

    private final UserSpendingCategoryRepository categoryRepository;

    /** 저장된 카테고리 전체(sort순). 없으면 기본 8종을 미영속 상태로 반환한다. */
    @Transactional(readOnly = true)
    public List<UserSpendingCategory> loadOrDefaults(Long userId) {
        List<UserSpendingCategory> all = categoryRepository.findAllByUserId(userId);
        return all.isEmpty() ? UserSpendingCategory.defaultSet(userId) : all;
    }

    /** 저축률 산입 카테고리 code 집합 */
    public Set<String> savingsCodes(List<UserSpendingCategory> categories) {
        Set<String> codes = new HashSet<>();
        categories.stream().filter(UserSpendingCategory::isSavings)
                .forEach(c -> codes.add(c.getCode()));
        return codes;
    }

    /** code가 사용자 카테고리(기본 8종 포함)에 존재하는지 검증 (레거시 endpoint용) */
    @Transactional(readOnly = true)
    public void requireKnownCategory(Long userId, String category) {
        boolean known = loadOrDefaults(userId).stream()
                .anyMatch(c -> c.getCode().equals(category));
        if (!known) {
            throw new IllegalArgumentException("알 수 없는 카테고리입니다: " + category);
        }
    }

    /**
     * 일괄 저장 payload의 카테고리 구조 반영.
     * code 없는 항목은 신규 생성(동명 inactive 재활성), payload에서 빠진 활성 카테고리는 비활성.
     * 표시 순서는 payload 순서가 authoritative. 기본 저축 카테고리(저축·투자)는 비활성 불가.
     *
     * @return payload 순서와 평행한 resolve된 code 목록 + 이번에 비활성 처리된 code 목록
     */
    @Transactional
    public StructureResult applyStructure(Long userId, List<SaveMonthlyCommand.CategoryCommand> payload) {
        Map<String, UserSpendingCategory> byCode = ensureSeeded(userId);
        SortCounter sort = new SortCounter(byCode.values());
        List<UserSpendingCategory> resolved = new ArrayList<>();
        for (SaveMonthlyCommand.CategoryCommand command : payload) {
            resolved.add(resolveOne(userId, command, byCode, sort));
        }
        reorderToPayload(resolved);
        List<String> resolvedCodes = resolved.stream()
                .map(UserSpendingCategory::getCode)
                .collect(Collectors.toList());
        List<String> deactivated = deactivateMissing(byCode, new HashSet<>(resolvedCodes));
        return new StructureResult(resolvedCodes, deactivated);
    }

    /** payload 순서를 sort_order로 반영 (변경된 것만 저장) */
    private void reorderToPayload(List<UserSpendingCategory> resolved) {
        for (int i = 0; i < resolved.size(); i++) {
            UserSpendingCategory cat = resolved.get(i);
            if (cat.getSortOrder() == i) continue;
            cat.updateSortOrder(i);
            categoryRepository.save(cat);
        }
    }

    /** 시드 보장 후 code → 카테고리 맵 (sort순 유지) */
    private Map<String, UserSpendingCategory> ensureSeeded(Long userId) {
        List<UserSpendingCategory> all = categoryRepository.findAllByUserId(userId);
        if (all.isEmpty()) {
            all = categoryRepository.saveAll(UserSpendingCategory.defaultSet(userId));
        }
        Map<String, UserSpendingCategory> byCode = new LinkedHashMap<>();
        all.forEach(c -> byCode.put(c.getCode(), c));
        return byCode;
    }

    private UserSpendingCategory resolveOne(Long userId, SaveMonthlyCommand.CategoryCommand command,
                                            Map<String, UserSpendingCategory> byCode, SortCounter sort) {
        if (command.getCategory() != null && !command.getCategory().isBlank()) {
            return resolveExisting(command, byCode, sort);
        }
        return resolveNew(userId, command, byCode, sort);
    }

    private UserSpendingCategory resolveExisting(SaveMonthlyCommand.CategoryCommand command,
                                                 Map<String, UserSpendingCategory> byCode, SortCounter sort) {
        UserSpendingCategory cat = byCode.get(command.getCategory());
        if (cat == null) {
            throw new IllegalArgumentException("알 수 없는 카테고리입니다: " + command.getCategory());
        }
        if (!cat.isActive()) {
            cat.reactivate(sort.next());
            categoryRepository.save(cat);
        }
        renameIfChanged(cat, command.getName());
        applySavingsIfChanged(cat, command.getSavings());
        return cat;
    }

    private UserSpendingCategory resolveNew(Long userId, SaveMonthlyCommand.CategoryCommand command,
                                            Map<String, UserSpendingCategory> byCode, SortCounter sort) {
        UserSpendingCategory inactiveSameName = findInactiveByName(byCode, command.getName());
        if (inactiveSameName != null) {
            inactiveSameName.reactivate(sort.next());
            categoryRepository.save(inactiveSameName);
            applySavingsIfChanged(inactiveSameName, command.getSavings());
            return inactiveSameName;
        }
        UserSpendingCategory created = categoryRepository.save(UserSpendingCategory.createCustom(
                userId, command.getName(), sort.next(), sort.seq(), Boolean.TRUE.equals(command.getSavings())));
        byCode.put(created.getCode(), created);
        return created;
    }

    private void renameIfChanged(UserSpendingCategory cat, String name) {
        if (cat.isSystem() || name == null || name.isBlank() || Objects.equals(cat.getName(), name.trim())) {
            return;
        }
        cat.rename(name);
        categoryRepository.save(cat);
    }

    /** 커스텀 카테고리의 저축률 산입 여부 반영 (null=미변경, system은 시드 값 고정) */
    private void applySavingsIfChanged(UserSpendingCategory cat, Boolean savings) {
        if (cat.isSystem() || savings == null || cat.isSavings() == savings) {
            return;
        }
        cat.updateSavings(savings);
        categoryRepository.save(cat);
    }

    private UserSpendingCategory findInactiveByName(Map<String, UserSpendingCategory> byCode, String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        return byCode.values().stream()
                .filter(c -> !c.isActive() && c.getName().equals(name.trim()))
                .findFirst().orElse(null);
    }

    private List<String> deactivateMissing(Map<String, UserSpendingCategory> byCode, Set<String> kept) {
        List<String> deactivated = new ArrayList<>();
        for (UserSpendingCategory cat : byCode.values()) {
            if (!cat.isActive() || kept.contains(cat.getCode())) continue;
            requireDeletable(cat);
            cat.deactivate();
            categoryRepository.save(cat);
            deactivated.add(cat.getCode());
        }
        return deactivated;
    }

    /** 기본 저축 카테고리(저축·투자)만 삭제 보호 — 커스텀 저축 카테고리는 삭제 가능 */
    private static void requireDeletable(UserSpendingCategory cat) {
        if (cat.isSystem() && cat.isSavings()) {
            throw new IllegalArgumentException("기본 저축 카테고리는 삭제할 수 없습니다: " + cat.getName());
        }
    }

    /** sort_order 발급기 — 기존 최댓값 다음부터 증가, 생성 seq는 코드 생성용 */
    private static class SortCounter {
        private int next;
        private long seq = 0;

        SortCounter(Iterable<UserSpendingCategory> existing) {
            int max = -1;
            for (UserSpendingCategory c : existing) {
                max = Math.max(max, c.getSortOrder());
            }
            this.next = max + 1;
        }

        int next() {
            return next++;
        }

        long seq() {
            return seq++;
        }
    }

    @Getter
    public static class StructureResult {
        private final List<String> resolvedCodes;
        private final List<String> deactivatedCodes;

        StructureResult(List<String> resolvedCodes, List<String> deactivatedCodes) {
            this.resolvedCodes = resolvedCodes;
            this.deactivatedCodes = deactivatedCodes;
        }
    }
}
