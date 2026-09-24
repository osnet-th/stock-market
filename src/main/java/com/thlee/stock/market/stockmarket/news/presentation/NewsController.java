package com.thlee.stock.market.stockmarket.news.presentation;

import com.thlee.stock.market.stockmarket.common.response.PageResult;
import com.thlee.stock.market.stockmarket.news.application.CollectorScheduleService;
import com.thlee.stock.market.stockmarket.news.application.KeywordNewsBatchService;
import com.thlee.stock.market.stockmarket.news.application.UserNewsStateService;
import com.thlee.stock.market.stockmarket.news.application.NewsQueryService;
import com.thlee.stock.market.stockmarket.news.application.dto.CollectorScheduleResponse;
import com.thlee.stock.market.stockmarket.news.application.dto.KeywordNewsFeedResponse;
import com.thlee.stock.market.stockmarket.news.application.dto.NewsBatchSaveResult;
import com.thlee.stock.market.stockmarket.news.application.dto.NewsDto;
import com.thlee.stock.market.stockmarket.news.presentation.dto.MarkAllReadRequest;
import com.thlee.stock.market.stockmarket.news.presentation.dto.NewsCollectRequest;
import com.thlee.stock.market.stockmarket.news.presentation.dto.NewsCollectResponse;
import com.thlee.stock.market.stockmarket.news.presentation.dto.NewsQueryResponse;
import jakarta.validation.Valid;
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
    private final CollectorScheduleService collectorScheduleService;
    private final UserNewsStateService userNewsStateService;

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

    /**
     * 수집 스케줄 상태 조회 (#115 통합 화면 헤더).
     *
     * <p>마지막 실행은 수집 이력에서, 다음 실행은 스케줄러와 같은 cron 설정으로 계산한다.
     * 연속 실패 키워드 수는 사용자마다 다르므로 인증 주체와 일치하는지 확인한다 (#114 H1).
     */
    @GetMapping("/collector/schedule")
    public ResponseEntity<CollectorScheduleResponse> getCollectorSchedule(@RequestParam Long userId) {
        if (!NewsSecurityContext.matchesCurrentUser(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(collectorScheduleService.getSchedule(userId));
    }

    /**
     * 기사 읽음 처리 (#115) — 목업에서 카드를 열면 굵기가 풀리는 동작.
     */
    @PatchMapping("/{newsId}/read")
    public ResponseEntity<Void> markRead(@PathVariable Long newsId, @RequestParam Long userId) {
        if (!NewsSecurityContext.matchesCurrentUser(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        userNewsStateService.markRead(userId, newsId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 기사 저장(★) 토글 (#115).
     *
     * @return 토글 후 저장 여부
     */
    @PatchMapping("/{newsId}/save")
    public ResponseEntity<Boolean> toggleSaved(@PathVariable Long newsId, @RequestParam Long userId) {
        if (!NewsSecurityContext.matchesCurrentUser(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(userNewsStateService.toggleSaved(userId, newsId));
    }

    /**
     * 모두 읽음 (#115) — 현재 화면에 보이는 기사만 대상으로 한다.
     */
    @PatchMapping("/read-all")
    public ResponseEntity<Integer> markAllRead(@RequestParam Long userId,
                                               @Valid @RequestBody MarkAllReadRequest request) {
        if (!NewsSecurityContext.matchesCurrentUser(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(userNewsStateService.markAllRead(userId, request.getNewsIds()));
    }
}
