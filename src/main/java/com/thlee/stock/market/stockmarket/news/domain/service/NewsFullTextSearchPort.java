package com.thlee.stock.market.stockmarket.news.domain.service;

import com.thlee.stock.market.stockmarket.common.response.PageResult;
import com.thlee.stock.market.stockmarket.news.domain.model.News;
import com.thlee.stock.market.stockmarket.news.domain.model.NewsSearchCriteria;

import java.util.List;

/**
 * 뉴스 전문 검색 포트
 *
 * <p>#115 에서 조건이 6개 → 9개로 늘어 {@link NewsSearchCriteria} 로 묶었다.
 * 이 포트는 <b>검색어가 있을 때만</b> 호출된다 — 검색어가 없는 조회는 정렬 안정성 때문에
 * ES 를 거치지 않고 DB 최신순으로 처리한다.
 */
public interface NewsFullTextSearchPort {

    PageResult<News> search(NewsSearchCriteria criteria);

    /**
     * {@code excludedUrls} 에 해당하는 문서를 빼고 검색한다 (#115 `안 읽은 것만`).
     *
     * <p>제외를 쿼리에 넣어야 전체 건수가 정확하다 — 조회 후 걸러내면 페이징이 어긋난다.
     */
    PageResult<News> search(NewsSearchCriteria criteria, List<String> excludedUrls);
}
