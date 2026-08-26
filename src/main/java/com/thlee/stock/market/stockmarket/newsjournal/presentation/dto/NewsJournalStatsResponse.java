package com.thlee.stock.market.stockmarket.newsjournal.presentation.dto;

import com.thlee.stock.market.stockmarket.newsjournal.application.dto.NewsJournalStatsResult;
import com.thlee.stock.market.stockmarket.newsjournal.domain.model.EventImpact;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 뉴스 기록 화면 통계 Response.
 *
 * <p>시장영향 세그먼트 뱃지 · 분류 드롭다운 건수 · 키워드 칩/추천 패널/관계도의 원자료.
 * 키워드 빈도와 동시 등장 계산은 {@code keywordEvents} 를 바탕으로 프론트가 수행한다
 * (다중 키워드 AND 선택의 연관 키워드 계산에 사건 단위 데이터가 필요).
 */
public record NewsJournalStatsResponse(
        long totalCount,
        Map<EventImpact, Long> impactCounts,
        List<CategoryCountDto> categories,
        List<KeywordEventDto> keywordEvents
) {

    public static NewsJournalStatsResponse from(NewsJournalStatsResult r) {
        return new NewsJournalStatsResponse(
                r.totalCount(),
                r.impactCounts(),
                r.categoryCounts().stream()
                        .map(c -> new CategoryCountDto(c.categoryId(), c.name(), c.count()))
                        .toList(),
                r.keywordEvents().stream()
                        .map(k -> new KeywordEventDto(k.eventId(), k.occurredDate(), k.keywords()))
                        .toList()
        );
    }

    /** 분류 건수 항목 (0건 분류 포함, 이름순). */
    public record CategoryCountDto(Long id, String name, long count) { }

    /** 사건 1건의 키워드 목록 (발생일 내림차순 그룹). */
    public record KeywordEventDto(Long eventId, LocalDate occurredDate, List<String> keywords) { }
}
