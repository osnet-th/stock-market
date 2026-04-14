package com.thlee.stock.market.stockmarket.portfolio.domain.repository;

import com.thlee.stock.market.stockmarket.portfolio.domain.model.DepositHistory;

import java.util.List;
import java.util.Optional;

public interface DepositHistoryRepository {
    DepositHistory save(DepositHistory history);
    Optional<DepositHistory> findById(Long id);
    List<DepositHistory> findByPortfolioItemId(Long portfolioItemId);
    List<DepositHistory> findByPortfolioItemIdIn(List<Long> portfolioItemIds);
    void delete(DepositHistory history);
    void deleteByPortfolioItemId(Long portfolioItemId);
}