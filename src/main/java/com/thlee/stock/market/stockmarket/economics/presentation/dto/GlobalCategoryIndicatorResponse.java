package com.thlee.stock.market.stockmarket.economics.presentation.dto;

import com.thlee.stock.market.stockmarket.economics.domain.model.CountryIndicatorSnapshot;
import com.thlee.stock.market.stockmarket.economics.domain.model.GlobalEconomicIndicatorType;
import com.thlee.stock.market.stockmarket.economics.domain.model.IndicatorCategory;

import java.util.List;
import java.util.Map;

/**
 * 카테고리별 전체 지표 데이터 응답 DTO
 */
public record GlobalCategoryIndicatorResponse(
    String categoryKey,
    String categoryDisplayName,
    List<GlobalIndicatorResponse> indicators
) {

    public static GlobalCategoryIndicatorResponse of(
            IndicatorCategory category,
            Map<GlobalEconomicIndicatorType, List<CountryIndicatorSnapshot>> snapshotsByType) {

        List<GlobalIndicatorResponse> indicators = snapshotsByType.entrySet().stream()
            .map(entry -> GlobalIndicatorResponse.of(entry.getKey(), entry.getValue()))
            .toList();

        return new GlobalCategoryIndicatorResponse(
            category.name(),
            category.getDisplayName(),
            indicators
        );
    }
}