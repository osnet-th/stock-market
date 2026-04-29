package com.thlee.stock.market.stockmarket.newsjournal.application.dto;

import com.thlee.stock.market.stockmarket.newsjournal.domain.model.EventImpact;

import java.time.LocalDate;
import java.util.List;

/**
 * 사건 생성 application command. presentation Request 에서 매핑된 뒤 WriteService 가 소비한다.
 * {@code categoryName} 은 사용자가 입력한 분류명 (find-or-create 대상).
 */
public record CreateNewsEventCommand(
        Long userId,
        String title,
        LocalDate occurredDate,
        EventImpact impact,
        String categoryName,
        String what,
        String why,
        String how,
        List<NewsEventLinkCommand> links
) { }