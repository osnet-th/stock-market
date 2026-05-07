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
 * <p>
 * 일배치에서 region×category 호출을 N병렬로 수행하는 전용 executor.
 * <ul>
 *   <li>core=4 / max=8 / queue=200 — 외부 출처 트래픽 폭증 방지 (학습: parallel-external-fetch-resilience)</li>
 *   <li>{@link ThreadPoolExecutor.AbortPolicy} — 풀 포화 시 거부, 다음 일배치에서 재처리</li>
 *   <li>graceful shutdown — awaitTermination 30s. daemon=false로 처리 중 작업 drain</li>
 *   <li>MDC 전파 TaskDecorator — LoggingContext의 requestId 등을 비동기 스레드에 복사</li>
 * </ul>
 */
@Configuration
public class RealEstateMarketAsyncConfig {

    public static final String FETCH_EXECUTOR_BEAN = "realEstateFetchExecutor";

    @Bean(name = FETCH_EXECUTOR_BEAN)
    public ThreadPoolTaskExecutor realEstateFetchExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("re-fetch-");
        executor.setTaskDecorator(new RealEstateMdcTaskDecorator());
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