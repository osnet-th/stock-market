package com.thlee.stock.market.stockmarket.realestate.domain.model;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 부동산 시장 지표 최신값 (변경 감지 비교용).
 * <p>
 * Unique key: (regionCode, category, source, indicatorCode)
 */
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RealEstateMarketLatest {

    private String regionCode;
    private RealEstateMarketCategory category;
    private RealEstateMarketSource source;
    private String indicatorCode;
    private String referenceText;
    private BigDecimal value;
    private String payload;
    private String previousReferenceText;
    private LocalDateTime updatedAt;

    public IndicatorKey pkKey() {
        return new IndicatorKey(regionCode, category, source, indicatorCode);
    }
}
