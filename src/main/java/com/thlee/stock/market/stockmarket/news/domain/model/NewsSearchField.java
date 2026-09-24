package com.thlee.stock.market.stockmarket.news.domain.model;

/**
 * 뉴스 검색 대상 필드 (#115).
 *
 * <p>목업의 `제목만` / `제목+본문` 토글에 대응한다.
 */
public enum NewsSearchField {
    /** 제목만 */
    TITLE,
    /** 제목 + 본문 (기본) */
    TITLE_CONTENT
}
