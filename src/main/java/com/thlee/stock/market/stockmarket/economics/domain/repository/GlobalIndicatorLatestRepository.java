package com.thlee.stock.market.stockmarket.economics.domain.repository;

import com.thlee.stock.market.stockmarket.economics.domain.model.GlobalIndicatorLatest;

import java.util.List;

/**
 * 글로벌 경제지표 최신값 저장소
 */
public interface GlobalIndicatorLatestRepository {

    /**
     * 전체 최신값 조회 (배치 시 1회 벌크 조회)
     */
    List<GlobalIndicatorLatest> findAll();

    /**
     * 최신값 일괄 UPSERT (countryName + indicatorType 기준)
     */
    void saveAll(List<GlobalIndicatorLatest> latestList);
}