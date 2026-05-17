package com.thlee.stock.market.stockmarket.newsjournal.application.dto;

import com.thlee.stock.market.stockmarket.newsjournal.domain.model.EventImpact;

import java.time.LocalDate;
import java.util.List;

/**
 * 사건 수정 application command. {@code id} + {@code userId} 로 소유권 검증 후 본문 + 링크 + 키워드를 갱신한다.
 * {@code categoryName} 은 사용자가 입력한 분류명 (find-or-create 대상).
 * {@code keywords} 는 해시태그형 키워드 문자열 목록 (displayOrder 는 WriteService 가 인덱스로 부여).
 */
public record UpdateNewsEventCommand(
        Long id,
        Long userId,
        String title,
        LocalDate occurredDate,
        EventImpact impact,
        String categoryName,
        String what,
        String why,
        String how,
        List<NewsEventLinkCommand> links,
        List<String> keywords
) { }