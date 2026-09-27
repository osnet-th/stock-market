package com.thlee.stock.market.stockmarket.news.domain.repository;

import java.time.LocalDateTime;

/**
 * 키워드별 수집 이력 요약 (#115).
 *
 * @param keywordId     키워드
 * @param lastAttemptAt 마지막 수집 시도 시각 (성공/실패 무관)
 * @param lastSuccessAt 마지막 수집 성공 시각. 한 번도 성공하지 않았으면 null
 * @param failureStreak 마지막 성공 이후 실패 횟수. 0 이면 정상
 */
public record KeywordCollectionSummary(
        Long keywordId,
        LocalDateTime lastAttemptAt,
        LocalDateTime lastSuccessAt,
        long failureStreak
) {

    public boolean isFailing() {
        return failureStreak > 0;
    }
}
