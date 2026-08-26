package com.thlee.stock.market.stockmarket.stock.infrastructure.stock.dart.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 재무 타임라인 병렬 수집용 전용 executor.
 * 동시성 8로 제한해 DART 호출이 과도하게 몰리지 않도록 한다.
 */
@Configuration
public class FinancialTimelineExecutorConfig {

    public static final String EXECUTOR_NAME = "financialTimelineExecutor";

    @Bean(EXECUTOR_NAME)
    public ThreadPoolTaskExecutor financialTimelineExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(8);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("fin-timeline-");
        executor.initialize();
        return executor;
    }
}
