package com.thlee.stock.market.stockmarket.realestate.infrastructure.config;

import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * realestate 도메인 비동기 인프라 설정.
 * <ul>
 *   <li>fetchExecutor — 일배치 region×category 병렬 fetch. core=4 / max=8 / queue=800 / CallerRunsPolicy.
 *       (실 submit 산정: 56 region × 12 카테고리 - 56 region prefix 헛 submit = 616. 800은 30% 안전계수)</li>
 *   <li>adminBatchExecutor — admin 수동 트리거 dispatch 전용. single-thread / queue 0.
 *       동시 호출은 in-flight guard(Service)에서 409 처리.</li>
 *   <li>graceful shutdown — awaitTermination 30s, daemon=false.</li>
 *   <li>MDC 전파 TaskDecorator.</li>
 * </ul>
 */
@Configuration
public class RealEstateMarketAsyncConfig {

    public static final String FETCH_EXECUTOR_BEAN = "realEstateFetchExecutor";
    public static final String ADMIN_EXECUTOR_BEAN = "realEstateAdminBatchExecutor";

    @Bean(name = FETCH_EXECUTOR_BEAN)
    public ThreadPoolTaskExecutor realEstateFetchExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(800);
        executor.setThreadNamePrefix("re-fetch-");
        executor.setTaskDecorator(new RealEstateMdcTaskDecorator());
        // CallerRunsPolicy: 큐 포화 시 호출자(스케줄러) 스레드가 직접 실행 — 자연 throttle.
        // 학습 parallel-external-fetch-resilience 함정 (1)의 "AbortPolicy 항목 단위 거부" 원칙은
        // Scheduler 의 submit try-catch + supportsRegion 사전 필터로 보강.
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.setDaemon(false);
        executor.initialize();
        return executor;
    }

    @Bean(name = ADMIN_EXECUTOR_BEAN)
    public ThreadPoolTaskExecutor realEstateAdminBatchExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(0);
        executor.setThreadNamePrefix("re-admin-batch-");
        executor.setTaskDecorator(new RealEstateMdcTaskDecorator());
        // admin endpoint 의 in-flight guard 가 이미 동시 호출을 차단하므로 거부 정책은 AbortPolicy.
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.setDaemon(false);
        executor.initialize();
        return executor;
    }

    static final class RealEstateMdcTaskDecorator implements TaskDecorator {
        @Override
        public Runnable decorate(Runnable runnable) {
            Map<String, String> context = MDC.getCopyOfContextMap();
            return () -> {
                if (context != null) {
                    MDC.setContextMap(context);
                }
                try {
                    runnable.run();
                } finally {
                    MDC.clear();
                }
            };
        }
    }
}