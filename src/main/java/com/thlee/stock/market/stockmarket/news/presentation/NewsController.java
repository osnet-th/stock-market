package com.thlee.stock.market.stockmarket.news.presentation;

import com.thlee.stock.market.stockmarket.common.response.PageResult;
import com.thlee.stock.market.stockmarket.news.application.KeywordNewsBatchService;
import com.thlee.stock.market.stockmarket.news.application.NewsQueryService;
import com.thlee.stock.market.stockmarket.news.application.dto.NewsBatchSaveResult;
import com.thlee.stock.market.stockmarket.news.application.dto.NewsDto;
import com.thlee.stock.market.stockmarket.news.domain.model.NewsPurpose;
import com.thlee.stock.market.stockmarket.news.presentation.dto.NewsCollectRequest;
import com.thlee.stock.market.stockmarket.news.presentation.dto.NewsCollectResponse;
import com.thlee.stock.market.stockmarket.news.presentation.dto.NewsQueryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 뉴스 조회 API
 */
@RestController
@RequestMapping("/api/news")
@RequiredArgsConstructor
public class NewsController {

    private final NewsQueryService newsQueryService;
    private final KeywordNewsBatchService keywordNewsBatchService;

    /**
     * 뉴스 조회 (페이징)
     * purpose + sourceId로 범용 조회
     */
    @GetMapping
    public ResponseEntity<NewsQueryResponse<NewsDto>> getNews(
            @RequestParam NewsPurpose purpose,
            @RequestParam Long sourceId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PageResult<NewsDto> result = newsQueryService.getNewsBySource(
                purpose, sourceId, page, size);
        return ResponseEntity.ok(NewsQueryResponse.from(result));
    }

    /**
     * 뉴스 즉시 수집 (purpose + sourceId 범용)
     */
    @PostMapping("/collect")
    public ResponseEntity<NewsCollectResponse> collectNews(@RequestBody NewsCollectRequest request) {
        NewsBatchSaveResult result = keywordNewsBatchService.collectBySource(
                request.getPurpose(),
                request.getSourceId(),
                request.getKeyword(),
                request.getUserId(),
                request.getRegion()
        );
        return ResponseEntity.ok(NewsCollectResponse.from(result));
    }
}