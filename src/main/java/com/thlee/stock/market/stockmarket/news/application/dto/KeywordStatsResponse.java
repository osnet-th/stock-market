package com.thlee.stock.market.stockmarket.news.application.dto;

import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 키워드 레일 통계 응답 (#115).
 *
 * <p>목업 레일 한 행에 필요한 4가지를 한 번에 내려준다 —
 * 총 건수 · 오늘 수집 · 마지막 성공 · 7일 스파크라인.
 * 키워드마다 따로 조회하면 N+1 이 되므로 집계 쿼리 2회로 묶어 만든다.
 */
@Getter
public class KeywordStatsResponse {

    /** 스파크라인 구간 길이. 목업 막대 개수와 같다. */
    public static final int SPARKLINE_DAYS = 7;

    private final long todayTotal;
    private final List<Item> items;

    public KeywordStatsResponse(long todayTotal, List<Item> items) {
        this.todayTotal = todayTotal;
        this.items = items;
    }

    public static KeywordStatsResponse empty() {
        return new KeywordStatsResponse(0L, List.of());
    }

    @Getter
    public static class Item {
        private final Long keywordId;
        private final long totalCount;
        private final long todayCount;
        /**
         * 마지막으로 기사가 저장된 시각. 수집을 시도했지만 새 기사가 없었던 경우는 반영되지 않는다.
         * 수집이 한 번도 성공하지 않았으면 null.
         */
        private final LocalDateTime lastCollectedAt;
        /** 오래된 날 → 오늘 순서의 일별 수집 건수. 길이는 항상 {@link #SPARKLINE_DAYS}. */
        private final List<Long> daily;

        public Item(Long keywordId, long totalCount, long todayCount,
                    LocalDateTime lastCollectedAt, List<Long> daily) {
            this.keywordId = keywordId;
            this.totalCount = totalCount;
            this.todayCount = todayCount;
            this.lastCollectedAt = lastCollectedAt;
            this.daily = daily;
        }
    }
}
