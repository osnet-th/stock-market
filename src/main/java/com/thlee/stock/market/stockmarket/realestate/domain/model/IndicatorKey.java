package com.thlee.stock.market.stockmarket.realestate.domain.model;

/**
 * 부동산 지표 PK 식별자.
 * <p>
 * (regionCode, category, source, indicatorCode) 4-튜플로 동일 지표를 식별한다.
 * referenceText는 row 단위 메타이므로 PK에 포함하지 않는다.
 * <p>
 * Map 키로 사용 시 record의 equals/hashCode 자동 생성에 의존한다.
 */
public record IndicatorKey(
        String regionCode,
        RealEstateMarketCategory category,
        RealEstateMarketSource source,
        String indicatorCode
) {
}
