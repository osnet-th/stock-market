package com.thlee.stock.market.stockmarket.newsjournal.domain.repository;

import java.time.LocalDate;

/**
 * 사용자 전체 사건의 키워드 평탄화 행 (화면 통계용 projection).
 *
 * <p>키워드 빈도 · 동시 등장(co-occurrence) · 최근 사용 계산의 원자료로,
 * 사건 단위 그룹핑을 위해 {@code eventId} 와 정렬 기준 {@code occurredDate} 를 함께 나른다.
 */
public record NewsEventKeywordRow(Long eventId, LocalDate occurredDate, String keyword) {
}
