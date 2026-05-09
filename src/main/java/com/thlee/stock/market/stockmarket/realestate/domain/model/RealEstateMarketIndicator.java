package com.thlee.stock.market.stockmarket.realestate.domain.model;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 부동산 시장 지표 히스토리 도메인 모델.
 * <p>
 * 단일 history 테이블에 카테고리 enum + payload(JSONB)로 다양한 데이터 형태를 흡수한다.
 * pkKey = (regionCode, category, source, indicatorCode) — Latest unique key와 동일.
 */
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RealEstateMarketIndicator {

    private Long id;
    private String regionCode;
    private RealEstateMarketCategory category;
    private RealEstateMarketSource source;
    private String indicatorCode;
    private String referenceText;
    private BigDecimal value;
    private String payload;
    private String sourceUrl;
    private String cycle;
    private LocalDateTime snapshotDate;
    private LocalDateTime createdAt;

    public IndicatorKey pkKey() {
        return new IndicatorKey(regionCode, category, source, indicatorCode);
    }
}
