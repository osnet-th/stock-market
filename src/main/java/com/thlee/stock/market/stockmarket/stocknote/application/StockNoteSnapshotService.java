package com.thlee.stock.market.stockmarket.stocknote.application;

import com.thlee.stock.market.stockmarket.stock.application.StockPriceService;
import com.thlee.stock.market.stockmarket.stock.domain.model.MarketType;
import com.thlee.stock.market.stockmarket.stocknote.application.exception.StockNoteNotFoundException;
import com.thlee.stock.market.stockmarket.stocknote.domain.model.StockNote;
import com.thlee.stock.market.stockmarket.stocknote.domain.model.StockNotePriceSnapshot;
import com.thlee.stock.market.stockmarket.stocknote.domain.model.enums.SnapshotStatus;
import com.thlee.stock.market.stockmarket.stocknote.domain.model.enums.SnapshotType;
import com.thlee.stock.market.stockmarket.stocknote.domain.repository.StockNotePriceSnapshotRepository;
import com.thlee.stock.market.stockmarket.stocknote.domain.repository.StockNoteRepository;
import com.thlee.stock.market.stockmarket.stocknote.infrastructure.scheduler.BusinessDayCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 가격 스냅샷 캡처 유스케이스 진입점.
 *
 * <p>실제 노트 1건 캡처는 {@link StockNoteSnapshotCaptureExecutor} 가 자기 트랜잭션으로 수행한다.
 * 본 서비스는 (1) 스케줄러 진입점 (captureForMarket / retryPending), (2) 사용자 수동 재시도
 * (manualRetry) 의 상위 오케스트레이션만 담당한다. captureForMarket / retryPending 은 트랜잭션을
 * 갖지 않으며 — executor 호출 마다 새 트랜잭션이 시작되어 per-note 격리를 확보한다.
 *
 * <p>외부 가격 소스는 {@link StockPriceService} 를 경유한다 (stock 도메인 포트 직접 호출 금지,
 * 아키텍처 심화 5).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockNoteSnapshotService {

    private static final int RETRY_BATCH_LIMIT = 200;

    private final StockNoteRepository noteRepository;
    private final StockNotePriceSnapshotRepository snapshotRepository;
    private final BusinessDayCalculator businessDayCalculator;
    private final StockNoteSnapshotCaptureExecutor captureExecutor;

    /**
     * 지정 시장의 D+7 / D+30 도달 스냅샷을 캡처한다 (스케줄러에서 호출).
     * 트랜잭션 미보유 — executor 호출 마다 새 트랜잭션. 한 노트의 트랜잭션 실패가 다음 노트로 전파되지 않음.
     */
    public void captureForMarket(SnapshotType type, MarketType marketType, LocalDate asOfDate) {
        int targetOffset = resolveOffset(type);
        List<StockNotePriceSnapshotRepository.PendingCaptureTarget> targets =
                snapshotRepository.findDueForCapture(type, marketType, asOfDate);
        for (StockNotePriceSnapshotRepository.PendingCaptureTarget target : targets) {
            // 영업일 기준 D+N 이 오늘인지 확인 (간이 한국 영업일만; 해외는 느슨하게 허용).
            LocalDate due = businessDayCalculator.addBusinessDays(target.noteDate(), targetOffset);
            if (marketType.isDomestic() && !due.isEqual(asOfDate)) {
                continue;
            }
            try {
                captureExecutor.captureTarget(type, target);
            } catch (Exception e) {
                log.warn("captureForMarket per-note isolation: noteId={}, type={}, error={}",
                        target.noteId(), type, e.getMessage());
            }
        }
    }

    /**
     * 사용자 수동 재시도 — 권한 검증 후 retryCount/상태 리셋 + 즉시 캡처 실행.
     * outer @Transactional 유지 — executor 호출 시 propagation REQUIRED 로 합류해 reset 결과가
     * 캡처 단계에서 보임 (race-safe).
     *
     * @throws StockNoteNotFoundException 기록이 없거나 본인 소유가 아닐 때 (404)
     * @throws IllegalArgumentException 스냅샷 행이 없거나 이미 SUCCESS 인 경우 (400)
     */
    @Transactional
    public void manualRetry(Long noteId, SnapshotType type, Long userId) {
        StockNote note = noteRepository.findByIdAndUserId(noteId, userId)
                .orElseThrow(() -> new StockNoteNotFoundException(noteId));
        StockNotePriceSnapshot snapshot = snapshotRepository.findByNoteIdAndType(noteId, type)
                .orElseThrow(() -> new IllegalArgumentException("snapshot not found: " + type));
        snapshot.resetForManualRetry();
        snapshotRepository.save(snapshot);

        if (type == SnapshotType.AT_NOTE) {
            captureExecutor.captureAtNote(noteId);
            return;
        }
        // D+7 / D+30 수동 재시도: AT_NOTE close 를 기준으로 inline 캡처
        BigDecimal atNoteClose = snapshotRepository.findByNoteIdAndType(noteId, SnapshotType.AT_NOTE)
                .filter(StockNotePriceSnapshot::isSuccess)
                .map(StockNotePriceSnapshot::getClosePrice)
                .orElse(null);
        captureExecutor.captureTarget(type, new StockNotePriceSnapshotRepository.PendingCaptureTarget(
                note.getId(), note.getStockCode(), note.getMarketType(), note.getExchangeCode(),
                note.getNoteDate(), atNoteClose));
    }

    /**
     * PENDING 스냅샷을 재시도한다 (10분 간격 스케줄러에서 호출).
     * 트랜잭션 미보유 — executor 호출 마다 새 트랜잭션. retryCount &lt; MAX_RETRY 만 대상.
     */
    public void retryPending() {
        List<StockNotePriceSnapshot> retryable = snapshotRepository.findRetryable(
                SnapshotStatus.PENDING, StockNotePriceSnapshot.MAX_RETRY, RETRY_BATCH_LIMIT);
        for (StockNotePriceSnapshot snapshot : retryable) {
            try {
                if (snapshot.getSnapshotType() == SnapshotType.AT_NOTE) {
                    captureExecutor.captureAtNote(snapshot.getNoteId());
                } else {
                    // D+7/D+30 PENDING 재시도 — note 정보 + AT_NOTE close 조립 후 captureTarget.
                    StockNote note = noteRepository.findById(snapshot.getNoteId()).orElse(null);
                    if (note == null) continue;
                    BigDecimal atNoteClose = snapshotRepository.findByNoteIdAndType(snapshot.getNoteId(), SnapshotType.AT_NOTE)
                            .filter(StockNotePriceSnapshot::isSuccess)
                            .map(StockNotePriceSnapshot::getClosePrice)
                            .orElse(null);
                    captureExecutor.captureTarget(snapshot.getSnapshotType(),
                            new StockNotePriceSnapshotRepository.PendingCaptureTarget(
                                    note.getId(), note.getStockCode(), note.getMarketType(),
                                    note.getExchangeCode(), note.getNoteDate(), atNoteClose));
                }
            } catch (Exception e) {
                log.warn("retryPending per-note isolation: noteId={}, type={}, error={}",
                        snapshot.getNoteId(), snapshot.getSnapshotType(), e.getMessage());
            }
        }
    }

    private static int resolveOffset(SnapshotType type) {
        return switch (type) {
            case D_PLUS_7 -> 7;
            case D_PLUS_30 -> 30;
            default -> throw new IllegalArgumentException("market capture 대상이 아님: " + type);
        };
    }
}