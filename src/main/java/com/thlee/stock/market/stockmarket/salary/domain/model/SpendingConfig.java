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

    /** 카테고리 월 예산. null이면 예산 미설정 (0과 동일하게 "예산 없음"으로 취급). */
    private BigDecimal budget;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** 재구성용 생성자 (Repository 조회 시) */
    public SpendingConfig(Long id, Long userId, SpendingCategory category,
                          YearMonth effectiveFromMonth, BigDecimal amount, String memo,
                          BigDecimal budget, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.category = category;
        this.effectiveFromMonth = effectiveFromMonth;
        this.amount = amount;
        this.memo = memo;
        this.budget = budget;
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
                                        BigDecimal amount, String memo, BigDecimal budget) {
        validateUserId(userId);
        validateCategory(category);
        validateAmount(amount);
        validateBudget(budget);
        requireNotFuture(effectiveFromMonth, referenceMonth);
        LocalDateTime now = LocalDateTime.now();
        return new SpendingConfig(null, userId, category, effectiveFromMonth, amount,
                                  normalizeMemo(memo), budget, now, now);
    }

    /** 금액 및 메모 수정 */
    public void updateAmountAndMemo(BigDecimal amount, String memo) {
        validateAmount(amount);
        this.amount = amount;
        this.memo = normalizeMemo(memo);
        this.updatedAt = LocalDateTime.now();
    }

    /** 금액 및 예산 수정 (일괄 저장 경로 — 메모는 보존) */
    public void updateAmountAndBudget(BigDecimal amount, BigDecimal budget) {
        validateAmount(amount);
        validateBudget(budget);
        this.amount = amount;
        this.budget = budget;
        this.updatedAt = LocalDateTime.now();
    }

    /** upsert noop 판정 — 금액과 메모가 모두 같아야 동일로 취급 */
    public boolean isSameAs(BigDecimal otherAmount, String otherMemo) {
        if (otherAmount == null || this.amount.compareTo(otherAmount) != 0) {
            return false;
        }
        return Objects.equals(this.memo, normalizeMemo(otherMemo));
    }

    /** 일괄 저장 noop 판정 — 금액과 예산이 모두 같아야 동일로 취급 (null 예산은 0과 동일) */
    public boolean isSameAmountAndBudgetAs(BigDecimal otherAmount, BigDecimal otherBudget) {
        if (otherAmount == null || this.amount.compareTo(otherAmount) != 0) {
            return false;
        }
        return normalizeBudget(this.budget).compareTo(normalizeBudget(otherBudget)) == 0;
    }

    private static BigDecimal normalizeBudget(BigDecimal budget) {
        return budget == null ? BigDecimal.ZERO : budget;
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

    private static void validateBudget(BigDecimal budget) {
        if (budget != null && budget.signum() < 0) {
            throw new IllegalArgumentException("예산은 0 이상이어야 합니다.");
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