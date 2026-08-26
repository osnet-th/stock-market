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

    /**
     * 최근 업데이트된 지표 조회 (updatedAt 기준, 최신순)
     */
    List<GlobalIndicatorLatest> findRecentlyUpdated(java.time.LocalDateTime after);

    /**
     * 전체 지표 중 가장 최근 수집 시각 (#51 catch-up 판단용, 수집 이력 없으면 empty)
     */
    java.util.Optional<java.time.LocalDateTime> findMaxLastCollectedAt();
}