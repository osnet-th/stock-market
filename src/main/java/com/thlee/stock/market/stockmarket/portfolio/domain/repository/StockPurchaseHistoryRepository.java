package com.thlee.stock.market.stockmarket.portfolio.domain.repository;

import com.thlee.stock.market.stockmarket.portfolio.domain.model.StockPurchaseHistory;

import java.util.List;
import java.util.Optional;

public interface StockPurchaseHistoryRepository {
    StockPurchaseHistory save(StockPurchaseHistory history);
    Optional<StockPurchaseHistory> findById(Long id);
    List<StockPurchaseHistory> findByPortfolioItemId(Long portfolioItemId);

    /** 여러 항목의 매수 이력을 한 번에 조회 (요약 집계의 N+1 방지) */
    List<StockPurchaseHistory> findByPortfolioItemIdIn(List<Long> portfolioItemIds);
    void delete(StockPurchaseHistory history);
    void deleteByPortfolioItemId(Long portfolioItemId);
}