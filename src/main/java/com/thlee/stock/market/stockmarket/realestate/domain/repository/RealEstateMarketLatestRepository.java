package com.thlee.stock.market.stockmarket.realestate.domain.repository;

import com.thlee.stock.market.stockmarket.realestate.domain.model.RealEstateMarketCategory;
import com.thlee.stock.market.stockmarket.realestate.domain.model.RealEstateMarketLatest;
import com.thlee.stock.market.stockmarket.realestate.domain.model.RealEstateMarketSource;

import java.util.List;
import java.util.Optional;

/**
 * 부동산 시장 지표 최신값 저장소.
 */
public interface RealEstateMarketLatestRepository {

    /**
     * Source 단위까지 좁힌 1회 조회 — pkKey 기반 변경 감지 비교에 사용.
     */
    List<RealEstateMarketLatest> findAllByRegionAndCategoryAndSource(String regionCode,
                                                                     RealEstateMarketCategory category,
                                                                     RealEstateMarketSource source);

    Optional<RealEstateMarketLatest> findByKey(String regionCode,
                                               RealEstateMarketCategory category,
                                               RealEstateMarketSource source,
                                               String indicatorCode);

    void upsertAll(List<RealEstateMarketLatest> latests);

    /**
     * 사용자 응답에 노출되는 출처별 마지막 갱신 시각.
     */
    List<RealEstateMarketLatest> findAll();

    long count();
}
