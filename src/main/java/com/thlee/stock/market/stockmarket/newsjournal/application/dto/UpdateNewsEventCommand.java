package com.thlee.stock.market.stockmarket.newsjournal.application.dto;

import com.thlee.stock.market.stockmarket.newsjournal.domain.model.EventCategory;

import java.time.LocalDate;
import java.util.List;

/**
 * 사건 수정 application command. {@code id} + {@code userId} 로 소유권 검증 후 본문 + 링크를 갱신한다.
 */
public record UpdateNewsEventCommand(
        Long id,
        Long userId,
        String title,
        LocalDate occurredDate,
        EventCategory category,
        String what,
        String why,
        String how,
        List<NewsEventLinkCommand> links
) { }