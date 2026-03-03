package com.thlee.stock.market.stockmarket.common.response;

import lombok.Getter;

import java.util.List;

/**
 * 순수 Java 페이징 결과 래퍼 (Spring 의존성 없음)
 */
@Getter
public class PageResult<T> {
    private final List<T> content;
    private final int page;
    private final int size;
    private final long totalElements;
    private final int totalPages;

    public PageResult(List<T> content, int page, int size, long totalElements) {
        this.content = content;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = size > 0 ? (int) Math.ceil((double) totalElements / size) : 0;
    }
}