package com.thlee.stock.market.stockmarket.portfolio.infrastructure.scheduler;

import com.thlee.stock.market.stockmarket.portfolio.application.PortfolioNewsBatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 포트폴리오 뉴스 배치 스케줄러
 */
@Component
@RequiredArgsConstructor
public class PortfolioNewsBatchScheduler {

    private final PortfolioNewsBatchService portfolioNewsBatchService;

    @Scheduled(cron = "${batch.schedule.portfolio-news-cron:0 0 * * * *}")
    public void schedulePortfolioNewsBatch() {
        portfolioNewsBatchService.collectNews();
    }
}