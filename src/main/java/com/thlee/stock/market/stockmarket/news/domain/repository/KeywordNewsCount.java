package com.thlee.stock.market.stockmarket.news.domain.repository;

import java.time.LocalDateTime;

/**
 * 키워드별 뉴스 합계 집계 행 (#115).
 *
 * <p>{@code lastCollectedAt} 은 {@code MAX(news.created_at)} 이라
 * "마지막으로 <b>기사가 저장된</b> 시각"이다. 수집을 시도했지만 새 기사가 없었던 경우는 반영되지 않는다.
 * 수집 이력 테이블(Phase 6)이 들어오면 이력 기준으로 대체한다.
 */
public record KeywordNewsCount(
        Long keywordId,
        long totalCount,
        LocalDateTime lastCollectedAt
) {
}
