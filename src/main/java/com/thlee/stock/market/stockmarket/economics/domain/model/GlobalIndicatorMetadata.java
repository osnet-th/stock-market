package com.thlee.stock.market.stockmarket.economics.domain.model;

import lombok.Getter;

/**
 * 글로벌 경제지표 메타데이터 도메인 모델
 */
@Getter
public class GlobalIndicatorMetadata {

    private final GlobalEconomicIndicatorType indicatorType;
    private final String description;

    public GlobalIndicatorMetadata(GlobalEconomicIndicatorType indicatorType,
                                    String description) {
        this.indicatorType = indicatorType;
        this.description = description;
    }
}