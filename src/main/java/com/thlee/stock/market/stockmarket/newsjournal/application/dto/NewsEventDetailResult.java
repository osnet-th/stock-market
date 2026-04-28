package com.thlee.stock.market.stockmarket.newsjournal.application.dto;

import com.thlee.stock.market.stockmarket.newsjournal.domain.model.NewsEvent;
import com.thlee.stock.market.stockmarket.newsjournal.domain.model.NewsEventLink;

import java.util.List;

/**
 * 사건 상세 조회 결과. 본체 + 링크 리스트(displayOrder 정렬) 동봉.
 */
public record NewsEventDetailResult(
        NewsEvent event,
        List<NewsEventLink> links
) { }