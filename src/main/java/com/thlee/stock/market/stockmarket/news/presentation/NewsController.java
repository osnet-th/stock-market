package com.thlee.stock.market.stockmarket.news.presentation;

import com.thlee.stock.market.stockmarket.common.response.PageResult;
import com.thlee.stock.market.stockmarket.news.application.KeywordNewsBatchService;
import com.thlee.stock.market.stockmarket.news.application.NewsQueryService;
import com.thlee.stock.market.stockmarket.news.application.dto.NewsBatchSaveResult;
import com.thlee.stock.market.stockmarket.news.application.dto.NewsDto;
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
     * 키워드 기반 뉴스 조회 (페이징)
     */
    @GetMapping
    public ResponseEntity<NewsQueryResponse<NewsDto>> getNewsByKeyword(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PageResult<NewsDto> result = newsQueryService.getNewsByKeyword(keyword, page, size);
        return ResponseEntity.ok(NewsQueryResponse.from(result));
    }

    /**
     * 키워드 뉴스 즉시 수집
     */
    @PostMapping("/collect")
    public ResponseEntity<NewsCollectResponse> collectNews(@RequestBody NewsCollectRequest request) {
        NewsBatchSaveResult result = keywordNewsBatchService.collectByKeyword(
                request.getKeyword(),
                request.getUserId(),
                request.getRegion()
        );
        return ResponseEntity.ok(NewsCollectResponse.from(result));
    }
}