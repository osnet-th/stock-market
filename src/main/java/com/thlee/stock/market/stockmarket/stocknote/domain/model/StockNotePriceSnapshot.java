package com.thlee.stock.market.stockmarket.stocknote.domain.model;

import com.thlee.stock.market.stockmarket.stocknote.domain.model.enums.SnapshotStatus;
import com.thlee.stock.market.stockmarket.stocknote.domain.model.enums.SnapshotType;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 가격 스냅샷. 상태 머신은 PENDING → SUCCESS | FAILED.
 *
 * <p>실패 누적은 {@code retryCount} 로 관리하며 {@code MAX_RETRY} 초과 시 재시도 종결로 간주.
 */
@Getter
public class StockNotePriceSnapshot {

    /** 재시도 상한. 이후로는 더 이상 재시도하지 않는다. */
    public static final int MAX_RETRY = 3;

    private Long id;
    private final Long noteId;
    private final SnapshotType snapshotType;
    private LocalDate priceDate;
    private BigDecimal closePrice;
    private BigDecimal changePercent;
    private SnapshotStatus status;
    private String failureReason;
    private int retryCount;
    private LocalDateTime capturedAt;

    public StockNotePriceSnapshot(Long id, Long noteId, SnapshotType snapshotType,
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

    /** 기록 생성 시점에 AT_NOTE PENDING 스냅샷을 생성한다. */
    public static StockNotePriceSnapshot createPending(Long noteId, SnapshotType type) {
        Objects.requireNonNull(noteId, "noteId");
        Objects.requireNonNull(type, "snapshotType");
        return new StockNotePriceSnapshot(null, noteId, type, null, null, null,
                SnapshotStatus.PENDING, null, 0, null);
    }

    public void assignId(Long id) {
        if (this.id != null) {
            throw new IllegalStateException("id 는 이미 설정되었습니다.");
        }
        this.id = id;
    }

    /**
     * 가격 조회 성공으로 전이. AT_NOTE 가 아닌 경우 기준 가격(atNotePrice) 을 넘기면
     * AT_NOTE 대비 변화율을 계산해 저장한다.
     */
    public void markSuccess(LocalDate priceDate, BigDecimal closePrice, BigDecimal atNotePrice) {
        Objects.requireNonNull(priceDate, "priceDate");
        Objects.requireNonNull(closePrice, "closePrice");
        this.priceDate = priceDate;
        this.closePrice = closePrice;
        this.changePercent = computeChangePercent(atNotePrice, closePrice);
        this.status = SnapshotStatus.SUCCESS;
        this.failureReason = null;
        this.capturedAt = LocalDateTime.now();
    }

    public void markFailed(String reason) {
        this.status = SnapshotStatus.FAILED;
        this.failureReason = reason != null && reason.length() > 255 ? reason.substring(0, 255) : reason;
        this.retryCount++;
        this.capturedAt = LocalDateTime.now();
    }

    /** 재시도 가능 여부. PENDING 이거나 FAILED+retry 여력이 있을 때 true. */
    public boolean canRetry() {
        return status == SnapshotStatus.PENDING
                || (status == SnapshotStatus.FAILED && retryCount < MAX_RETRY);
    }

    public boolean isRetryExhausted() {
        return status == SnapshotStatus.FAILED && retryCount >= MAX_RETRY;
    }

    public boolean isSuccess() {
        return status == SnapshotStatus.SUCCESS;
    }

    private static BigDecimal computeChangePercent(BigDecimal base, BigDecimal current) {
        if (base == null || base.signum() == 0 || current == null) {
            return null;
        }
        return current.subtract(base)
                .divide(base, 6, RoundingMode.HALF_UP)
                .movePointRight(2)
                .setScale(2, RoundingMode.HALF_UP);
    }
}