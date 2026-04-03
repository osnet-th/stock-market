package com.thlee.stock.market.stockmarket.overseasnews.application.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 해외뉴스종합 응답 DTO.
 * 뉴스 목록과 페이지네이션 정보를 함께 전달.
 */
@Getter
@Builder
public class ComprehensiveNewsResponse {

    private final List<NewsItem> items;
    private final boolean hasMore;
    private final String lastDataDt;     // 다음 페이지 요청용 마지막 일자
    private final String lastDataTm;     // 다음 페이지 요청용 마지막 시간

    @Getter
    @Builder
    public static class NewsItem {
        private final String dateTime;       // 조회일시
        private final String title;          // 제목
        private final String className;      // 중분류명
        private final String source;         // 자료원
        private final String stockName;      // 종목명
    }
}