package com.thlee.stock.market.stockmarket.news.presentation;

import com.thlee.stock.market.stockmarket.common.response.PageResult;
import com.thlee.stock.market.stockmarket.news.application.NewsSearchApplicationService;
import com.thlee.stock.market.stockmarket.news.application.dto.NewsDto;
import com.thlee.stock.market.stockmarket.news.presentation.dto.NewsSearchRequest;
import com.thlee.stock.market.stockmarket.news.presentation.dto.NewsSearchResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 뉴스 전문 검색 API
 */
@RestController
@RequestMapping("/api/news/search")
@RequiredArgsConstructor
public class NewsSearchController {

    private final NewsSearchApplicationService newsSearchApplicationService;

    @GetMapping
    public ResponseEntity<NewsSearchResponse<NewsDto>> search(@Valid NewsSearchRequest request) {
        PageResult<NewsDto> result = newsSearchApplicationService.search(
                request.getQuery(),
                request.getStartDate(),
                request.getEndDate(),
                request.getRegion(),
                request.getPage(),
                request.getSize()
        );
        return ResponseEntity.ok(NewsSearchResponse.from(result));
    }
}