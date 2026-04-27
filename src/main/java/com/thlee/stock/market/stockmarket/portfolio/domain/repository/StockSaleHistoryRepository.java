package com.thlee.stock.market.stockmarket.portfolio.domain.repository;

import com.thlee.stock.market.stockmarket.portfolio.domain.model.StockSaleHistory;

import java.util.List;
import java.util.Optional;

public interface StockSaleHistoryRepository {

    StockSaleHistory save(StockSaleHistory history);

    Optional<StockSaleHistory> findById(Long id);

    /**
     * 특정 PortfolioItem의 매도 이력 (오래된 순)
     */
    List<StockSaleHistory> findByPortfolioItemId(Long portfolioItemId);

    /**
     * 여러 PortfolioItem의 매도 이력 일괄 조회 (배치 조회로 N+1 회피)
     */
    List<StockSaleHistory> findByPortfolioItemIdIn(List<Long> portfolioItemIds);

    /**
     * 사용자 단위 매도 이력 (매도일 내림차순) — PortfolioItem과 ID 조인
     */
    List<StockSaleHistory> findByUserId(Long userId);

    void delete(StockSaleHistory history);

    void deleteByPortfolioItemId(Long portfolioItemId);
}