package com.thlee.stock.market.stockmarket.news.presentation;

import com.thlee.stock.market.stockmarket.news.application.KeywordService;
import com.thlee.stock.market.stockmarket.news.application.dto.KeywordResponse;
import com.thlee.stock.market.stockmarket.news.domain.model.Keyword;
import com.thlee.stock.market.stockmarket.news.application.dto.RegisterKeywordRequest;
import lombok.RequiredArgsConstructor;
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
}
