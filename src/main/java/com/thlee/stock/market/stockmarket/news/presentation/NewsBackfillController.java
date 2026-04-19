package com.thlee.stock.market.stockmarket.news.presentation;

import com.thlee.stock.market.stockmarket.news.application.NewsBackfillService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 뉴스 백필 API (운영자 전용, 일회성)
 */
@RestController
@RequestMapping("/api/news/backfill")
@RequiredArgsConstructor
public class NewsBackfillController {

    private final NewsBackfillService newsBackfillService;

    @PostMapping
    public ResponseEntity<Map<String, Object>> backfill() {
        int totalIndexed = newsBackfillService.backfill();
        return ResponseEntity.ok(Map.of(
                "message", "백필 완료",
                "totalIndexed", totalIndexed
        ));
    }
}