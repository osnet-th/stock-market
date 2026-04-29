package com.thlee.stock.market.stockmarket.newsjournal.presentation.dto;

import com.thlee.stock.market.stockmarket.newsjournal.domain.model.NewsEventCategory;

/**
 * 사건 분류 응답 DTO. 카테고리 목록 / 사건 응답 동봉용.
 */
public record CategoryDto(Long id, String name) {

    public static CategoryDto from(NewsEventCategory c) {
        return new CategoryDto(c.getId(), c.getName());
    }
}