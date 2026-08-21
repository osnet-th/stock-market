package com.thlee.stock.market.stockmarket.newsjournal.presentation;

import com.thlee.stock.market.stockmarket.newsjournal.application.NewsEventReadService;
import com.thlee.stock.market.stockmarket.newsjournal.presentation.dto.NewsJournalStatsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 뉴스 기록 화면 통계 REST API.
 *
 * <p>{@link NewsJournalController}({@code /api/news-journal/events}) ·
 * {@link NewsJournalDashboardController}({@code /api/news-journal/dashboard}) 와 매핑이
 * 분리되어 sibling 경로 충돌이 없다. userId 는 {@link NewsJournalSecurityContext} 로
 * 안전하게 추출한다 (전 건 사용자 스코프).
 */
@RestController
@RequestMapping("/api/news-journal/stats")
@RequiredArgsConstructor
public class NewsJournalStatsController {

    private final NewsEventReadService readService;

    @GetMapping
    public ResponseEntity<NewsJournalStatsResponse> stats() {
        Long userId = NewsJournalSecurityContext.currentUserId();
        return ResponseEntity.ok(NewsJournalStatsResponse.from(readService.findStats(userId)));
    }
}
