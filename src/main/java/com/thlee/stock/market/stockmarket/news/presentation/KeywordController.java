package com.thlee.stock.market.stockmarket.news.presentation;

import com.thlee.stock.market.stockmarket.news.application.KeywordService;
import com.thlee.stock.market.stockmarket.news.application.dto.KeywordResponse;
import com.thlee.stock.market.stockmarket.news.application.dto.KeywordStatsResponse;
import com.thlee.stock.market.stockmarket.news.domain.model.Keyword;
import com.thlee.stock.market.stockmarket.news.application.dto.RegisterKeywordRequest;
import com.thlee.stock.market.stockmarket.news.application.dto.BulkKeywordRequest;
import com.thlee.stock.market.stockmarket.news.application.dto.UpdateKeywordRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 키워드 관련 HTTP 엔드포인트
 */
@RestController
@RequestMapping("/api/keywords")
@RequiredArgsConstructor
public class KeywordController {

    private final KeywordService keywordService;

    /**
     * 키워드 등록
     */
    @PostMapping
    public ResponseEntity<Keyword> registerKeyword(@RequestBody RegisterKeywordRequest request) {
        Keyword keyword = keywordService.registerKeyword(request);
        return ResponseEntity.ok(keyword);
    }

    /**
     * 사용자별 키워드 목록 조회
     */
    @GetMapping
    public ResponseEntity<List<KeywordResponse>> getKeywords(
            @RequestParam Long userId,
            @RequestParam(required = false) Boolean active
    ) {
        List<KeywordResponse> keywords;
        if (active == null) {
            keywords = keywordService.getKeywordsByUser(userId);
        } else if (active) {
            keywords = keywordService.getActiveKeywordsByUser(userId);
        } else {
            keywords = keywordService.getKeywordsByUser(userId);
        }
        return ResponseEntity.ok(keywords);
    }

    /**
     * 키워드 레일 통계 조회 (#115) — 총 건수 · 오늘 수집 · 마지막 성공 · 7일 스파크라인.
     *
     * <p>어떤 키워드를 얼마나 보고 있는지는 사용자 정보이므로 인증 주체와 일치하는지 확인한다 (#114 H1).
     */
    @GetMapping("/stats")
    public ResponseEntity<KeywordStatsResponse> getKeywordStats(@RequestParam Long userId) {
        if (!NewsSecurityContext.matchesCurrentUser(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(keywordService.getKeywordStats(userId));
    }

    /**
     * 사용자의 키워드 구독 활성화
     */
    @PatchMapping("/{keywordId}/activate")
    public ResponseEntity<Void> activateKeyword(
            @PathVariable Long keywordId,
            @RequestParam Long userId
    ) {
        keywordService.activateUserKeyword(userId, keywordId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 사용자의 키워드 구독 비활성화
     */
    @PatchMapping("/{keywordId}/deactivate")
    public ResponseEntity<Void> deactivateKeyword(
            @PathVariable Long keywordId,
            @RequestParam Long userId
    ) {
        keywordService.deactivateUserKeyword(userId, keywordId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 사용자의 키워드 구독 해제
     */
    @DeleteMapping("/{keywordId}")
    public ResponseEntity<Void> deleteKeyword(
            @PathVariable Long keywordId,
            @RequestParam Long userId
    ) {
        keywordService.unsubscribeKeyword(userId, keywordId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 키워드 수정 (#115) — 이름 · 수집 범위 · 활성 상태.
     *
     * <p>{@code Keyword} 가 공유 리소스라 UPDATE 가 아닌 <b>재구독</b>으로 처리된다.
     * 그래서 이름·지역이 바뀌면 <b>응답의 id 가 요청 경로의 id 와 다르다</b> —
     * 호출부는 응답 id 로 선택 상태를 갱신해야 한다.
     */
    @PutMapping("/{keywordId}")
    public ResponseEntity<KeywordResponse> updateKeyword(
            @PathVariable Long keywordId,
            @RequestParam Long userId,
            @Valid @RequestBody UpdateKeywordRequest request
    ) {
        if (!NewsSecurityContext.matchesCurrentUser(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(keywordService.updateKeyword(userId, keywordId, request));
    }

    /**
     * 선택 키워드 일괄 비활성화 (#115 레일 벌크 `중단`).
     */
    @PatchMapping("/bulk/deactivate")
    public ResponseEntity<Void> bulkDeactivate(
            @RequestParam Long userId,
            @Valid @RequestBody BulkKeywordRequest request
    ) {
        if (!NewsSecurityContext.matchesCurrentUser(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        keywordService.deactivateUserKeywords(userId, request.getKeywordIds());
        return ResponseEntity.noContent().build();
    }

    /**
     * 선택 키워드 일괄 구독 해제 (#115 레일 벌크 `삭제`).
     *
     * <p>DELETE 는 본문을 싣기 어려워 POST 로 둔다.
     */
    @PostMapping("/bulk/delete")
    public ResponseEntity<Void> bulkDelete(
            @RequestParam Long userId,
            @Valid @RequestBody BulkKeywordRequest request
    ) {
        if (!NewsSecurityContext.matchesCurrentUser(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        keywordService.unsubscribeKeywords(userId, request.getKeywordIds());
        return ResponseEntity.noContent().build();
    }
}
