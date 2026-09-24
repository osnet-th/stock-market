package com.thlee.stock.market.stockmarket.news.domain.repository;

/**
 * 키워드별 뉴스 합계 집계 행 (#115).
 *
 * <p>"마지막 수집 성공" 은 여기서 다루지 않는다 — {@code MAX(news.created_at)} 은
 * 마지막으로 <b>기사가 저장된</b> 시각이라 새 기사가 없던 성공을 놓친다.
 * 그 값은 수집 이력({@link KeywordCollectionSummary})이 근거다.
 */
public record KeywordNewsCount(
        Long keywordId,
        long totalCount
) {
}
