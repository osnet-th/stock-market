package com.thlee.stock.market.stockmarket.favorite.presentation.dto;

import com.thlee.stock.market.stockmarket.economics.domain.model.GlobalEconomicIndicatorType;
import com.thlee.stock.market.stockmarket.favorite.application.FavoriteIndicatorService.EnrichedGlobalFavorite;
import com.thlee.stock.market.stockmarket.favorite.presentation.dto.EnrichedFavoriteResponse.GlobalItem;

import java.util.List;

/**
 * 단일 indicatorType 재조회 응답.
 * 해당 indicatorType 을 관심 등록한 사용자의 카드들만 반환한다.
 */
public record GlobalRefreshResponse(
    String indicatorType,
    List<GlobalItem> items
) {
    public static GlobalRefreshResponse of(GlobalEconomicIndicatorType indicatorType,
                                           List<EnrichedGlobalFavorite> enriched) {
        List<GlobalItem> items = enriched.stream()
            .map(GlobalItem::from)
            .toList();
        return new GlobalRefreshResponse(indicatorType.name(), items);
    }
}