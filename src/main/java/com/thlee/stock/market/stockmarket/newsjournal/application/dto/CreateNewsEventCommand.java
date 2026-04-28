package com.thlee.stock.market.stockmarket.newsjournal.application.dto;

import com.thlee.stock.market.stockmarket.newsjournal.domain.model.EventCategory;

import java.time.LocalDate;
import java.util.List;

/**
 * 사건 생성 application command. presentation Request 에서 매핑된 뒤 WriteService 가 소비한다.
 */
public record CreateNewsEventCommand(
        Long userId,
        String title,
        LocalDate occurredDate,
        EventCategory category,
        String what,
        String why,
        String how,
        List<NewsEventLinkCommand> links
) { }