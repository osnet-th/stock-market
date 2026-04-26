package com.thlee.stock.market.stockmarket.stocknote.domain.repository;

import com.thlee.stock.market.stockmarket.stock.domain.model.MarketType;
import com.thlee.stock.market.stockmarket.stocknote.domain.model.StockNotePriceSnapshot;
import com.thlee.stock.market.stockmarket.stocknote.domain.model.enums.SnapshotStatus;
import com.thlee.stock.market.stockmarket.stocknote.domain.model.enums.SnapshotType;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 가격 스냅샷 Repository 포트.
 *
 * <p>재시도 배치는 {@link #findRetryable} 로 조회하며, 동일 기록의 AT_NOTE PENDING
 * 업데이트는 {@code WHERE status = 'PENDING'} 조건부 UPDATE 로 경합을 방어한다
 * (경합 결과 row count=0 은 호출자가 warn 로그로 무시).
 */
public interface StockNotePriceSnapshotRepository {

    StockNotePriceSnapshot save(StockNotePriceSnapshot snapshot);

    List<StockNotePriceSnapshot> saveAll(List<StockNotePriceSnapshot> snapshots);

    Optional<StockNotePriceSnapshot> findByNoteIdAndType(Long noteId, SnapshotType type);

    /** 다건 기록의 스냅샷을 한 번에 조회 (N+1 회피 배치 fetch). 키는 noteId. */
    Map<Long, List<StockNotePriceSnapshot>> findAllByNoteIds(Collection<Long> noteIds);

    /**
     * 재시도 대상 스냅샷 조회.
     *
     * @param status PENDING 또는 FAILED
     * @param maxRetryCount retryCount &lt; maxRetryCount 인 행만
     * @param limit 한 배치 처리 상한
     */
    List<StockNotePriceSnapshot> findRetryable(SnapshotStatus status, int maxRetryCount, int limit);

    /**
     * 특정 시장의 D+N 도달 미수집 스냅샷 조회.
     * 구현은 기록일(noteDate)에 영업일 N 을 더한 값이 오늘인 기록을 찾아 AT_NOTE 스냅샷과
     * 함께 반환한다 (응용 계층에서 사용).
     */
    List<PendingCaptureTarget> findDueForCapture(SnapshotType type, MarketType marketType, LocalDate asOfDate);

    /**
     * {@code UPDATE ... WHERE id=? AND status='PENDING'} 조건부 업데이트 (async race-safe).
     *
     * @return 업데이트된 행 수 (0 이면 경합으로 이미 전이/삭제)
     */
    int markSuccessIfPending(Long snapshotId, LocalDate priceDate,
                             java.math.BigDecimal closePrice, java.math.BigDecimal changePercent);

    void deleteByNoteId(Long noteId);

    /** D+N 수집 배치가 필요로 하는 조회 결과 projection. */
    record PendingCaptureTarget(Long noteId, String stockCode, MarketType marketType,
                                com.thlee.stock.market.stockmarket.stock.domain.model.ExchangeCode exchangeCode,
                                LocalDate noteDate,
                                java.math.BigDecimal atNoteClosePrice) { }
}