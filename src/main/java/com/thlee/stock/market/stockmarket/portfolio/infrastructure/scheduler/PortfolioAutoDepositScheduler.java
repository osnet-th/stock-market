package com.thlee.stock.market.stockmarket.portfolio.infrastructure.scheduler;

import com.thlee.stock.market.stockmarket.logging.application.LoggingContext;
import com.thlee.stock.market.stockmarket.portfolio.application.PortfolioAutoDepositBatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 자동 납입 일배치 스케줄러.
 * 매일 00:10(KST)에 납입일이 된 자동 반영 현금성 항목의 납입을 기록한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PortfolioAutoDepositScheduler {

    private final PortfolioAutoDepositBatchService portfolioAutoDepositBatchService;

    @Scheduled(cron = "${scheduler.portfolio.auto-deposit.cron:0 10 0 * * *}", zone = "Asia/Seoul")
    public void recordAutoDeposits() {
        try (var ctx = LoggingContext.forScheduler("portfolio-auto-deposit")) {
            log.info("자동 납입 배치 시작");
            try {
                int recordedCount = portfolioAutoDepositBatchService.recordDueAutoDeposits();
                log.info("자동 납입 배치 완료: 기록 {}건", recordedCount);
            } catch (Exception e) {
                log.error("자동 납입 배치 실패", e);
            }
        }
    }
}
