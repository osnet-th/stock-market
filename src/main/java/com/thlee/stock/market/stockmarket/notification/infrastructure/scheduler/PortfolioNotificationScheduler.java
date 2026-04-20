package com.thlee.stock.market.stockmarket.notification.infrastructure.scheduler;

import com.thlee.stock.market.stockmarket.logging.application.LoggingContext;
import com.thlee.stock.market.stockmarket.notification.application.PortfolioNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 포트폴리오 마감 메일 알림 스케줄러
 * 평일 15:30에 알림 대상 사용자에게 포트폴리오 평가 리포트 발송
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PortfolioNotificationScheduler {

    private final PortfolioNotificationService portfolioNotificationService;

    @Scheduled(cron = "${batch.schedule.portfolio-notification-cron:0 30 15 * * MON-FRI}", zone = "Asia/Seoul")
    public void sendMarketCloseNotification() {
        try (var ctx = LoggingContext.forScheduler("portfolio-notification")) {
            log.info("포트폴리오 마감 알림 배치 시작");
            try {
                int successCount = portfolioNotificationService.sendMarketCloseNotifications();
                log.info("포트폴리오 마감 알림 배치 완료: 발송 {}건", successCount);
            } catch (Exception e) {
                log.error("포트폴리오 마감 알림 배치 실패", e);
            }
        }
    }
}