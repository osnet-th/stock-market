package com.thlee.stock.market.stockmarket.portfolio.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StockSaleHistoryJpaRepository extends JpaRepository<StockSaleHistoryEntity, Long> {

    List<StockSaleHistoryEntity> findByPortfolioItemIdOrderBySoldAtAsc(Long portfolioItemId);

    /**
     * PortfolioItem 테이블과 ID 조인하여 사용자 단위 매도 이력을 조회한다.
     * 매도일 내림차순 정렬.
     */
    @Query("""
            SELECT s
            FROM StockSaleHistoryEntity s
            JOIN PortfolioItemEntity p ON s.portfolioItemId = p.id
            WHERE p.userId = :userId
            ORDER BY s.soldAt DESC, s.id DESC
            """)
    List<StockSaleHistoryEntity> findByUserId(@Param("userId") Long userId);

    /**
     * 사용자가 매도 이력을 1건이라도 보유한 PortfolioItem id 집합.
     * 보유 카드 삭제 버튼 disabled 판정 전용 — 전체 이력 페이로드 회피.
     */
    @Query("""
            SELECT DISTINCT s.portfolioItemId
            FROM StockSaleHistoryEntity s
            JOIN PortfolioItemEntity p ON s.portfolioItemId = p.id
            WHERE p.userId = :userId
            """)
    List<Long> findItemIdsByUserId(@Param("userId") Long userId);
}