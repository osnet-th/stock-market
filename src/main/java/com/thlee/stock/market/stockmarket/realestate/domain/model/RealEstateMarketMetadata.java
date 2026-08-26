package com.thlee.stock.market.stockmarket.realestate.domain.model;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 부동산 시장 지표 메타데이터.
 * <p>
 * PK: (category, source, indicatorCode)
 */
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RealEstateMarketMetadata {

    private RealEstateMarketCategory category;
    private RealEstateMarketSource source;
    private String indicatorCode;
    private String displayName;
    private String description;
    private String unit;
    private boolean defaultVisible;
}
