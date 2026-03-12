package com.thlee.stock.market.stockmarket.portfolio.domain.repository;

import com.thlee.stock.market.stockmarket.portfolio.domain.model.StockPurchaseHistory;

import java.util.List;
import java.util.Optional;

public interface StockPurchaseHistoryRepository {
    StockPurchaseHistory save(StockPurchaseHistory history);
    Optional<StockPurchaseHistory> findById(Long id);
    List<StockPurchaseHistory> findByPortfolioItemId(Long portfolioItemId);
    void delete(StockPurchaseHistory history);
    void deleteByPortfolioItemId(Long portfolioItemId);
}