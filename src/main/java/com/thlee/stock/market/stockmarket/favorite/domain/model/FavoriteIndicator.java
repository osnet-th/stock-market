package com.thlee.stock.market.stockmarket.favorite.domain.model;

import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 관심 지표 도메인 모델
 */
@Getter
public class FavoriteIndicator {

    private final Long id;
    private final Long userId;
    private final FavoriteIndicatorSourceType sourceType;
    private final String indicatorCode;
    private final FavoriteDisplayMode displayMode;
    private final Integer priority;
    private final LocalDateTime createdAt;

    public FavoriteIndicator(Long id,
                             Long userId,
                             FavoriteIndicatorSourceType sourceType,
                             String indicatorCode,
                             FavoriteDisplayMode displayMode,
                             Integer priority,
                             LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.sourceType = sourceType;
        this.indicatorCode = indicatorCode;
        this.displayMode = displayMode != null ? displayMode : FavoriteDisplayMode.INDICATOR;
        this.priority = priority;
        this.createdAt = createdAt;
    }

    /**
     * 신규 관심지표 생성. priority는 SQL이 산출하므로 도메인에서는 null로 둔다.
     */
    public static FavoriteIndicator create(Long userId,
                                           FavoriteIndicatorSourceType sourceType,
                                           String indicatorCode) {
        return new FavoriteIndicator(null, userId, sourceType, indicatorCode,
                FavoriteDisplayMode.INDICATOR, null, LocalDateTime.now());
    }

    public FavoriteIndicator changeDisplayMode(FavoriteDisplayMode newDisplayMode) {
        return new FavoriteIndicator(id, userId, sourceType, indicatorCode, newDisplayMode, priority, createdAt);
    }

    public FavoriteIndicator withPriority(Integer newPriority) {
        return new FavoriteIndicator(id, userId, sourceType, indicatorCode, displayMode, newPriority, createdAt);
    }
}
