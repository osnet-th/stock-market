package com.thlee.stock.market.stockmarket.favorite.infrastructure.config;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 글로벌 관심 지표 cold-cache 스크래핑 병렬화를 위한 전용 고정 풀 (Rev.2).
 * Tomcat 워커 풀/Spring 공용 풀과 분리해 상한을 고정한다.
 * Rev.3: daemon=false 로 바꾸고 @PreDestroy 에서 graceful shutdown 수행 (SIGTERM 시 in-flight 요청 완주).
 */
@Slf4j
@Configuration
public class GlobalFavoriteExecutorConfig {

    public static final String BEAN_NAME = "globalFavoriteFetchExecutor";

    private static final int POOL_SIZE = 4;
    /** 카테고리 전체 병렬 조회의 벽시계 상한. cold p95 5s 목표 + 여유 (Rev.3). */
    public static final long WALL_CLOCK_TIMEOUT_SECONDS = 8L;
    private static final long SHUTDOWN_GRACE_SECONDS = 5L;

    private ExecutorService executor;

    @Bean(name = BEAN_NAME)
    public ExecutorService globalFavoriteFetchExecutor() {
        this.executor = Executors.newFixedThreadPool(POOL_SIZE, new NamedThreadFactory());
        return this.executor;
    }

    /**
     * graceful shutdown: 신규 task 거부 → 진행 중 task 의 완료를 최대 {@value #SHUTDOWN_GRACE_SECONDS} 초 대기 → 강제 종료.
     * Tomcat graceful shutdown 과 맞춰 in-flight 대시보드 요청이 5xx 로 잘리지 않도록 한다 (Rev.3).
     */
    @PreDestroy
    public void shutdownGracefully() {
        if (executor == null) {
            return;
        }
        executor.shutdown();
        try {
            if (!executor.awaitTermination(SHUTDOWN_GRACE_SECONDS, TimeUnit.SECONDS)) {
                log.warn("globalFavoriteFetchExecutor 가 {}초 내 종료되지 않아 shutdownNow 실행",
                    SHUTDOWN_GRACE_SECONDS);
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            executor.shutdownNow();
        }
    }

    private static class NamedThreadFactory implements ThreadFactory {
        private final AtomicInteger counter = new AtomicInteger();

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "global-fav-fetch-" + counter.incrementAndGet());
            // Rev.3: daemon=false — 진행 중 HTTP fetch 가 JVM exit 시 즉시 잘리지 않도록.
            t.setDaemon(false);
            return t;
        }
    }
}