package com.thlee.stock.market.stockmarket.portfolio.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StockSaleHistoryJpaRepository extends JpaRepository<StockSaleHistoryEntity, Long> {

    List<StockSaleHistoryEntity> findByPortfolioItemIdOrderBySoldAtAsc(Long portfolioItemId);

    List<StockSaleHistoryEntity> findByPortfolioItemIdInOrderBySoldAtAsc(List<Long> portfolioItemIds);

    /**
     * PortfolioItem 테이블과 ID 조인하여 사용자 단위 매도 이력을 조회한다.
     * 매도일 내림차순 정렬.
     */
    @Query("""
            SELECT s
            FROM StockSaleHistoryEntity s, PortfolioItemEntity p
            WHERE s.portfolioItemId = p.id
              AND p.userId = :userId
            ORDER BY s.soldAt DESC, s.id DESC
            """)
    List<StockSaleHistoryEntity> findByUserId(@Param("userId") Long userId);

    void deleteByPortfolioItemId(Long portfolioItemId);
}