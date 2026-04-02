package com.thlee.stock.market.stockmarket.economics.domain.repository;

import com.thlee.stock.market.stockmarket.economics.domain.model.EcosIndicator;

import java.util.List;
import java.util.Set;

/**
 * ECOS 경제지표 히스토리 저장소
 */
public interface EcosIndicatorRepository {

    /**
     * 경제지표 목록 일괄 저장
     */
    List<EcosIndicator> saveAll(List<EcosIndicator> indicators);

    /**
     * 히스토리 데이터 존재 여부 확인
     */
    boolean existsAny();

    /**
     * className 집합에 해당하는 지표들의 히스토리 조회
     * 동일 (className, keystatName, cycle) 조합에 대해 최신 snapshotDate 기준 1건만 반환
     */
    List<EcosIndicator> findLatestHistoryByClassNames(Set<String> classNames);
}
