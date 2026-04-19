package com.thlee.stock.market.stockmarket.news.domain.service;

import com.thlee.stock.market.stockmarket.news.domain.model.News;

import java.util.List;

/**
 * 뉴스 검색 인덱싱 포트
 */
public interface NewsIndexPort {

    /**
     * @return 실제 ES에 인덱싱된 건수 (실패 시 0)
     */
    int indexAll(List<News> newsList);
}