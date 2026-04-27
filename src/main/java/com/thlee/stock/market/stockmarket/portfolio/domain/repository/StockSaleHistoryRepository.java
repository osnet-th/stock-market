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
     * 사용자 단위 매도 이력 (매도일 내림차순) — PortfolioItem과 ID 조인
     */
    List<StockSaleHistory> findByUserId(Long userId);

    /**
     * 사용자가 매도 이력을 1건이라도 보유한 PortfolioItem id 집합.
     * 보유 카드 삭제 버튼 disabled 판정 등 경량 lookup 전용.
     */
    List<Long> findItemIdsByUserId(Long userId);

    void delete(StockSaleHistory history);
}