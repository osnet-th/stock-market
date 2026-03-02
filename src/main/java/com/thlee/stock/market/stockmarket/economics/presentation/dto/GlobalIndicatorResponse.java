package com.thlee.stock.market.stockmarket.economics.presentation.dto;

import com.thlee.stock.market.stockmarket.economics.domain.model.CountryIndicatorSnapshot;
import com.thlee.stock.market.stockmarket.economics.domain.model.GlobalEconomicIndicatorType;

import java.util.List;

public record GlobalIndicatorResponse(
    String indicatorType,
    String displayName,
    String category,
    int count,
    List<CountryIndicatorRowResponse> countries
) {

    public static GlobalIndicatorResponse of(
            GlobalEconomicIndicatorType type,
            List<CountryIndicatorSnapshot> snapshots) {
        List<CountryIndicatorRowResponse> rows = snapshots.stream()
            .map(CountryIndicatorRowResponse::from)
            .toList();

        return new GlobalIndicatorResponse(
            type.name(),
            type.getDisplayName(),
            type.getCategory().getDisplayName(),
            rows.size(),
            rows
        );
    }
}