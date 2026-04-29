package com.thlee.stock.market.stockmarket.newsjournal.presentation.dto;

import com.thlee.stock.market.stockmarket.newsjournal.application.dto.NewsJournalSummaryResult;
import com.thlee.stock.market.stockmarket.newsjournal.domain.model.NewsEvent;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 메인 대시보드 뉴스 기록 카드용 요약 응답.
 *
 * <ul>
 *   <li>{@code recentEvents} — 최근 등록 N건. 표시 라벨은 등록일({@code createdAt}).</li>
 *   <li>{@code categoryCounts} — 사용자별 카테고리/사건 건수.
 *       카테고리 이름이 매핑되지 않는 그룹(레거시 NULL/삭제 이력)은 application 단계에서 제외됨.</li>
 * </ul>
 */
public record NewsJournalSummaryResponse(
        List<RecentEventDto> recentEvents,
        List<CategoryCountDto> categoryCounts
) {

    public static NewsJournalSummaryResponse from(NewsJournalSummaryResult r) {
        return new NewsJournalSummaryResponse(
                r.recentEvents().stream().map(RecentEventDto::from).toList(),
                r.categoryCounts().stream().map(CategoryCountDto::from).toList()
        );
    }

    public record RecentEventDto(
            Long id,
            String title,
            LocalDate occurredDate,
            LocalDateTime createdAt
    ) {
        public static RecentEventDto from(NewsEvent e) {
            return new RecentEventDto(e.getId(), e.getTitle(), e.getOccurredDate(), e.getCreatedAt());
        }
    }

    public record CategoryCountDto(Long categoryId, String name, long count) {
        public static CategoryCountDto from(NewsJournalSummaryResult.CategoryCountItem i) {
            return new CategoryCountDto(i.categoryId(), i.name(), i.count());
        }
    }
}