package com.thlee.stock.market.stockmarket.portfolio.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DepositHistoryJpaRepository extends JpaRepository<DepositHistoryEntity, Long> {
    List<DepositHistoryEntity> findByPortfolioItemIdOrderByDepositDateAsc(Long portfolioItemId);
    List<DepositHistoryEntity> findByPortfolioItemIdInOrderByDepositDateAsc(List<Long> portfolioItemIds);
    void deleteByPortfolioItemId(Long portfolioItemId);
}