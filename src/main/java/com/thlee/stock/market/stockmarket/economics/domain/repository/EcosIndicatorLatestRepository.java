package com.thlee.stock.market.stockmarket.economics.domain.repository;

import com.thlee.stock.market.stockmarket.economics.domain.model.EcosIndicatorLatest;

import java.util.List;
import java.util.Optional;

/**
 * ECOS 경제지표 최신값 저장소
 */
public interface EcosIndicatorLatestRepository {

    /**
     * 전체 최신값 조회 (배치 시 1회 벌크 조회)
     */
    List<EcosIndicatorLatest> findAll();

    /**
     * className + keystatName으로 단건 조회
     */
    Optional<EcosIndicatorLatest> findByClassNameAndKeystatName(String className, String keystatName);

    /**
     * 최신값 일괄 UPSERT (className + keystatName 기준)
     */
    void saveAll(List<EcosIndicatorLatest> latestList);

    /**
     * 최근 업데이트된 지표 조회 (updatedAt 기준, 최신순)
     */
    List<EcosIndicatorLatest> findRecentlyUpdated(java.time.LocalDateTime after);
}
