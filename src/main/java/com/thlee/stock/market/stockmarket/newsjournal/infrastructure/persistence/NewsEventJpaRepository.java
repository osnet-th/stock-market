package com.thlee.stock.market.stockmarket.newsjournal.infrastructure.persistence;

import com.thlee.stock.market.stockmarket.newsjournal.domain.model.EventImpact;
import com.thlee.stock.market.stockmarket.newsjournal.domain.repository.NewsEventCategoryCount;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface NewsEventJpaRepository extends JpaRepository<NewsEventEntity, Long> {

    Optional<NewsEventEntity> findByIdAndUserId(Long id, Long userId);

    @Query("""
            SELECT e FROM NewsEventEntity e
             WHERE e.userId = :userId
               AND (:impact IS NULL OR e.impact = :impact)
               AND (:categoryId IS NULL OR e.categoryId = :categoryId)
               AND (:fromDate IS NULL OR e.occurredDate >= :fromDate)
               AND (:toDate IS NULL OR e.occurredDate <= :toDate)
             ORDER BY e.occurredDate DESC, e.id DESC
            """)
    List<NewsEventEntity> findList(
            @Param("userId") Long userId,
            @Param("impact") EventImpact impact,
            @Param("categoryId") Long categoryId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            Pageable pageable
    );

    @Query("""
            SELECT COUNT(e) FROM NewsEventEntity e
             WHERE e.userId = :userId
               AND (:impact IS NULL OR e.impact = :impact)
               AND (:categoryId IS NULL OR e.categoryId = :categoryId)
               AND (:fromDate IS NULL OR e.occurredDate >= :fromDate)
               AND (:toDate IS NULL OR e.occurredDate <= :toDate)
            """)
    long countList(
            @Param("userId") Long userId,
            @Param("impact") EventImpact impact,
            @Param("categoryId") Long categoryId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );

    @Modifying
    @Query("DELETE FROM NewsEventEntity e WHERE e.id = :id AND e.userId = :userId")
    int deleteByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    /**
     * 최근 등록 N건 (대시보드 요약). created_at DESC + id DESC 안정 정렬.
     * categoryCounts 쿼리와 동일하게 NULL category_id 행은 제외해 응답 일관성 유지(legacy backfill 잔재 호환).
     * limit 은 Pageable.getPageSize() 로 강제.
     */
    @Query("""
            SELECT e FROM NewsEventEntity e
             WHERE e.userId = :userId
               AND e.categoryId IS NOT NULL
             ORDER BY e.createdAt DESC, e.id DESC
            """)
    List<NewsEventEntity> findRecentByUserId(@Param("userId") Long userId, Pageable pageable);

    /** 사용자별 카테고리 ID 그룹 합계. NULL 카테고리 행은 제외(레거시 backfill 잔재 호환). */
    @Query("""
            SELECT new com.thlee.stock.market.stockmarket.newsjournal.domain.repository.NewsEventCategoryCount(
                e.categoryId, COUNT(e)
            )
            FROM NewsEventEntity e
            WHERE e.userId = :userId
              AND e.categoryId IS NOT NULL
            GROUP BY e.categoryId
            """)
    List<NewsEventCategoryCount> countByCategoryGroupedByCategoryId(@Param("userId") Long userId);
}