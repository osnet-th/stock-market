package com.thlee.stock.market.stockmarket.newsjournal.presentation.dto;

import com.thlee.stock.market.stockmarket.newsjournal.application.dto.NewsEventListItemResult;
import com.thlee.stock.market.stockmarket.newsjournal.application.dto.NewsEventListResult;
import com.thlee.stock.market.stockmarket.newsjournal.domain.model.EventCategory;
import com.thlee.stock.market.stockmarket.newsjournal.domain.model.NewsEvent;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 사건 리스트 Response (타임라인 화면용). 각 항목은 본체 + 링크 평탄화.
 */
public record NewsEventListResponse(
        List<ItemDto> items,
        long totalCount,
        int page,
        int size
) {

    public static NewsEventListResponse from(NewsEventListResult r) {
        return new NewsEventListResponse(
                r.items().stream().map(ItemDto::from).toList(),
                r.totalCount(), r.page(), r.size()
        );
    }

    public record ItemDto(
            Long id,
            String title,
            LocalDate occurredDate,
            EventCategory category,
            String what,
            String why,
            String how,
            List<NewsEventLinkDto> links,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        public static ItemDto from(NewsEventListItemResult i) {
            NewsEvent e = i.event();
            return new ItemDto(
                    e.getId(), e.getTitle(), e.getOccurredDate(), e.getCategory(),
                    e.getWhat(), e.getWhy(), e.getHow(),
                    i.links().stream().map(NewsEventLinkDto::from).toList(),
                    e.getCreatedAt(), e.getUpdatedAt()
            );
        }
    }
}