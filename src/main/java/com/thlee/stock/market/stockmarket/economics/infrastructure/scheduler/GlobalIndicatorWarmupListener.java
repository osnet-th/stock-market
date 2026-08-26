package com.thlee.stock.market.stockmarket.economics.infrastructure.scheduler;

import com.thlee.stock.market.stockmarket.economics.application.GlobalIndicatorSaveService;
import com.thlee.stock.market.stockmarket.economics.domain.repository.GlobalIndicatorLatestRepository;
import com.thlee.stock.market.stockmarket.logging.application.LoggingContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 앱 시작 시 글로벌 경제지표 조건부 catch-up (#51)
 *
 * 마지막 수집(last_collected_at)이 없거나 {@link #CATCH_UP_THRESHOLD} 이상 경과한
 * 경우에만 수집을 보충한다 — 배치 시각에 앱이 내려가 있던 날을 복구하되,
 * 잦은 재배포 시 TradingEconomics 중복 스크래핑(41개 지표 × 1.5초)은 피한다.
 *
 * 수집은 약 2분 소요되므로 다른 ApplicationReadyEvent 리스너(ECOS warmup 등)를
 * 막지 않도록 전용 daemon 스레드에서 실행한다. 시작 직후 정규 배치 시각과 겹치면
 * 드물게 동시 수집될 수 있으나, cycle 중복 행은 히스토리 조회의 cycle 단위
 * dedup(ROW_NUMBER) 이 흡수한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GlobalIndicatorWarmupListener {

    private static final Duration CATCH_UP_THRESHOLD = Duration.ofHours(20);

    private final GlobalIndicatorSaveService globalIndicatorSaveService;
    private final GlobalIndicatorLatestRepository globalIndicatorLatestRepository;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        LocalDateTime lastCollectedAt =
            globalIndicatorLatestRepository.findMaxLastCollectedAt().orElse(null);

        if (lastCollectedAt != null
                && Duration.between(lastCollectedAt, LocalDateTime.now()).compareTo(CATCH_UP_THRESHOLD) < 0) {
            log.info("글로벌 경제지표 catch-up 불필요: 마지막 수집 {}", lastCollectedAt);
            return;
        }

        Thread worker = new Thread(() -> runCatchUp(lastCollectedAt), "global-indicator-warmup");
        worker.setDaemon(true);
        worker.start();
    }

    private void runCatchUp(LocalDateTime lastCollectedAt) {
        try (var ctx = LoggingContext.forScheduler("global-indicator-warmup")) {
            log.info("글로벌 경제지표 catch-up 시작 (마지막 수집: {})", lastCollectedAt);
            try {
                int savedCount = globalIndicatorSaveService.fetchAndSave();
                log.info("글로벌 경제지표 catch-up 완료: {}건", savedCount);
            } catch (Exception e) {
                log.error("글로벌 경제지표 catch-up 실패", e);
            }
        }
    }
}
