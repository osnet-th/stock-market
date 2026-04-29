package com.thlee.stock.market.stockmarket.newsjournal.domain.repository;

import com.thlee.stock.market.stockmarket.newsjournal.domain.model.EventImpact;

import java.time.LocalDate;

/**
 * 사건 리스트 필터 value. 모든 필드는 nullable (null = 해당 필터 비활성).
 * page 는 0-base, size 는 1~200 범위.
 */
public record NewsEventListFilter(
        EventImpact impact,
        Long categoryId,
        LocalDate fromDate,
        LocalDate toDate,
        int page,
        int size
) {
    public NewsEventListFilter {
        if (page < 0) {
            throw new IllegalArgumentException("page 는 0 이상이어야 합니다.");
        }
        if (size <= 0 || size > 200) {
            throw new IllegalArgumentException("size 는 1~200 범위여야 합니다.");
        }
    }
}