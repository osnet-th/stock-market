package com.thlee.stock.market.stockmarket.news.infrastructure.scheduler;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 키워드 뉴스 스케줄러
 * 트리거 역할만 수행, 비즈니스 로직은 KeywordNewsSyncService에 위임
 */
@Component
@RequiredArgsConstructor
public class KeywordNewsScheduler {


    /**
     * 매 시간 키워드 뉴스 동기화 실행
     */
    @Scheduled(cron = "0 0 * * * *")
    public void searchKeywordNews() {
    }
}