package com.thlee.stock.market.stockmarket.news.presentation;

import com.thlee.stock.market.stockmarket.common.response.PageResult;
import com.thlee.stock.market.stockmarket.news.application.KeywordNewsBatchService;
import com.thlee.stock.market.stockmarket.news.application.NewsQueryService;
import com.thlee.stock.market.stockmarket.news.application.dto.KeywordNewsFeedResponse;
import com.thlee.stock.market.stockmarket.news.application.dto.NewsBatchSaveResult;
import com.thlee.stock.market.stockmarket.news.application.dto.NewsDto;
import com.thlee.stock.market.stockmarket.news.presentation.dto.NewsCollectRequest;
import com.thlee.stock.market.stockmarket.news.presentation.dto.NewsCollectResponse;
import com.thlee.stock.market.stockmarket.news.presentation.dto.NewsQueryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
     * 뉴스 조회 (keywordId 기반, 페이징)
     */
    @GetMapping
    public ResponseEntity<NewsQueryResponse<NewsDto>> getNews(
            @RequestParam Long keywordId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PageResult<NewsDto> result = newsQueryService.getNewsByKeywordId(keywordId, page, size);
        return ResponseEntity.ok(NewsQueryResponse.from(result));
    }

    /**
     * 사용자 활성 키워드 전체를 합친 최신 뉴스 피드 (#114 홈 대시보드).
     * 키워드마다 GET /api/news 를 호출하지 않도록 서버에서 합쳐 준다.
     *
     * <p>키워드 목록은 사용자가 무엇을 보고 있는지 드러내므로, 파라미터 userId 가
     * 인증 주체와 같은지 확인한다 (#114 review H1).
     */
    @GetMapping("/feed")
    public ResponseEntity<KeywordNewsFeedResponse> getKeywordNewsFeed(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "5") int size
    ) {
        if (!NewsSecurityContext.matchesCurrentUser(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(newsQueryService.getKeywordNewsFeed(userId, size));
    }

    /**
     * 뉴스 즉시 수집
     */
    @PostMapping("/collect")
    public ResponseEntity<NewsCollectResponse> collectNews(@RequestBody NewsCollectRequest request) {
        NewsBatchSaveResult result = keywordNewsBatchService.collectByKeyword(
                request.getKeywordId(),
                request.getKeyword(),
                request.getRegion()
        );
        return ResponseEntity.ok(NewsCollectResponse.from(result));
    }
}
