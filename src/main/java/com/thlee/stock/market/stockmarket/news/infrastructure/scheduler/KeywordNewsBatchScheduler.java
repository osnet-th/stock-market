package com.thlee.stock.market.stockmarket.news.infrastructure.scheduler;

import com.thlee.stock.market.stockmarket.news.application.KeywordNewsBatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 키워드 뉴스 배치 스케줄러
 */
@Component
@RequiredArgsConstructor
public class KeywordNewsBatchScheduler {

    private final KeywordNewsBatchService keywordNewsBatchService;

    @Scheduled(cron = "0 0 * * * *") // 매 시간 정각
    public void scheduleKeywordNewsBatch() {
        keywordNewsBatchService.executeKeywordNewsBatch();
    }
}