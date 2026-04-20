package com.thlee.stock.market.stockmarket.logging.application.event;

import com.thlee.stock.market.stockmarket.logging.domain.model.ApplicationLog;
import com.thlee.stock.market.stockmarket.logging.domain.model.LogDomain;

import java.time.Instant;
import java.util.Map;

/**
 * 로그 적재를 위한 Spring 애플리케이션 이벤트.
 *
 * {@code ApplicationEventPublisher.publishEvent(...)} 로 발행되어
 * {@code LogEventListener} 가 비동기로 처리한다.
 *
 * Timestamp 규약 (Data Integrity Policy): 발행 시점에 {@link #of} 로 한 번만 확정한다.
 * 리스너/indexer 는 재할당 금지.
 */
public record ApplicationLogEvent(
        LogDomain domain,
        Instant timestamp,
        Long userId,
        String requestId,
        Map<String, Object> payload
) {

    /**
     * 현재 시각을 {@code timestamp} 로 확정해 이벤트 생성.
     */
    public static ApplicationLogEvent of(LogDomain domain,
                                         Long userId,
                                         String requestId,
                                         Map<String, Object> payload) {
        return new ApplicationLogEvent(domain, Instant.now(), userId, requestId, payload);
    }

    /**
     * 도메인 모델로 변환. {@code truncated/originalSize} 는 sanitizer 단계에서 갱신된다.
     */
    public ApplicationLog toApplicationLog() {
        return new ApplicationLog(timestamp, domain, userId, requestId, payload, false, null);
    }
}