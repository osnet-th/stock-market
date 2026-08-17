package com.thlee.stock.market.stockmarket.news.application.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 홈 대시보드 우측 패널의 "내 키워드 뉴스" 응답 (#114).
 *
 * <p>기존 {@code GET /api/news} 는 {@code keywordId} 가 필수라 키워드 수만큼 호출해야 했다.
 * 이 응답은 사용자의 활성 키워드 전체를 한 번에 합쳐 최신순으로 돌려준다.
 *
 * @param keywordCount 활성 키워드 수
 * @param todayCount   오늘(서버 로컬 자정 기준) 수집된 뉴스 건수
 * @param items        최신순 뉴스. 요청 size 만큼
 */
public record KeywordNewsFeedResponse(
        int keywordCount,
        long todayCount,
        List<Item> items
) {

    public static KeywordNewsFeedResponse empty() {
        return new KeywordNewsFeedResponse(0, 0L, List.of());
    }

    /**
     * 언론사(source)는 News 도메인에 없는 값이라 담지 않는다 — 목업 패널도 키워드·시각·제목만 쓴다.
     *
     * @param keyword 뉴스가 속한 키워드 이름. 매칭되는 키워드가 없으면 null
     */
    public record Item(
            Long id,
            String keyword,
            String title,
            String originalUrl,
            LocalDateTime publishedAt
    ) {
    }
}
