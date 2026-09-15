package com.thlee.stock.market.stockmarket.portfolio.infrastructure.scheduler;

import com.thlee.stock.market.stockmarket.logging.application.LoggingContext;
import com.thlee.stock.market.stockmarket.portfolio.application.PortfolioSnapshotBatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 자산 스냅샷 일 1회 저장 스케줄러.
 * 평일 국내장 마감(15:30) 직후에 실행해 모든 스냅샷의 기준 시점을 종가로 통일한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PortfolioSnapshotScheduler {

    private final PortfolioSnapshotBatchService portfolioSnapshotBatchService;

    @Scheduled(cron = "${scheduler.portfolio.snapshot.cron:0 40 15 * * MON-FRI}", zone = "Asia/Seoul")
    public void saveDailySnapshots() {
        try (var ctx = LoggingContext.forScheduler("portfolio-snapshot")) {
            log.info("자산 스냅샷 배치 시작");
            try {
                int savedCount = portfolioSnapshotBatchService.saveDailySnapshots();
                log.info("자산 스냅샷 배치 완료: 저장 {}건", savedCount);
            } catch (Exception e) {
                log.error("자산 스냅샷 배치 실패", e);
            }
        }
    }
}
