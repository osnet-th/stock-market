package com.thlee.stock.market.stockmarket.salary.domain.model;

import lombok.Getter;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 월 단위 하위 항목 스냅샷 세트 (변경 지점 기반 Effective Date 모델).
 *
 * <p>한 세트는 해당 월 이후의 <b>전체 카테고리 항목 구성</b>을 나타낸다.
 * {@code effectiveFromMonth <= 대상월}인 최신 세트가 유효하며,
 * 빈 세트는 "이 달부터 항목 없음"을 명시적으로 표현한다.
 */
@Getter
public class SpendingItemSet {

    private Long id;
    private Long userId;
    private YearMonth effectiveFromMonth;
    private List<SpendingItem> items;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** 재구성용 생성자 (Repository 조회 시) */
    public SpendingItemSet(Long id, Long userId, YearMonth effectiveFromMonth,
                           List<SpendingItem> items, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.effectiveFromMonth = effectiveFromMonth;
        this.items = List.copyOf(items);
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * 새 항목 세트 생성.
     *
     * @param referenceMonth 현재 월 판정 기준 (테스트 가능성 목적으로 주입)
     */
    public static SpendingItemSet create(Long userId, YearMonth effectiveFromMonth,
                                         YearMonth referenceMonth, List<SpendingItem> items) {
        validateUserId(userId);
        validateItems(items);
        requireNotFuture(effectiveFromMonth, referenceMonth);
        LocalDateTime now = LocalDateTime.now();
        return new SpendingItemSet(null, userId, effectiveFromMonth, items, now, now);
    }

    /** 항목 전체 교체 (해당 월 세트 재저장 시) */
    public void replaceItems(List<SpendingItem> items) {
        validateItems(items);
        this.items = List.copyOf(items);
        this.updatedAt = LocalDateTime.now();
    }

    /** upsert NOOP 판정 — 순서 포함 내용 동일 여부 */
    public boolean hasSameItemsAs(List<SpendingItem> others) {
        if (others == null || this.items.size() != others.size()) {
            return false;
        }
        for (int i = 0; i < this.items.size(); i++) {
            if (!this.items.get(i).isSameAs(others.get(i))) return false;
        }
        return true;
    }

    /** 특정 카테고리의 항목 (세트 내 순서 유지) */
    public List<SpendingItem> itemsOf(String category) {
        return items.stream()
                .filter(item -> item.getCategory().equals(category))
                .collect(Collectors.toList());
    }

    private static void validateUserId(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId는 필수입니다.");
        }
    }

    private static void validateItems(List<SpendingItem> items) {
        if (items == null) {
            throw new IllegalArgumentException("항목 목록은 null일 수 없습니다.");
        }
    }

    private static void requireNotFuture(YearMonth effectiveFromMonth, YearMonth referenceMonth) {
        if (effectiveFromMonth == null) {
            throw new IllegalArgumentException("effectiveFromMonth는 필수입니다.");
        }
        if (referenceMonth == null) {
            throw new IllegalArgumentException("referenceMonth는 필수입니다.");
        }
        if (effectiveFromMonth.isAfter(referenceMonth)) {
            throw new IllegalArgumentException("미래 월은 입력할 수 없습니다.");
        }
    }
}
