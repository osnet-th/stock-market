package com.thlee.stock.market.stockmarket.news.domain.repository;

import java.time.LocalDate;

/**
 * 키워드별 일자별 수집 건수 집계 행 (#115 레일 스파크라인).
 *
 * <p>발행일이 아니라 <b>수집 시각</b>(created_at) 기준이다 — 목업의 "오늘 +N" 은
 * 오늘 새로 들어온 건수를 뜻하고, 과거 발행 기사가 오늘 수집될 수 있다.
 */
public record KeywordDailyNewsCount(
        Long keywordId,
        LocalDate day,
        long dailyCount
) {
}
