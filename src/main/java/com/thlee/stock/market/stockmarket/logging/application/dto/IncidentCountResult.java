package com.thlee.stock.market.stockmarket.logging.application.dto;

import java.time.Instant;

/**
 * 운영자 카드용 "오늘 장애 건수" 조회 결과(application 결과 DTO).
 *
 * <ul>
 *   <li>{@code count} — KST 오늘 00:00 ~ 24:00 의 ERROR 도메인 카운트.
 *       {@code available=false} 일 때는 {@code 0} 으로 채워진다(의미 없음).</li>
 *   <li>{@code asOf} — 조회 수행 시점.</li>
 *   <li>{@code available} — ES 정상 응답 여부.
 *       {@code false} 면 ES 어댑터가 예외를 던져 graceful degrade 된 상태.</li>
 * </ul>
 */
public record IncidentCountResult(long count, Instant asOf, boolean available) { }