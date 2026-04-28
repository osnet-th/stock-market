package com.thlee.stock.market.stockmarket.newsjournal.infrastructure.persistence;

import com.thlee.stock.market.stockmarket.newsjournal.domain.model.EventCategory;
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
               AND (:category IS NULL OR e.category = :category)
               AND (:fromDate IS NULL OR e.occurredDate >= :fromDate)
               AND (:toDate IS NULL OR e.occurredDate <= :toDate)
             ORDER BY e.occurredDate DESC, e.id DESC
            """)
    List<NewsEventEntity> findList(
            @Param("userId") Long userId,
            @Param("category") EventCategory category,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            Pageable pageable
    );

    @Query("""
            SELECT COUNT(e) FROM NewsEventEntity e
             WHERE e.userId = :userId
               AND (:category IS NULL OR e.category = :category)
               AND (:fromDate IS NULL OR e.occurredDate >= :fromDate)
               AND (:toDate IS NULL OR e.occurredDate <= :toDate)
            """)
    long countList(
            @Param("userId") Long userId,
            @Param("category") EventCategory category,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );

    @Modifying
    @Query("DELETE FROM NewsEventEntity e WHERE e.id = :id AND e.userId = :userId")
    int deleteByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);
}