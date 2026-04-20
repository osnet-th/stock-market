package com.thlee.stock.market.stockmarket.logging.infrastructure.async;

import com.thlee.stock.market.stockmarket.logging.domain.service.LogSanitizer;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Map;

/**
 * 로그 적재 전용 비동기 인프라 설정.
 *
 * <ul>
 *   <li>{@code @EnableAsync} — 프로젝트 최초 도입. Spring Boot 4 가상 스레드 설정과 충돌 회피를 위해
 *       반드시 명시된 {@code @Async("logIndexerExecutor")} 로 지정해야 함</li>
 *   <li>MDC 전파용 커스텀 {@link TaskDecorator} — {@code requestId} 를 async 스레드로 복사</li>
 *   <li>{@code DiscardOldestPolicy} 시뮬레이션 — audit/error 유실 시에도 요청 스레드 블로킹 방지
 *       (CallerRunsPolicy 금지). 드롭 건수는 {@code log.ingestion.dropped} 카운터에 누적</li>
 *   <li>{@link AsyncUncaughtExceptionHandler} — 리스너 미처리 예외 silent drop 방지</li>
 * </ul>
 */
@Slf4j
@Configuration
@EnableAsync
public class LogAsyncConfig implements AsyncConfigurer {

    public static final String EXECUTOR_BEAN_NAME = "logIndexerExecutor";
    public static final String DROPPED_METRIC_NAME = "log.ingestion.dropped";

    private final MeterRegistry meterRegistry;

    public LogAsyncConfig(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Bean(name = EXECUTOR_BEAN_NAME)
    public ThreadPoolTaskExecutor logIndexerExecutor() {
        Counter droppedCounter = Counter.builder(DROPPED_METRIC_NAME)
                .description("로그 인덱싱 큐 포화/거절로 유실된 로그 건수")
                .register(meterRegistry);

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(2000);
        executor.setThreadNamePrefix("log-indexer-");
        executor.setTaskDecorator(new MdcTaskDecorator());
        executor.setRejectedExecutionHandler((runnable, rawExecutor) -> {
            droppedCounter.increment();
            // DiscardOldestPolicy 시뮬레이션 — 가장 오래된 작업 제거 후 신규 작업 등록 시도
            if (!rawExecutor.isShutdown()) {
                rawExecutor.getQueue().poll();
                rawExecutor.execute(runnable);
            }
        });
        executor.initialize();
        return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex, method, params) ->
                log.error("비동기 리스너 미처리 예외: method={}, message={}",
                        method.getName(), ex.getMessage(), ex);
    }

    /**
     * 로그 payload sanitizer. Domain 계층 POJO 를 infrastructure 에서 빈으로 등록
     * (도메인 계층에 Spring 의존 비침투 유지).
     */
    @Bean
    public LogSanitizer logSanitizer() {
        return new LogSanitizer();
    }

    /**
     * MDC ({@code requestId} 등) 를 async 스레드로 전파하는 최소 구현.
     * 실행 후 MDC clear — 스레드 풀 재사용 시 오염 방지.
     */
    static final class MdcTaskDecorator implements TaskDecorator {
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