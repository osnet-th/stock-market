package com.thlee.stock.market.stockmarket.news.domain.repository;

import com.thlee.stock.market.stockmarket.news.domain.model.KeywordCollectionHistory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface KeywordCollectionHistoryRepository {

    KeywordCollectionHistory save(KeywordCollectionHistory history);

    /**
     * 키워드별 이력 요약 — 마지막 시도 · 마지막 성공 · 연속 실패 (#115). 쿼리 1회.
     */
    List<KeywordCollectionSummary> summarizeByKeywordIds(List<Long> keywordIds);

    /**
     * 배치 전체의 마지막 시도 시각 (#115 스케줄 상태 "마지막 N분 전").
     */
    Optional<LocalDateTime> findLastAttemptAt();

    /**
     * 보존 기간이 지난 이력 삭제 (#115) — 키워드 수만큼 매시 쌓이므로 상한이 필요하다.
     *
     * @return 삭제된 건수
     */
    int deleteOlderThan(LocalDateTime threshold);
}
