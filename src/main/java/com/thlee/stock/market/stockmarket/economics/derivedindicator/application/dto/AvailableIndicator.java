package com.thlee.stock.market.stockmarket.economics.derivedindicator.application.dto;

import com.thlee.stock.market.stockmarket.economics.domain.model.EcosIndicatorCategory;

/**
 * 수식 생성 폼에서 선택 가능한 원시 지표. 카테고리 동반(같은 카테고리 조합 가이드용).
 */
public record AvailableIndicator(String className, String keystatName, EcosIndicatorCategory category) {
}
