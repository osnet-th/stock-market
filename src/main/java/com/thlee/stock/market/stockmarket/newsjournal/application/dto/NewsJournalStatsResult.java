package com.thlee.stock.market.stockmarket.newsjournal.application.dto;

import com.thlee.stock.market.stockmarket.newsjournal.domain.model.EventImpact;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 뉴스 기록 화면 통계 결과(application 결과 DTO).
 *
 * <ul>
 *   <li>{@code totalCount} — 사용자 전체 사건 수 (필터 무관)</li>
 *   <li>{@code impactCounts} — 시장영향별 건수. 0건 임팩트도 키 포함</li>
 *   <li>{@code categoryCounts} — 분류별 건수(이름순). 0건 분류 포함</li>
 *   <li>{@code keywordEvents} — 키워드를 가진 사건별 키워드 목록.
 *       빈도·동시 등장·추천·관계도 계산의 원자료 (발생일 내림차순)</li>
 * </ul>
 */
public record NewsJournalStatsResult(
        long totalCount,
        Map<EventImpact, Long> impactCounts,
        List<CategoryCountItem> categoryCounts,
        List<KeywordEventItem> keywordEvents
) {

    /** 분류 건수 항목 (0건 분류 포함). */
    public record CategoryCountItem(Long categoryId, String name, long count) { }

    /** 사건 1건의 키워드 목록 (displayOrder 순). */
    public record KeywordEventItem(Long eventId, LocalDate occurredDate, List<String> keywords) { }
}
