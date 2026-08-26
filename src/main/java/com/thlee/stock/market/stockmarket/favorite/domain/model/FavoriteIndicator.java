package com.thlee.stock.market.stockmarket.favorite.domain.model;

import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 관심 지표 도메인 모델
 *
 * <p>표시 모드(INDICATOR/GRAPH)는 #114 에서 폐지됐다. 모든 카드가 스파크라인 한 형태로 통일되며
 * 순서 컨테이너도 (userId, sourceType) 하나로 단순해졌다.
 */
@Getter
public class FavoriteIndicator {

    private final Long id;
    private final Long userId;
    private final FavoriteIndicatorSourceType sourceType;
    private final String indicatorCode;
    private final Integer priority;
    private final LocalDateTime createdAt;

    public FavoriteIndicator(Long id,
                             Long userId,
                             FavoriteIndicatorSourceType sourceType,
                             String indicatorCode,
                             Integer priority,
                             LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.sourceType = sourceType;
        this.indicatorCode = indicatorCode;
        this.priority = priority;
        this.createdAt = createdAt;
    }

    /**
     * 신규 관심지표 생성. priority는 SQL이 산출하므로 도메인에서는 null로 둔다.
     */
    public static FavoriteIndicator create(Long userId,
                                           FavoriteIndicatorSourceType sourceType,
                                           String indicatorCode) {
        return new FavoriteIndicator(null, userId, sourceType, indicatorCode, null, LocalDateTime.now());
    }

    public FavoriteIndicator withPriority(Integer newPriority) {
        return new FavoriteIndicator(id, userId, sourceType, indicatorCode, newPriority, createdAt);
    }
}
