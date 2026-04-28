package com.thlee.stock.market.stockmarket.newsjournal.application.dto;

import java.util.List;

/**
 * 사건 리스트 페이징 결과.
 */
public record NewsEventListResult(
        List<NewsEventListItemResult> items,
        long totalCount,
        int page,
        int size
) { }