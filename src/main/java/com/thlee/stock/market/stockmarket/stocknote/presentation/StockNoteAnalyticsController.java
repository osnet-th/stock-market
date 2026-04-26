package com.thlee.stock.market.stockmarket.stocknote.presentation;

import com.thlee.stock.market.stockmarket.stocknote.application.StockNoteChartService;
import com.thlee.stock.market.stockmarket.stocknote.application.StockNoteDashboardService;
import com.thlee.stock.market.stockmarket.stocknote.application.StockNotePatternMatchService;
import com.thlee.stock.market.stockmarket.stocknote.application.dto.DashboardResult;
import com.thlee.stock.market.stockmarket.stocknote.application.dto.SimilarPatternResult;
import com.thlee.stock.market.stockmarket.stocknote.domain.model.enums.NoteDirection;
import com.thlee.stock.market.stockmarket.stocknote.presentation.dto.ChartDataResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * stocknote 분석 API: 대시보드 / 유사 패턴 / 종목 차트.
 */
@RestController
@RequestMapping("/api/stock-notes")
@RequiredArgsConstructor
public class StockNoteAnalyticsController {

    private final StockNoteDashboardService dashboardService;
    private final StockNotePatternMatchService patternMatchService;
    private final StockNoteChartService chartService;

    @GetMapping("/dashboard")
    public ResponseEntity<DashboardResult> dashboard() {
        return ResponseEntity.ok(
                dashboardService.getDashboard(StockNoteSecurityContext.currentUserId()));
    }

    @GetMapping("/{id}/similar-patterns")
    public ResponseEntity<SimilarPatternResult> similarPatterns(
            @PathVariable Long id,
            @RequestParam(required = false) NoteDirection directionFilter
    ) {
        return ResponseEntity.ok(
                patternMatchService.findSimilar(id, StockNoteSecurityContext.currentUserId(), directionFilter));
    }

    @GetMapping("/by-stock/{stockCode}/chart")
    public ResponseEntity<ChartDataResponse> chart(
            @PathVariable String stockCode,
            @RequestParam(required = false) Integer period
    ) {
        return ResponseEntity.ok(ChartDataResponse.from(
                chartService.getChartData(StockNoteSecurityContext.currentUserId(), stockCode, period)));
    }
}