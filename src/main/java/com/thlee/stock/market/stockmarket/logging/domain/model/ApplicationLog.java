package com.thlee.stock.market.stockmarket.logging.domain.model;

import java.time.Instant;
import java.util.Map;

/**
 * 애플리케이션 로그 도메인 모델 (불변).
 *
 * 공통 필드 + 도메인별 자유 payload 구조. 단일 record 로 3개 도메인(AUDIT/ERROR/BUSINESS)
 * 모두 표현하며, 월별 인덱스 라우팅은 UTC 기준 timestamp 의 연월에 따른다.
 *
 * Timestamp 규약: AOP {@code @Around} 진입 시 또는 {@code publishEvent} 직전에 한 번만 찍고
 * 리스너/indexer 는 재할당 금지 (best-effort at-most-once 정책).
 */
public record ApplicationLog(
        Instant timestamp,
        LogDomain domain,
        Long userId,
        String requestId,
        Map<String, Object> payload,
        boolean truncated,
        Integer originalSize
) {
}