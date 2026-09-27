package com.thlee.stock.market.stockmarket.news.domain.model;

import java.time.LocalDate;
import java.util.List;

/**
 * 뉴스 검색 조건 (#115).
 *
 * <p>Phase 2 에서 검색 파라미터가 6개 → 9개로 늘어 위치 인자로는 호출부를 읽을 수 없게 되었다.
 * 조건을 한 덩어리로 묶어 전달한다.
 *
 * <p>{@code unreadOnly} 는 사용자별 읽음 상태에 걸리는 조건이라 조회 계층에서 사용자 컨텍스트가 필요하다.
 *
 * <p>{@code keywordIds} 는 <b>항상 사용자가 구독 중인 키워드로 해석된 결과</b>다.
 * 컨트롤러가 요청값을 검증(내 구독인지)하거나, 요청에 없으면 내 전체 구독으로 채워서 넘긴다.
 * 즉 이 객체가 도메인에 도달한 시점에는 이미 소유권이 확정되어 있다.
 */
public record NewsSearchCriteria(
        String query,
        List<Long> keywordIds,
        LocalDate startDate,
        LocalDate endDate,
        Region region,
        NewsSearchField field,
        NewsSearchSort sort,
        boolean unreadOnly,
        int page,
        int size
) {

    /**
     * 검색어가 있으면 전문 검색(ES), 없으면 DB 최신순 조회로 갈린다.
     */
    public boolean hasQuery() {
        return query != null && !query.isBlank();
    }

    /**
     * 조회 대상 키워드가 없으면 결과가 있을 수 없다 (키워드를 하나도 등록하지 않은 사용자).
     */
    public boolean hasNoKeywordScope() {
        return keywordIds == null || keywordIds.isEmpty();
    }

    /**
     * 검색어가 없으면 관련도 정렬은 의미가 없다 — 최신순으로 고정한다.
     */
    public NewsSearchSort effectiveSort() {
        if (!hasQuery()) {
            return NewsSearchSort.LATEST;
        }
        return sort == null ? NewsSearchSort.LATEST : sort;
    }

    public NewsSearchField effectiveField() {
        return field == null ? NewsSearchField.TITLE_CONTENT : field;
    }
}
