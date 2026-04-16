package com.thlee.stock.market.stockmarket.favorite.presentation.dto;

import com.thlee.stock.market.stockmarket.favorite.domain.model.FavoriteIndicator;

public record FavoriteIndicatorResponse(
    String sourceType,
    String indicatorCode
) {
    public static FavoriteIndicatorResponse from(FavoriteIndicator favorite) {
        return new FavoriteIndicatorResponse(
            favorite.getSourceType().name(),
            favorite.getIndicatorCode()
        );
    }
}