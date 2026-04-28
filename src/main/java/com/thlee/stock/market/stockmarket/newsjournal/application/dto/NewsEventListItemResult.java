package com.thlee.stock.market.stockmarket.newsjournal.application.dto;

import com.thlee.stock.market.stockmarket.newsjournal.domain.model.NewsEvent;
import com.thlee.stock.market.stockmarket.newsjournal.domain.model.NewsEventLink;

import java.util.List;

/**
 * 타임라인 리스트의 한 항목. 본체 + 링크 리스트(displayOrder 정렬)를 N+1 없이 한 번에 반환.
 */
public record NewsEventListItemResult(
        NewsEvent event,
        List<NewsEventLink> links
) { }