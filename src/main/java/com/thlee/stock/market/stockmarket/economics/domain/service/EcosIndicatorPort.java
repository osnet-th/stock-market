package com.thlee.stock.market.stockmarket.economics.domain.service;

import com.thlee.stock.market.stockmarket.economics.domain.model.EcosKeyStatResult;

/**
 * ECOS 경제지표 조회 포트
 */
public interface EcosIndicatorPort {
    EcosKeyStatResult fetchKeyStatistics();
}