package com.thlee.stock.market.stockmarket.news.domain.service;

import com.thlee.stock.market.stockmarket.common.response.PageResult;
import com.thlee.stock.market.stockmarket.news.domain.model.News;
import com.thlee.stock.market.stockmarket.news.domain.model.Region;

import java.time.LocalDate;

/**
 * 뉴스 전문 검색 포트
 */
public interface NewsFullTextSearchPort {

    PageResult<News> search(String query, LocalDate startDate, LocalDate endDate,
                            Region region, int page, int size);
}