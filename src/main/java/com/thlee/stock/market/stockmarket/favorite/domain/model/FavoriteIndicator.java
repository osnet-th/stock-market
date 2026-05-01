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
    private final LocalDateTime createdAt;

    public FavoriteIndicator(Long id,
                             Long userId,
                             FavoriteIndicatorSourceType sourceType,
                             String indicatorCode,
                             FavoriteDisplayMode displayMode,
                             LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.sourceType = sourceType;
        this.indicatorCode = indicatorCode;
        this.displayMode = displayMode != null ? displayMode : FavoriteDisplayMode.INDICATOR;
        this.createdAt = createdAt;
    }

    public static FavoriteIndicator create(Long userId,
                                           FavoriteIndicatorSourceType sourceType,
                                           String indicatorCode) {
        return new FavoriteIndicator(null, userId, sourceType, indicatorCode,
                FavoriteDisplayMode.INDICATOR, LocalDateTime.now());
    }

    public FavoriteIndicator changeDisplayMode(FavoriteDisplayMode newDisplayMode) {
        return new FavoriteIndicator(id, userId, sourceType, indicatorCode, newDisplayMode, createdAt);
    }
}
