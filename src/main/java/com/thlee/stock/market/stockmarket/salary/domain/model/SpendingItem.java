package com.thlee.stock.market.stockmarket.salary.domain.model;

import com.thlee.stock.market.stockmarket.salary.domain.model.enums.SpendingCategory;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * 카테고리 하위 지출 항목. {@link SpendingItemSet}에 속한 값이며 세트 단위로 스냅샷된다.
 *
 * <p>이름은 빈 문자열을 허용한다(화면에서 이름 미입력 상태로 편집 중 저장 가능).
 */
@Getter
public class SpendingItem {

    public static final int NAME_MAX_LENGTH = 100;

    private final Long id;
    private final SpendingCategory category;
    private final String name;
    private final BigDecimal amount;
    private final boolean fixed;
    private final int sortOrder;

    /** 재구성용 생성자 (Repository 조회 시) */
    public SpendingItem(Long id, SpendingCategory category, String name,
                        BigDecimal amount, boolean fixed, int sortOrder) {
        this.id = id;
        this.category = category;
        this.name = name;
        this.amount = amount;
        this.fixed = fixed;
        this.sortOrder = sortOrder;
    }

    /** 새 항목 생성 (세트 저장 시 통째로 재생성된다) */
    public static SpendingItem create(SpendingCategory category, String name,
                                      BigDecimal amount, boolean fixed, int sortOrder) {
        validateCategory(category);
        validateAmount(amount);
        return new SpendingItem(null, category, normalizeName(name), amount, fixed, sortOrder);
    }

    /** 세트 NOOP 판정용 동등 비교 — 내용(카테고리·이름·금액·고정 여부)만 비교한다. */
    public boolean isSameAs(SpendingItem other) {
        if (other == null || this.category != other.category || this.fixed != other.fixed) {
            return false;
        }
        return this.name.equals(other.name) && this.amount.compareTo(other.amount) == 0;
    }

    private static String normalizeName(String name) {
        String normalized = name == null ? "" : name.trim();
        if (normalized.length() > NAME_MAX_LENGTH) {
            throw new IllegalArgumentException("항목 이름은 " + NAME_MAX_LENGTH + "자 이내여야 합니다.");
        }
        return normalized;
    }

    private static void validateCategory(SpendingCategory category) {
        if (category == null) {
            throw new IllegalArgumentException("항목 카테고리는 필수입니다.");
        }
    }

    private static void validateAmount(BigDecimal amount) {
        if (amount == null || amount.signum() < 0) {
            throw new IllegalArgumentException("항목 금액은 0 이상이어야 합니다.");
        }
    }
}
