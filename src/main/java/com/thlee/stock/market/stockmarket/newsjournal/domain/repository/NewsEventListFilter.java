package com.thlee.stock.market.stockmarket.newsjournal.domain.repository;

import com.thlee.stock.market.stockmarket.newsjournal.domain.model.EventImpact;

import java.time.LocalDate;
import java.util.List;

/**
 * 사건 리스트 필터 value. 모든 필드는 nullable (null = 해당 필터 비활성).
 * page 는 0-base, size 는 1~200 범위.
 *
 * <ul>
 *   <li>{@code query} — 통합 검색어. 제목/WHAT/WHY/HOW 본문 + 키워드 + 분류명을
 *       대소문자 무시 부분 일치로 검색한다. blank 는 null 로 정규화.</li>
 *   <li>{@code keywords} — 키워드 필터. 나열된 키워드를 <b>모두</b> 가진 사건만 매칭(AND).
 *       null 은 빈 리스트로 정규화. 항목당 정확 일치.</li>
 * </ul>
 */
public record NewsEventListFilter(
        EventImpact impact,
        Long categoryId,
        LocalDate fromDate,
        LocalDate toDate,
        String query,
        List<String> keywords,
        int page,
        int size
) {
    /** 키워드 필터 상한 — 화면 다중 선택 상한과 동일하게 방어. */
    public static final int KEYWORDS_MAX = 10;

    public NewsEventListFilter {
        if (page < 0) {
            throw new IllegalArgumentException("page 는 0 이상이어야 합니다.");
        }
        if (size <= 0 || size > 200) {
            throw new IllegalArgumentException("size 는 1~200 범위여야 합니다.");
        }
        query = (query == null || query.isBlank()) ? null : query.trim();
        keywords = keywords == null ? List.of() : keywords.stream()
                .filter(k -> k != null && !k.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
        if (keywords.size() > KEYWORDS_MAX) {
            throw new IllegalArgumentException("keywords 는 최대 " + KEYWORDS_MAX + "개까지 허용됩니다.");
        }
    }
}
