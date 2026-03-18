package com.thlee.stock.market.stockmarket.portfolio.domain.repository;

import com.thlee.stock.market.stockmarket.portfolio.domain.model.CashStockLink;

import java.util.List;
import java.util.Optional;

public interface CashStockLinkRepository {
    CashStockLink save(CashStockLink link);
    Optional<CashStockLink> findByStockItemId(Long stockItemId);
    List<CashStockLink> findByCashItemId(Long cashItemId);
    List<CashStockLink> findByStockItemIdIn(List<Long> stockItemIds);
    void deleteByStockItemId(Long stockItemId);
    boolean existsByCashItemId(Long cashItemId);
}