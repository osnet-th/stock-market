package com.thlee.stock.market.stockmarket.news.domain.service;

import com.thlee.stock.market.stockmarket.common.response.PageResult;
import com.thlee.stock.market.stockmarket.news.domain.model.News;
import com.thlee.stock.market.stockmarket.news.domain.model.NewsSearchCriteria;

/**
 * 뉴스 전문 검색 포트
 *
 * <p>#115 에서 조건이 6개 → 9개로 늘어 {@link NewsSearchCriteria} 로 묶었다.
 * 이 포트는 <b>검색어가 있을 때만</b> 호출된다 — 검색어가 없는 조회는 정렬 안정성 때문에
 * ES 를 거치지 않고 DB 최신순으로 처리한다.
 */
public interface NewsFullTextSearchPort {

    PageResult<News> search(NewsSearchCriteria criteria);
}
