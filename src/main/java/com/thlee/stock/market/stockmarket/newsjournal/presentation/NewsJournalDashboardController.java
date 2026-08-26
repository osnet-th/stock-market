package com.thlee.stock.market.stockmarket.newsjournal.presentation;

import com.thlee.stock.market.stockmarket.newsjournal.application.NewsEventReadService;
import com.thlee.stock.market.stockmarket.newsjournal.application.dto.NewsJournalSummaryResult;
import com.thlee.stock.market.stockmarket.newsjournal.presentation.dto.NewsJournalSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 메인 대시보드의 뉴스 기록 카드용 요약 REST API.
 *
 * <p>기존 {@link NewsJournalController}({@code /api/news-journal/events}) 와 매핑이 분리되어 있어
 * sibling 경로 충돌 위험이 없다. userId 는 {@link NewsJournalSecurityContext} 로 안전하게 추출한다.
 */
@RestController
@RequestMapping("/api/news-journal/dashboard")
@RequiredArgsConstructor
public class NewsJournalDashboardController {

    /** 최근 등록 사건 카드에 표시할 항목 수. plan 결정 — 서버 고정. */
    private static final int RECENT_LIMIT = 3;

    private final NewsEventReadService readService;

    @GetMapping("/summary")
    public ResponseEntity<NewsJournalSummaryResponse> summary() {
        Long userId = NewsJournalSecurityContext.currentUserId();
        NewsJournalSummaryResult result = readService.findSummary(userId, RECENT_LIMIT);
        return ResponseEntity.ok(NewsJournalSummaryResponse.from(result));
    }
}