package com.thlee.stock.market.stockmarket.salary.domain.model;

import com.thlee.stock.market.stockmarket.salary.domain.model.enums.SpendingCategory;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Objects;

/**
 * 카테고리별 지출 변경 이력 (변경 지점 기반 Effective Date 모델).
 */
@Getter
public class SpendingConfig {

    private Long id;
    private Long userId;
    private SpendingCategory category;
    private YearMonth effectiveFromMonth;
    private BigDecimal amount;
    private String memo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** 재구성용 생성자 (Repository 조회 시) */
    public SpendingConfig(Long id, Long userId, SpendingCategory category,
                          YearMonth effectiveFromMonth, BigDecimal amount, String memo,
                          LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.category = category;
        this.effectiveFromMonth = effectiveFromMonth;
        this.amount = amount;
        this.memo = memo;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * 새 지출 변경 레코드 생성.
     *
     * @param referenceMonth 현재 월 판정 기준 (테스트 가능성 목적으로 주입)
     */
    public static SpendingConfig create(Long userId, SpendingCategory category,
                                        YearMonth effectiveFromMonth, YearMonth referenceMonth,
                                        BigDecimal amount, String memo) {
        validateUserId(userId);
        validateCategory(category);
        validateAmount(amount);
        requireNotFuture(effectiveFromMonth, referenceMonth);
        LocalDateTime now = LocalDateTime.now();
        return new SpendingConfig(null, userId, category, effectiveFromMonth, amount,
                                  normalizeMemo(memo), now, now);
    }

    /** 금액 및 메모 수정 */
    public void updateAmountAndMemo(BigDecimal amount, String memo) {
        validateAmount(amount);
        this.amount = amount;
        this.memo = normalizeMemo(memo);
        this.updatedAt = LocalDateTime.now();
    }

    /** upsert noop 판정 — 금액과 메모가 모두 같아야 동일로 취급 */
    public boolean isSameAs(BigDecimal otherAmount, String otherMemo) {
        if (otherAmount == null || this.amount.compareTo(otherAmount) != 0) {
            return false;
        }
        return Objects.equals(this.memo, normalizeMemo(otherMemo));
    }

    private static String normalizeMemo(String memo) {
        if (memo == null) {
            return null;
        }
        String trimmed = memo.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static void validateUserId(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId는 필수입니다.");
        }
    }

    private static void validateCategory(SpendingCategory category) {
        if (category == null) {
            throw new IllegalArgumentException("카테고리는 필수입니다.");
        }
    }

    private static void validateAmount(BigDecimal amount) {
        if (amount == null || amount.signum() < 0) {
            throw new IllegalArgumentException("지출 금액은 0 이상이어야 합니다.");
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