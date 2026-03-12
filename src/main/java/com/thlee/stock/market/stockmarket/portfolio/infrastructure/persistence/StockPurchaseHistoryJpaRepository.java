package com.thlee.stock.market.stockmarket.portfolio.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockPurchaseHistoryJpaRepository extends JpaRepository<StockPurchaseHistoryEntity, Long> {
    List<StockPurchaseHistoryEntity> findByPortfolioItemIdOrderByPurchasedAtAsc(Long portfolioItemId);
    void deleteByPortfolioItemId(Long portfolioItemId);
}