package com.thlee.stock.market.stockmarket.economics.domain.repository;

import com.thlee.stock.market.stockmarket.economics.domain.model.GlobalEconomicIndicatorType;
import com.thlee.stock.market.stockmarket.economics.domain.model.GlobalIndicator;

import java.util.List;

/**
 * 글로벌 경제지표 히스토리 저장소
 */
public interface GlobalIndicatorRepository {

    /**
     * 경제지표 목록 일괄 저장
     */
    List<GlobalIndicator> saveAll(List<GlobalIndicator> indicators);

    /**
     * 히스토리 데이터 존재 여부 확인
     */
    boolean existsAny();

    /**
     * 지표타입별 히스토리 조회 (cycle별 최신 snapshot만 반환)
     */
    List<GlobalIndicator> findLatestHistoryByIndicatorType(GlobalEconomicIndicatorType indicatorType);
}