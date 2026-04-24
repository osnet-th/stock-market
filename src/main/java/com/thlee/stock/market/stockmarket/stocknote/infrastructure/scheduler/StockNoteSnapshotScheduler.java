package com.thlee.stock.market.stockmarket.stocknote.infrastructure.scheduler;

import com.thlee.stock.market.stockmarket.logging.application.LoggingContext;
import com.thlee.stock.market.stockmarket.stock.domain.model.MarketType;
import com.thlee.stock.market.stockmarket.stocknote.application.StockNoteSnapshotService;
import com.thlee.stock.market.stockmarket.stocknote.domain.model.enums.SnapshotType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;

/**
 * 가격 스냅샷 배치 스케줄러.
 *
 * <ul>
 *   <li>국내: 매 영업일 16:00 KST (KRX 장 마감 15:30 직후 여유 30분)</li>
 *   <li>해외: 매 영업일 07:00 KST (미국 장 마감 후 다음날 오전)</li>
 *   <li>AT_NOTE PENDING 재시도: 10분 간격</li>
 * </ul>
 *
 * <p>MarketType enum 값 중 {@code isDomestic()} 으로 분기해 해당 시장 enum 전체를 순회한다.
 * 시장별로 기록이 없는 enum 은 빈 결과로 무해하게 통과.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StockNoteSnapshotScheduler {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final StockNoteSnapshotService snapshotService;

    @Scheduled(cron = "${stocknote.snapshot.domestic.cron:0 0 16 * * MON-FRI}", zone = "Asia/Seoul")
    public void captureDomesticSnapshots() {
        try (var ctx = LoggingContext.forScheduler("stocknote-snapshot-domestic")) {
            LocalDate today = LocalDate.now(KST);
            captureForDomain(true, today);
        }
    }

    @Scheduled(cron = "${stocknote.snapshot.overseas.cron:0 0 7 * * TUE-SAT}", zone = "Asia/Seoul")
    public void captureOverseasSnapshots() {
        try (var ctx = LoggingContext.forScheduler("stocknote-snapshot-overseas")) {
            // 해외는 한국시간 기준 다음날 오전 07:00 실행 → 실제 시장일은 전일.
            LocalDate asOf = LocalDate.now(KST).minusDays(1);
            captureForDomain(false, asOf);
        }
    }

    @Scheduled(cron = "${stocknote.snapshot.retry.cron:0 */10 * * * *}")
    public void retryPendingSnapshots() {
        try (var ctx = LoggingContext.forScheduler("stocknote-snapshot-retry")) {
            try {
                snapshotService.retryPending();
            } catch (Exception e) {
                log.error("stocknote snapshot retry failed", e);
            }
        }
    }

    private void captureForDomain(boolean domestic, LocalDate asOf) {
        for (SnapshotType type : new SnapshotType[]{SnapshotType.D_PLUS_7, SnapshotType.D_PLUS_30}) {
            for (MarketType market : MarketType.values()) {
                if (market.isDomestic() != domestic) {
                    continue;
                }
                try {
                    snapshotService.captureForMarket(type, market, asOf);
                } catch (Exception e) {
                    log.error("snapshot capture failed: type={}, market={}, reason={}",
                            type, market, e.getMessage(), e);
                }
            }
        }
    }
}