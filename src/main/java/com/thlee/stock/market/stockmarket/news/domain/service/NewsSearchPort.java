package com.thlee.stock.market.stockmarket.news.domain.service;

import com.thlee.stock.market.stockmarket.news.domain.model.NewsSearchResult;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 외부 뉴스 API 조회 포트
 */
public interface NewsSearchPort {
    List<NewsSearchResult> search(String keyword, LocalDateTime fromDateTime);
}
