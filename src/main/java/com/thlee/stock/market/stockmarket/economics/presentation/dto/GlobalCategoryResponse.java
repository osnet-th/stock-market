package com.thlee.stock.market.stockmarket.economics.presentation.dto;

import com.thlee.stock.market.stockmarket.economics.domain.model.GlobalEconomicIndicatorType;
import com.thlee.stock.market.stockmarket.economics.domain.model.IndicatorCategory;

import java.util.Arrays;
import java.util.List;

/**
 * 글로벌 지표 카테고리 응답 DTO
 */
public record GlobalCategoryResponse(
    String key,
    String displayName,
    List<GlobalIndicatorSummaryResponse> indicators
) {

    public static List<GlobalCategoryResponse> fromAll() {
        return Arrays.stream(IndicatorCategory.values())
            .map(GlobalCategoryResponse::from)
            .toList();
    }

    private static GlobalCategoryResponse from(IndicatorCategory category) {
        List<GlobalIndicatorSummaryResponse> indicators = Arrays.stream(GlobalEconomicIndicatorType.values())
            .filter(type -> type.getCategory() == category)
            .map(type -> new GlobalIndicatorSummaryResponse(type.name(), type.getDisplayName()))
            .toList();

        return new GlobalCategoryResponse(
            category.name(),
            category.getDisplayName(),
            indicators
        );
    }
}