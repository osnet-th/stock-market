package com.thlee.stock.market.stockmarket.newsjournal.presentation.dto;

import com.thlee.stock.market.stockmarket.newsjournal.application.dto.NewsEventDetailResult;
import com.thlee.stock.market.stockmarket.newsjournal.domain.model.EventCategory;
import com.thlee.stock.market.stockmarket.newsjournal.domain.model.NewsEvent;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 사건 상세 Response. event + links 를 평탄화하여 반환.
 */
public record NewsEventDetailResponse(
        Long id,
        Long userId,
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

    public static NewsEventDetailResponse from(NewsEventDetailResult r) {
        NewsEvent e = r.event();
        return new NewsEventDetailResponse(
                e.getId(), e.getUserId(), e.getTitle(), e.getOccurredDate(), e.getCategory(),
                e.getWhat(), e.getWhy(), e.getHow(),
                r.links().stream().map(NewsEventLinkDto::from).toList(),
                e.getCreatedAt(), e.getUpdatedAt()
        );
    }
}