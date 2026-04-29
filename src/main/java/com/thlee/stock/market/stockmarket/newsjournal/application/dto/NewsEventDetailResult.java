package com.thlee.stock.market.stockmarket.newsjournal.application.dto;

import com.thlee.stock.market.stockmarket.newsjournal.domain.model.NewsEvent;
import com.thlee.stock.market.stockmarket.newsjournal.domain.model.NewsEventCategory;
import com.thlee.stock.market.stockmarket.newsjournal.domain.model.NewsEventLink;

import java.util.List;

/**
 * 사건 상세 조회 결과. 본체 + 카테고리 + 링크 리스트(displayOrder 정렬) 동봉.
 *
 * <p>{@code category} 는 backfill 직후 잔여 데이터에 한해 {@code null} 일 수 있다 (자바 nullable 설계).
 */
public record NewsEventDetailResult(
        NewsEvent event,
        NewsEventCategory category,
        List<NewsEventLink> links
) { }