package com.thlee.stock.market.stockmarket.stocknote.infrastructure.persistence;

import com.thlee.stock.market.stockmarket.stocknote.domain.model.enums.SnapshotStatus;
import com.thlee.stock.market.stockmarket.stocknote.domain.model.enums.SnapshotType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 가격 스냅샷 Entity. AT_NOTE / D+7 / D+30 3가지 시점.
 */
@Entity
@Table(
        name = "stock_note_price_snapshot",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_stock_note_snapshot_note_type",
                        columnNames = {"note_id", "snapshot_type"})
        },
        indexes = {
                // 재시도 배치 스캔 (PENDING/FAILED 상태별 조회)
                @Index(name = "idx_stock_note_snapshot_status_type",
                        columnList = "status, snapshot_type")
        }
)
@Getter
public class StockNotePriceSnapshotEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "note_id", nullable = false)
    private Long noteId;

    @Enumerated(EnumType.STRING)
    @Column(name = "snapshot_type", nullable = false, length = 20)
    private SnapshotType snapshotType;

    @Column(name = "price_date")
    private LocalDate priceDate;

    @Column(name = "close_price", precision = 18, scale = 4)
    private BigDecimal closePrice;

    @Column(name = "change_percent", precision = 8, scale = 2)
    private BigDecimal changePercent;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 15)
    private SnapshotStatus status;

    @Column(name = "failure_reason", length = 255)
    private String failureReason;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "captured_at")
    private LocalDateTime capturedAt;

    protected StockNotePriceSnapshotEntity() {
    }

    public StockNotePriceSnapshotEntity(Long id, Long noteId, SnapshotType snapshotType,
                                        LocalDate priceDate, BigDecimal closePrice, BigDecimal changePercent,
                                        SnapshotStatus status, String failureReason, int retryCount,
                                        LocalDateTime capturedAt) {
        this.id = id;
        this.noteId = noteId;
        this.snapshotType = snapshotType;
        this.priceDate = priceDate;
        this.closePrice = closePrice;
        this.changePercent = changePercent;
        this.status = status;
        this.failureReason = failureReason;
        this.retryCount = retryCount;
        this.capturedAt = capturedAt;
    }
}