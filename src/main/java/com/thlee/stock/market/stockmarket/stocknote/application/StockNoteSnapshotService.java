package com.thlee.stock.market.stockmarket.stocknote.application;

import com.thlee.stock.market.stockmarket.stock.application.StockPriceService;
import com.thlee.stock.market.stockmarket.stock.application.dto.StockPriceResponse;
import com.thlee.stock.market.stockmarket.stock.domain.model.MarketType;
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
import java.util.Optional;

/**
 * 가격 스냅샷 캡처/재시도 유스케이스.
 *
 * <p>외부 가격 소스는 {@link StockPriceService} 를 경유해 호출한다 (stock 도메인 포트 직접 호출 금지,
 * 아키텍처 심화 5). 각 메서드는 자체 트랜잭션 경계를 가지며, 종목별 실패는 try/catch 로 격리되어
 * 배치 전체 실패를 막는다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockNoteSnapshotService {

    private static final int RETRY_BATCH_LIMIT = 200;

    private final StockNoteRepository noteRepository;
    private final StockNotePriceSnapshotRepository snapshotRepository;
    private final StockPriceService stockPriceService;
    private final BusinessDayCalculator businessDayCalculator;

    /**
     * 특정 기록의 AT_NOTE PENDING 스냅샷을 현재가로 갱신한다.
     * 이벤트 리스너 또는 수동 재시도 경로에서 호출.
     */
    @Transactional
    public void captureAtNote(Long noteId) {
        Optional<StockNotePriceSnapshot> snapshotOpt = snapshotRepository.findByNoteIdAndType(noteId, SnapshotType.AT_NOTE);
        if (snapshotOpt.isEmpty()) {
            log.warn("AT_NOTE snapshot missing for noteId={} (already deleted?)", noteId);
            return;
        }
        StockNotePriceSnapshot snapshot = snapshotOpt.get();
        if (snapshot.isSuccess() || snapshot.isRetryExhausted()) {
            return;
        }
        Optional<StockNote> noteOpt = noteRepository.findById(noteId);
        if (noteOpt.isEmpty()) {
            log.warn("stocknote missing for noteId={} (deleted mid-capture)", noteId);
            return;
        }
        StockNote note = noteOpt.get();
        try {
            BigDecimal price = fetchCurrentPrice(note);
            // AT_NOTE 는 기준가 자체이므로 changePercent 는 0 (Domain 의 markSuccess 가 null 반환).
            // 동일 행을 경합 없이 갱신하기 위해 conditional UPDATE 사용 (race-safe).
            int updated = snapshotRepository.markSuccessIfPending(
                    snapshot.getId(),
                    LocalDate.now(),
                    price,
                    BigDecimal.ZERO
            );
            if (updated == 0) {
                log.warn("AT_NOTE snapshot conditional update skipped: id={} (status transitioned)",
                        snapshot.getId());
            }
        } catch (Exception e) {
            snapshot.markFailed("AT_NOTE capture failed: " + e.getClass().getSimpleName());
            snapshotRepository.save(snapshot);
            log.warn("AT_NOTE capture failed: noteId={}, retryCount={}, reason={}",
                    noteId, snapshot.getRetryCount(), e.getMessage());
        }
    }

    /**
     * 지정 시장의 D+7 / D+30 도달 스냅샷을 캡처한다 (스케줄러에서 호출).
     */
    @Transactional
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
            captureTarget(type, target);
        }
    }

    /**
     * PENDING 스냅샷을 재시도한다 (10분 간격 스케줄러에서 호출).
     * retryCount &lt; MAX_RETRY 만 대상. MAX 도달 시 이 쿼리에서 제외되어 자연 종결.
     */
    @Transactional
    public void retryPending() {
        List<StockNotePriceSnapshot> retryable = snapshotRepository.findRetryable(
                SnapshotStatus.PENDING, StockNotePriceSnapshot.MAX_RETRY, RETRY_BATCH_LIMIT);
        for (StockNotePriceSnapshot snapshot : retryable) {
            if (snapshot.getSnapshotType() == SnapshotType.AT_NOTE) {
                captureAtNote(snapshot.getNoteId());
            }
            // D+7/D+30 PENDING 재시도는 market scheduler 경로에서 처리됨
        }
    }

    private void captureTarget(SnapshotType type,
                               StockNotePriceSnapshotRepository.PendingCaptureTarget target) {
        Optional<StockNotePriceSnapshot> existing = snapshotRepository
                .findByNoteIdAndType(target.noteId(), type);
        if (existing.isEmpty()) {
            log.warn("snapshot row missing for noteId={}, type={}", target.noteId(), type);
            return;
        }
        StockNotePriceSnapshot snapshot = existing.get();
        if (!snapshot.canRetry()) {
            return;
        }
        StockNote note = noteRepository.findById(target.noteId()).orElse(null);
        if (note == null) {
            log.warn("stocknote missing during market capture: noteId={}", target.noteId());
            return;
        }
        try {
            BigDecimal price = fetchCurrentPrice(note);
            snapshot.markSuccess(LocalDate.now(), price, target.atNoteClosePrice());
            snapshotRepository.save(snapshot);
        } catch (Exception e) {
            snapshot.markFailed(type.name() + " capture failed: " + e.getClass().getSimpleName());
            snapshotRepository.save(snapshot);
            log.warn("{} capture failed: noteId={}, retryCount={}, reason={}",
                    type, target.noteId(), snapshot.getRetryCount(), e.getMessage());
        }
    }

    private BigDecimal fetchCurrentPrice(StockNote note) {
        StockPriceResponse response = stockPriceService.getPrice(
                note.getStockCode(), note.getMarketType(), note.getExchangeCode());
        String raw = response.getCurrentPrice();
        if (raw == null || raw.isBlank()) {
            throw new IllegalStateException("empty current price for " + note.getStockCode());
        }
        return new BigDecimal(raw.replace(",", ""));
    }

    private static int resolveOffset(SnapshotType type) {
        return switch (type) {
            case D_PLUS_7 -> 7;
            case D_PLUS_30 -> 30;
            default -> throw new IllegalArgumentException("market capture 대상이 아님: " + type);
        };
    }
}