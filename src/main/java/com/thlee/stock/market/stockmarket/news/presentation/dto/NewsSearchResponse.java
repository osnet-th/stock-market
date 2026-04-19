package com.thlee.stock.market.stockmarket.news.presentation.dto;

import com.thlee.stock.market.stockmarket.common.response.PageResult;
import lombok.Getter;

import java.util.List;

/**
 * 뉴스 전문 검색 페이징 응답 DTO
 */
@Getter
public class NewsSearchResponse<T> {
    private final List<T> content;
    private final int page;
    private final int size;
    private final long totalElements;
    private final int totalPages;

    private NewsSearchResponse(List<T> content, int page, int size, long totalElements, int totalPages) {
        this.content = content;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
    }

    public static <T> NewsSearchResponse<T> from(PageResult<T> pageResult) {
        return new NewsSearchResponse<>(
                pageResult.getContent(),
                pageResult.getPage(),
                pageResult.getSize(),
                pageResult.getTotalElements(),
                pageResult.getTotalPages()
        );
    }
}