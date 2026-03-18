package com.thlee.stock.market.stockmarket.portfolio.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CashStockLinkJpaRepository extends JpaRepository<CashStockLinkEntity, Long> {
    Optional<CashStockLinkEntity> findByStockItemId(Long stockItemId);
    List<CashStockLinkEntity> findByCashItemId(Long cashItemId);
    List<CashStockLinkEntity> findByStockItemIdIn(List<Long> stockItemIds);
    void deleteByStockItemId(Long stockItemId);
    boolean existsByCashItemId(Long cashItemId);
}