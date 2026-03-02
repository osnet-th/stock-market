package com.thlee.stock.market.stockmarket.economics.domain.model;

import java.util.List;

/**
 * ECOS 100대 경제지표 조회 결과
 */
public record EcosKeyStatResult(
    int totalCount,
    List<KeyStatIndicator> indicators
) {

    /**
     * 유효한 카테고리에 매핑되는 지표만 필터링
     */
    public List<KeyStatIndicator> validIndicators() {
        return indicators.stream()
            .filter(indicator -> EcosIndicatorCategory.fromClassName(indicator.className()) != null)
            .toList();
    }
}