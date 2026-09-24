package com.thlee.stock.market.stockmarket.news.presentation;

import com.thlee.stock.market.stockmarket.common.response.PageResult;
import com.thlee.stock.market.stockmarket.news.application.KeywordService;
import com.thlee.stock.market.stockmarket.news.application.NewsSearchApplicationService;
import com.thlee.stock.market.stockmarket.news.application.dto.NewsDto;
import com.thlee.stock.market.stockmarket.news.domain.model.NewsSearchCriteria;
import com.thlee.stock.market.stockmarket.news.presentation.dto.NewsSearchRequest;
import com.thlee.stock.market.stockmarket.news.presentation.dto.NewsSearchResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 뉴스 전문 검색 API
 *
 * <p>#115 에서 검색이 <b>사용자 구독 키워드로 스코프</b>된다.
 * 통합 화면의 뉴스 스트림이 이 엔드포인트 하나로 동작하고,
 * 검색어가 없으면 "저장된 전체 뉴스"(= 내 키워드 전체)를 최신순으로 돌려준다.
 *
 * <p>스코프가 사용자별로 갈리므로 인증이 필요해졌다 —
 * 이전에는 {@code permitAll} 로 비로그인 전역 검색이 가능했다.
 */
@RestController
@RequestMapping("/api/news/search")
@RequiredArgsConstructor
public class NewsSearchController {

    private final NewsSearchApplicationService newsSearchApplicationService;
    private final KeywordService keywordService;

    @GetMapping
    public ResponseEntity<NewsSearchResponse<NewsDto>> search(
            @RequestParam Long userId,
            @Valid NewsSearchRequest request
    ) {
        if (!NewsSecurityContext.matchesCurrentUser(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        List<Long> scope = resolveKeywordScope(userId, request.getKeywordIds());
        if (scope == null) {
            // 내 구독이 아닌 키워드를 요청했다 — 남의 키워드 뉴스를 열람할 경로를 막는다 (#114 H1 과 같은 문제)
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        NewsSearchCriteria criteria = new NewsSearchCriteria(
                request.getQuery(),
                scope,
                request.getStartDate(),
                request.getEndDate(),
                request.getRegion(),
                request.getField(),
                request.getSort(),
                request.getPage(),
                request.getSize()
        );

        PageResult<NewsDto> result = newsSearchApplicationService.search(criteria);
        return ResponseEntity.ok(NewsSearchResponse.from(result));
    }

    /**
     * 요청한 키워드가 전부 내 구독인지 확인하고 조회 스코프를 확정한다.
     *
     * @return 확정된 키워드 id 목록. 내 구독이 아닌 id 가 섞여 있으면 {@code null}
     */
    private List<Long> resolveKeywordScope(Long userId, List<Long> requested) {
        List<Long> owned = keywordService.getSubscribedKeywordIds(userId);
        if (requested == null || requested.isEmpty()) {
            return owned;
        }
        if (!owned.containsAll(requested)) {
            return null;
        }
        return requested;
    }
}
