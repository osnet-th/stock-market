package com.thlee.stock.market.stockmarket.newsjournal.application.dto;

import com.thlee.stock.market.stockmarket.newsjournal.domain.model.NewsEvent;
import com.thlee.stock.market.stockmarket.newsjournal.domain.model.NewsEventCategory;
import com.thlee.stock.market.stockmarket.newsjournal.domain.model.NewsEventLink;

import java.util.List;

/**
 * 타임라인 리스트의 한 항목. 본체 + 카테고리 + 링크 리스트(displayOrder 정렬)를 N+1 없이 한 번에 반환.
 *
 * <p>{@code category} 는 backfill 직후 잔여 데이터에 한해 {@code null} 일 수 있다.
 */
public record NewsEventListItemResult(
        NewsEvent event,
        NewsEventCategory category,
        List<NewsEventLink> links
) { }