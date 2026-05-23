package com.thlee.stock.market.stockmarket.realestate.domain.repository;

import com.thlee.stock.market.stockmarket.realestate.domain.model.RealEstateMarketCategory;
import com.thlee.stock.market.stockmarket.realestate.domain.model.RealEstateMarketIndicator;
import com.thlee.stock.market.stockmarket.realestate.domain.model.RealEstateMarketSource;

import java.util.List;

/**
 * 부동산 시장 지표 히스토리 저장소.
 */
public interface RealEstateMarketIndicatorRepository {

    List<RealEstateMarketIndicator> saveAll(List<RealEstateMarketIndicator> indicators);

    /**
     * (regionCode, category, source, indicatorCode) 기준 최근 limit개 시계열을 snapshotDate 오름차순으로 조회.
     */
    List<RealEstateMarketIndicator> findHistory(String regionCode,
                                                RealEstateMarketCategory category,
                                                RealEstateMarketSource source,
                                                String indicatorCode,
                                                int limit);

    /**
     * regionCode + category 범위에서 출처별 최신 시점 1건씩 조회.
     */
    List<RealEstateMarketIndicator> findLatestByRegionAndCategory(String regionCode,
                                                                  RealEstateMarketCategory category);
}
