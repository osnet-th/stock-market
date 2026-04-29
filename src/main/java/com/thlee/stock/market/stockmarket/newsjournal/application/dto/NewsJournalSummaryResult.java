package com.thlee.stock.market.stockmarket.newsjournal.application.dto;

import com.thlee.stock.market.stockmarket.newsjournal.domain.model.NewsEvent;

import java.util.List;

/**
 * 뉴스 저널 대시보드 요약 결과(application 결과 DTO).
 *
 * <ul>
 *   <li>{@code recentEvents} — 최근 등록 N 건 (Service 가 limit 결정)</li>
 *   <li>{@code categoryCounts} — 사용자별 카테고리 ID/이름/건수 그룹 합계.
 *       카테고리가 삭제된 후 잔재 그룹은 Service 단계에서 제외된다.</li>
 * </ul>
 */
public record NewsJournalSummaryResult(
        List<NewsEvent> recentEvents,
        List<CategoryCountItem> categoryCounts
) {

    /** 카테고리명 메모리 join 후의 단일 카운트 항목. */
    public record CategoryCountItem(Long categoryId, String name, long count) { }
}