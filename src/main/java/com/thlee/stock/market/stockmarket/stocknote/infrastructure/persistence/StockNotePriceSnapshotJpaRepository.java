package com.thlee.stock.market.stockmarket.stocknote.infrastructure.persistence;

import com.thlee.stock.market.stockmarket.stock.domain.model.MarketType;
import com.thlee.stock.market.stockmarket.stocknote.domain.model.enums.SnapshotStatus;
import com.thlee.stock.market.stockmarket.stocknote.domain.model.enums.SnapshotType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface StockNotePriceSnapshotJpaRepository extends JpaRepository<StockNotePriceSnapshotEntity, Long> {

    Optional<StockNotePriceSnapshotEntity> findByNoteIdAndSnapshotType(Long noteId, SnapshotType snapshotType);

    List<StockNotePriceSnapshotEntity> findByNoteIdIn(Collection<Long> noteIds);

    void deleteByNoteId(Long noteId);

    /** 재시도 스캔: 지정 상태 + retry_count &lt; max 행 조회 (Pageable 로 상한 제어). */
    @Query("SELECT s FROM StockNotePriceSnapshotEntity s "
            + "WHERE s.status = :status AND s.retryCount < :maxRetry "
            + "ORDER BY s.id ASC")
    List<StockNotePriceSnapshotEntity> findRetryable(@Param("status") SnapshotStatus status,
                                                     @Param("maxRetry") int maxRetry,
                                                     Pageable pageable);

    /**
     * AT_NOTE PENDING 행을 race-safe 하게 SUCCESS 로 전이.
     * row count 가 0 이면 이미 전이/삭제된 상태 (호출자가 로그 처리).
     */
    @Modifying
    @Query("UPDATE StockNotePriceSnapshotEntity s "
            + "SET s.status = com.thlee.stock.market.stockmarket.stocknote.domain.model.enums.SnapshotStatus.SUCCESS, "
            + "    s.priceDate = :priceDate, s.closePrice = :closePrice, s.changePercent = :changePercent, "
            + "    s.failureReason = null, s.capturedAt = :capturedAt "
            + "WHERE s.id = :id "
            + "  AND s.status = com.thlee.stock.market.stockmarket.stocknote.domain.model.enums.SnapshotStatus.PENDING")
    int markSuccessIfPending(@Param("id") Long id,
                             @Param("priceDate") LocalDate priceDate,
                             @Param("closePrice") BigDecimal closePrice,
                             @Param("changePercent") BigDecimal changePercent,
                             @Param("capturedAt") LocalDateTime capturedAt);

    /**
     * 특정 시장의 D+N 도달 대기 스냅샷 조회.
     * JOIN stock_note 로 stockCode/marketType/noteDate 와 AT_NOTE 종가 반환.
     */
    @Query(value = """
            SELECT n.id              AS note_id,
                   n.stock_code      AS stock_code,
                   n.market_type     AS market_type,
                   n.exchange_code   AS exchange_code,
                   n.note_date       AS note_date,
                   atps.close_price  AS at_note_close_price
            FROM stock_note_price_snapshot ps
            JOIN stock_note n ON n.id = ps.note_id
            LEFT JOIN stock_note_price_snapshot atps
                   ON atps.note_id = n.id
                  AND atps.snapshot_type = 'AT_NOTE'
                  AND atps.status = 'SUCCESS'
            WHERE ps.snapshot_type = :type
              AND ps.status = 'PENDING'
              AND n.market_type = :marketType
            ORDER BY n.note_date ASC
            """, nativeQuery = true)
    List<PendingCaptureRow> findDueForCapture(@Param("type") String type,
                                              @Param("marketType") String marketType);

    /** native 쿼리 row projection. */
    interface PendingCaptureRow {
        Long getNoteId();

        String getStockCode();

        String getMarketType();

        String getExchangeCode();

        LocalDate getNoteDate();

        BigDecimal getAtNoteClosePrice();
    }
}