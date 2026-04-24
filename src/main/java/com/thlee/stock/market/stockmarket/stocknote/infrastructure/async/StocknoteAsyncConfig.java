package com.thlee.stock.market.stockmarket.stocknote.infrastructure.async;

import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * stocknote 도메인 비동기 인프라 설정.
 *
 * <p>전용 {@link ThreadPoolTaskExecutor} 빈 {@code stocknoteSnapshotExecutor} 를 제공한다.
 * {@code @EnableAsync} 는 {@link com.thlee.stock.market.stockmarket.logging.infrastructure.async.LogAsyncConfig}
 * 에서 이미 활성화되어 있으므로 재선언하지 않는다.
 *
 * <ul>
 *   <li>core=4 / max=8 / queue=200 — KIS 레이트리밋 고려 (async 리서치 심화 11)</li>
 *   <li>rejection = {@link ThreadPoolExecutor.CallerRunsPolicy} — AT_NOTE 스냅샷 유실 금지.
 *       이벤트 리스너가 AFTER_COMMIT 으로 호출되므로 caller 스레드 점유 허용 가능</li>
 *   <li>MDC 전파 TaskDecorator — LoggingContext 의 requestId 를 async 스레드에 복사</li>
 *   <li>graceful shutdown — awaitTermination 30초, 처리 중 스냅샷 drain</li>
 * </ul>
 */
@Configuration
public class StocknoteAsyncConfig {

    public static final String SNAPSHOT_EXECUTOR_BEAN = "stocknoteSnapshotExecutor";

    @Bean(name = SNAPSHOT_EXECUTOR_BEAN)
    public ThreadPoolTaskExecutor stocknoteSnapshotExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("stocknote-snap-");
        executor.setTaskDecorator(new StocknoteMdcTaskDecorator());
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }

    /**
     * MDC (requestId 등) 를 async 스레드로 복사. 실행 후 clear 로 풀 오염 방지.
     */
    static final class StocknoteMdcTaskDecorator implements TaskDecorator {
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