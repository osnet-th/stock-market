package com.thlee.stock.market.stockmarket.economics.derivedindicator.presentation.dto;

import com.thlee.stock.market.stockmarket.economics.derivedindicator.application.dto.AvailableIndicator;

/**
 * 수식 생성 폼에서 선택 가능한 원시 지표 응답.
 */
public record AvailableIndicatorResponse(String className, String keystatName, String category, String categoryLabel) {
    public static AvailableIndicatorResponse from(AvailableIndicator indicator) {
        return new AvailableIndicatorResponse(
                indicator.className(),
                indicator.keystatName(),
                indicator.category().name(),
                indicator.category().getLabel()
        );
    }
}
