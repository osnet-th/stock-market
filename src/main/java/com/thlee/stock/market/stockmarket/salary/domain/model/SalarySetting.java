package com.thlee.stock.market.stockmarket.salary.domain.model;

import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 사용자별 월급 화면 설정. 현재는 저축률 목표(%)만 보유한다.
 */
@Getter
public class SalarySetting {

    public static final int DEFAULT_SAVING_TARGET = 50;
    public static final int TARGET_MIN = 0;
    public static final int TARGET_MAX = 100;

    private Long id;
    private Long userId;
    private int savingTargetPct;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** 재구성용 생성자 (Repository 조회 시) */
    public SalarySetting(Long id, Long userId, int savingTargetPct,
                         LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.savingTargetPct = savingTargetPct;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static SalarySetting create(Long userId, int savingTargetPct) {
        validateUserId(userId);
        validateTarget(savingTargetPct);
        LocalDateTime now = LocalDateTime.now();
        return new SalarySetting(null, userId, savingTargetPct, now, now);
    }

    public void updateSavingTarget(int savingTargetPct) {
        validateTarget(savingTargetPct);
        this.savingTargetPct = savingTargetPct;
        this.updatedAt = LocalDateTime.now();
    }

    private static void validateUserId(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId는 필수입니다.");
        }
    }

    private static void validateTarget(int target) {
        if (target < TARGET_MIN || target > TARGET_MAX) {
            throw new IllegalArgumentException(
                    "저축률 목표는 " + TARGET_MIN + " 이상 " + TARGET_MAX + " 이하여야 합니다.");
        }
    }
}
