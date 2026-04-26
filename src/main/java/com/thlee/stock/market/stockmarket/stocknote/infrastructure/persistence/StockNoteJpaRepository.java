package com.thlee.stock.market.stockmarket.stocknote.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface StockNoteJpaRepository extends JpaRepository<StockNoteEntity, Long> {

    Optional<StockNoteEntity> findByIdAndUserId(Long id, Long userId);

    List<StockNoteEntity> findByUserIdAndStockCodeAndNoteDateBetweenOrderByNoteDateAsc(
            Long userId, String stockCode, LocalDate fromDate, LocalDate toDate);

    @Modifying
    @Query("DELETE FROM StockNoteEntity n WHERE n.id = :id AND n.userId = :userId")
    int deleteByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    long countByUserIdAndNoteDateBetween(Long userId, LocalDate fromDate, LocalDate toDate);
}