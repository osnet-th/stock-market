package com.thlee.stock.market.stockmarket.economics.presentation.dto;

import com.thlee.stock.market.stockmarket.economics.domain.model.EcosIndicatorCategory;

/**
 * 카테고리 응답 DTO
 */
public record CategoryResponse(
    String name,
    String label
) {

    public static CategoryResponse from(EcosIndicatorCategory category) {
        return new CategoryResponse(
            category.name(),
            category.getLabel()
        );
    }
}
