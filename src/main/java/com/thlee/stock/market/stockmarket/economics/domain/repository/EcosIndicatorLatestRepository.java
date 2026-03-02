package com.thlee.stock.market.stockmarket.economics.domain.repository;

import com.thlee.stock.market.stockmarket.economics.domain.model.EcosIndicatorLatest;

import java.util.List;

/**
 * ECOS 경제지표 최신값 저장소
 */
public interface EcosIndicatorLatestRepository {

    /**
     * 전체 최신값 조회 (배치 시 1회 벌크 조회)
     */
    List<EcosIndicatorLatest> findAll();

    /**
     * 최신값 일괄 UPSERT (className + keystatName 기준)
     */
    void saveAll(List<EcosIndicatorLatest> latestList);
}
