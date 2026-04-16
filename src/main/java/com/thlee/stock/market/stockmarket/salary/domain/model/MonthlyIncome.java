package com.thlee.stock.market.stockmarket.salary.domain.model;

import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;

/**
 * 월급 변경 이력 (변경 지점 기반 Effective Date 모델).
 *
 * <p>effectiveFromMonth 이후 더 최신 레코드가 없으면 계속 유효하다.
 */
@Getter
public class MonthlyIncome {

    /** 저축율 등 비율 계산용 scale / 반올림 정책 */
    public static final int RATIO_SCALE = 4;
    public static final RoundingMode RATIO_ROUNDING = RoundingMode.HALF_UP;

    private Long id;
    private Long userId;
    private YearMonth effectiveFromMonth;
    private BigDecimal amount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** 재구성용 생성자 (Repository 조회 시) */
    public MonthlyIncome(Long id, Long userId, YearMonth effectiveFromMonth,
                         BigDecimal amount, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.effectiveFromMonth = effectiveFromMonth;
        this.amount = amount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * 새 월급 변경 레코드 생성.
     *
     * @param referenceMonth 현재 월 판정 기준 (테스트 가능성 목적으로 주입)
     */
    public static MonthlyIncome create(Long userId, YearMonth effectiveFromMonth,
                                       YearMonth referenceMonth, BigDecimal amount) {
        validateUserId(userId);
        validateAmount(amount);
        requireNotFuture(effectiveFromMonth, referenceMonth);
        LocalDateTime now = LocalDateTime.now();
        return new MonthlyIncome(null, userId, effectiveFromMonth, amount, now, now);
    }

    /** 금액 수정 */
    public void updateAmount(BigDecimal amount) {
        validateAmount(amount);
        this.amount = amount;
        this.updatedAt = LocalDateTime.now();
    }

    /** upsert noop 판정 — 상속값과 새 값이 같은지 */
    public boolean isSameAmountAs(BigDecimal other) {
        return other != null && this.amount.compareTo(other) == 0;
    }

    /**
     * 저축율 계산: savingsAmount / amount.
     *
     * @return income이 0이거나 savingsAmount가 null이면 {@code null},
     *         아니면 scale=4, HALF_UP의 BigDecimal
     */
    public BigDecimal calculateSavingsRatio(BigDecimal savingsAmount) {
        if (savingsAmount == null || this.amount.signum() == 0) {
            return null;
        }
        return savingsAmount.divide(this.amount, RATIO_SCALE, RATIO_ROUNDING);
    }

    private static void validateUserId(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId는 필수입니다.");
        }
    }

    private static void validateAmount(BigDecimal amount) {
        if (amount == null || amount.signum() < 0) {
            throw new IllegalArgumentException("월급 금액은 0 이상이어야 합니다.");
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
