package com.thlee.stock.market.stockmarket.news.domain.model;

/**
 * 뉴스 검색 정렬 기준 (#115).
 *
 * <p>목업의 `최신` / `관련도` 토글에 대응한다.
 * 검색어가 없으면 관련도 점수가 모두 같아 순서가 불안정하므로 {@link #LATEST} 로 강제된다.
 */
public enum NewsSearchSort {
    /** 발행일 최신순 (기본) */
    LATEST,
    /** 전문 검색 관련도순 */
    RELEVANCE
}
