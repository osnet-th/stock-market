package com.thlee.stock.market.stockmarket.logging.application;

import com.thlee.stock.market.stockmarket.logging.infrastructure.filter.RequestIdFilter;
import org.slf4j.MDC;

import java.util.UUID;

/**
 * 요청이 아닌 경로(스케줄러, 배치 등)에서 진입하는 실행에 requestId 를 부여하는 MDC 유틸.
 *
 * HTTP 진입은 {@link RequestIdFilter} 가 자동으로 UUID 를 MDC 에 세팅하지만,
 * {@code @Scheduled} 메서드는 요청 컨텍스트 없이 실행되므로 수동으로 MDC 를 세팅해야
 * 하위 로그(AUDIT/BUSINESS) 의 {@code requestId} 가 유의미해진다.
 *
 * <pre>
 * {@code
 *   @Scheduled(cron = "...")
 *   public void run() {
 *       try (var ctx = LoggingContext.forScheduler("keyword-news-batch")) {
 *           // 실제 작업
 *       }
 *   }
 * }
 * </pre>
 *
 * try-with-resources 가 끝나면 MDC 에서 requestId 가 제거/복원된다.
 */
public final class LoggingContext {

    private static final String SCHEDULED_PREFIX = "scheduled-";

    private LoggingContext() {
    }

    /**
     * 스케줄러 진입 시 MDC 에 {@code requestId = scheduled-{jobName}-{uuid}} 를 세팅한다.
     * 반환된 {@link Scope} 를 try-with-resources 로 닫으면 이전 값으로 복원.
     *
     * @param jobName 식별 가능한 짧은 소문자 hyphen-case (예: "keyword-news-batch")
     */
    public static Scope forScheduler(String jobName) {
        String previous = MDC.get(RequestIdFilter.MDC_KEY);
        String requestId = SCHEDULED_PREFIX + jobName + "-" + UUID.randomUUID();
        MDC.put(RequestIdFilter.MDC_KEY, requestId);
        return new Scope(previous);
    }

    /**
     * MDC requestId 복원 전용 {@link AutoCloseable}. {@code close()} 는 예외를 던지지 않는다.
     */
    public static final class Scope implements AutoCloseable {

        private final String previous;

        Scope(String previous) {
            this.previous = previous;
        }

        @Override
        public void close() {
            if (previous == null) {
                MDC.remove(RequestIdFilter.MDC_KEY);
            } else {
                MDC.put(RequestIdFilter.MDC_KEY, previous);
            }
        }
    }
}