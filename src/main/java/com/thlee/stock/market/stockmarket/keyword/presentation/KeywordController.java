package com.thlee.stock.market.stockmarket.keyword.presentation;

import com.thlee.stock.market.stockmarket.keyword.application.KeywordService;
import com.thlee.stock.market.stockmarket.keyword.domain.model.Keyword;
import com.thlee.stock.market.stockmarket.keyword.presentation.dto.RegisterKeywordRequest;
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
     *
     * @param request 키워드 등록 요청 (keyword, userId)
     * @return 등록된 키워드
     */
    @PostMapping
    public ResponseEntity<Keyword> registerKeyword(@RequestBody RegisterKeywordRequest request) {
        Keyword keyword = keywordService.registerKeyword(request.getKeyword(), request.getUserId());
        return ResponseEntity.ok(keyword);
    }

    /**
     * 키워드 목록 조회
     *
     * @param userId 사용자 ID (필수)
     * @param active 활성화 여부 (선택적)
     * @return 키워드 목록
     */
    @GetMapping
    public ResponseEntity<List<Keyword>> getKeywords(
            @RequestParam Long userId,
            @RequestParam(required = false) Boolean active
    ) {
        List<Keyword> keywords;
        if (active == null) {
            keywords = keywordService.getKeywordsByUserId(userId);
        } else if (active) {
            keywords = keywordService.getActiveKeywordsByUserId(userId);
        } else {
            keywords = keywordService.getKeywordsByUserId(userId).stream()
                    .filter(k -> !k.isActive())
                    .toList();
        }
        return ResponseEntity.ok(keywords);
    }

    /**
     * 키워드 활성화
     *
     * @param keywordId 키워드 ID
     * @return 성공 응답
     */
    @PatchMapping("/{keywordId}/activate")
    public ResponseEntity<Void> activateKeyword(@PathVariable Long keywordId) {
        keywordService.activateKeyword(keywordId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 키워드 비활성화
     *
     * @param keywordId 키워드 ID
     * @return 성공 응답
     */
    @PatchMapping("/{keywordId}/deactivate")
    public ResponseEntity<Void> deactivateKeyword(@PathVariable Long keywordId) {
        keywordService.deactivateKeyword(keywordId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 키워드 삭제
     *
     * @param keywordId 키워드 ID
     * @return 성공 응답
     */
    @DeleteMapping("/{keywordId}")
    public ResponseEntity<Void> deleteKeyword(@PathVariable Long keywordId) {
        keywordService.deleteKeyword(keywordId);
        return ResponseEntity.noContent().build();
    }
}
