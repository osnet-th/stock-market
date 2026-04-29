package com.thlee.stock.market.stockmarket.newsjournal.domain.repository;

/**
 * 사용자별 카테고리 ID 와 사건 건수의 그룹 합계 결과(rollup projection).
 *
 * <p>group-by 쿼리에서 {@code SELECT new ...} constructor expression 으로 직접 매핑된다.
 * 카테고리명은 application 계층에서 {@link com.thlee.stock.market.stockmarket.newsjournal.domain.model.NewsEventCategory}
 * 와 메모리 join 으로 채운다.
 */
public record NewsEventCategoryCount(Long categoryId, long count) { }