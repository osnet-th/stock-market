package com.thlee.stock.market.stockmarket.stocknote.application;

import com.thlee.stock.market.stockmarket.stock.application.StockPriceService;
import com.thlee.stock.market.stockmarket.stock.application.dto.StockPriceResponse;
import com.thlee.stock.market.stockmarket.stocknote.domain.model.StockNote;
import com.thlee.stock.market.stockmarket.stocknote.domain.model.StockNotePriceSnapshot;
import com.thlee.stock.market.stockmarket.stocknote.domain.model.enums.SnapshotType;
import com.thlee.stock.market.stockmarket.stocknote.domain.repository.StockNotePriceSnapshotRepository;
import com.thlee.stock.market.stockmarket.stocknote.domain.repository.StockNoteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

/**
 * 가격 스냅샷 1건 캡처 단위 — per-note 트랜잭션 격리 전용 빈.
 *
 * <p>외부 빈 분리로 Spring AOP 프록시가 동작 — 호출자가 트랜잭션 없으면 새 트랜잭션 시작,
 * 호출자가 트랜잭션 안이면 합류 (default REQUIRED). 이 분리는 self-invocation 회피와
 * captureForMarket / retryPending 의 per-note 격리를 동시에 달성한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockNoteSnapshotCaptureExecutor {

    private final StockNoteRepository noteRepository;
    private final StockNotePriceSnapshotRepository snapshotRepository;
    private final StockPriceService stockPriceService;

    /**
     * 특정 기록의 AT_NOTE PENDING 스냅샷을 현재가로 갱신한다.
     * 이벤트 리스너 / 사용자 수동 재시도 / retryPending 배치 공통 진입점.
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
            BigDecimal price = fetchCurrentPrice(note.getStockCode(), note.getMarketType(), note.getExchangeCode());
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
                return;
            }
            // AT_NOTE 가 SUCCESS 로 전이됐으니 동일 noteId 의 D+N SUCCESS+changePercent=null 행 보강.
            backfillDPlusNChangePercent(noteId, price);
        } catch (Exception e) {
            snapshot.markFailed("AT_NOTE capture failed: " + e.getClass().getSimpleName());
            snapshotRepository.save(snapshot);
            log.warn("AT_NOTE capture failed: noteId={}, retryCount={}, reason={}",
                    noteId, snapshot.getRetryCount(), e.getMessage());
        }
    }

    /**
     * D+7 / D+30 PENDING 스냅샷 1건 캡처. 호출자가 PendingCaptureTarget 으로 노트 정보를 미리 보유.
     */
    @Transactional
    public void captureTarget(SnapshotType type,
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
        // AT_NOTE 가 SUCCESS 가 아니면 (atNoteClose=null) D+N 캡처를 미루고 PENDING 유지.
        // changePercent=null SUCCESS 가 영구 저장되는 것을 차단. AT_NOTE 가 늦게 SUCCESS 되면
        // captureAtNote 의 backfill 또는 다음 cron 회차의 재시도(Task #24 후속) 가 처리.
        if (target.atNoteClosePrice() == null) {
            log.info("D+N capture deferred (AT_NOTE not yet SUCCESS): noteId={}, type={}",
                    target.noteId(), type);
            return;
        }
        // PendingCaptureTarget 이 stockCode/marketType/exchangeCode 를 모두 들고 있으므로
        // noteRepository.findById 호출 없이 target 으로 가격 조회 (ce-review #27 redundant 제거).
        try {
            BigDecimal price = fetchCurrentPrice(target.stockCode(), target.marketType(), target.exchangeCode());
            snapshot.markSuccess(LocalDate.now(), price, target.atNoteClosePrice());
            snapshotRepository.save(snapshot);
        } catch (Exception e) {
            snapshot.markFailed(type.name() + " capture failed: " + e.getClass().getSimpleName());
            snapshotRepository.save(snapshot);
            log.warn("{} capture failed: noteId={}, retryCount={}, reason={}",
                    type, target.noteId(), snapshot.getRetryCount(), e.getMessage());
        }
    }

    /**
     * AT_NOTE 가 늦게 SUCCESS 된 경우 동일 noteId 의 D+7/D+30 SUCCESS+changePercent=null 행을 보강.
     * captureAtNote 의 markSuccessIfPending 직후 같은 트랜잭션에서 호출된다.
     */
    private void backfillDPlusNChangePercent(Long noteId, BigDecimal atNotePrice) {
        for (SnapshotType type : new SnapshotType[]{SnapshotType.D_PLUS_7, SnapshotType.D_PLUS_30}) {
            snapshotRepository.findByNoteIdAndType(noteId, type)
                    .filter(s -> s.backfillChangePercent(atNotePrice))
                    .ifPresent(s -> {
                        snapshotRepository.save(s);
                        log.info("D+N changePercent backfilled: noteId={}, type={}", noteId, type);
                    });
        }
    }

    private BigDecimal fetchCurrentPrice(String stockCode,
                                         com.thlee.stock.market.stockmarket.stock.domain.model.MarketType marketType,
                                         com.thlee.stock.market.stockmarket.stock.domain.model.ExchangeCode exchangeCode) {
        StockPriceResponse response = stockPriceService.getPrice(stockCode, marketType, exchangeCode);
        String raw = response.getCurrentPrice();
        if (raw == null || raw.isBlank()) {
            throw new IllegalStateException("empty current price for " + stockCode);
        }
        return new BigDecimal(raw.replace(",", ""));
    }
}
