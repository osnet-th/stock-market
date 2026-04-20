package com.thlee.stock.market.stockmarket.logging.presentation.dto;

import com.thlee.stock.market.stockmarket.logging.domain.model.LogDomain;

import java.time.Instant;
import java.util.List;

/**
 * 로그 검색 요청 파라미터 (Controller 에서 조립).
 *
 * <ul>
 *   <li>{@code from/to} — UTC Instant. null 시 기본 최근 24시간 (service 에서 적용)</li>
 *   <li>{@code userId} — 정확 일치 필터</li>
 *   <li>{@code q} — {@code requestId} 또는 payload 주요 필드 단순 매칭 (현재는 requestId 전용)</li>
 *   <li>{@code status} — {@code status} 최상위 필드 정확 일치 (AUDIT 전용)</li>
 *   <li>{@code exceptionClass} — {@code exceptionClass} 최상위 필드 정확 일치 (ERROR 전용)</li>
 *   <li>{@code size} — 1..100 (service 에서 clamp)</li>
 *   <li>{@code searchAfter} — 이전 페이지 마지막 문서의 sort 값 (timestamp ms + _id). null 시 첫 페이지</li>
 * </ul>
 */
public record LogSearchRequest(
        LogDomain domain,
        Instant from,
        Instant to,
        Long userId,
        String q,
        Integer status,
        String exceptionClass,
        int size,
        List<Object> searchAfter
) {
}