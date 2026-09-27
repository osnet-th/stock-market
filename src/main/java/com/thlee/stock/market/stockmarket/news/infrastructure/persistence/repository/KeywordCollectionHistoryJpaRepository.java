package com.thlee.stock.market.stockmarket.news.infrastructure.persistence.repository;

import com.thlee.stock.market.stockmarket.news.infrastructure.persistence.KeywordCollectionHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface KeywordCollectionHistoryJpaRepository extends JpaRepository<KeywordCollectionHistoryEntity, Long> {

    /**
     * 키워드별 마지막 시도 · 마지막 성공 · 연속 실패를 한 방에 집계한다 (#115).
     *
     * <p>연속 실패는 "마지막 성공 이후의 실패 횟수"다. 집계 함수를 중첩할 수 없어
     * CTE 로 마지막 성공 시각을 먼저 구하고 상관 서브쿼리로 그 이후 실패만 센다.
     * {@code FILTER} · CTE 는 Postgres 문법이며, 이 프로젝트는 이미
     * {@code ON CONFLICT} 등 Postgres 전용 SQL 을 쓰고 있다.
     */
    @Query(value = """
            WITH agg AS (
                SELECT keyword_id,
                       MAX(attempted_at) AS last_attempt_at,
                       MAX(attempted_at) FILTER (WHERE status = 'SUCCESS') AS last_success_at
                  FROM keyword_collection_history
                 WHERE keyword_id IN (:keywordIds)
                 GROUP BY keyword_id
            )
            SELECT a.keyword_id,
                   a.last_attempt_at,
                   a.last_success_at,
                   (SELECT COUNT(*)
                      FROM keyword_collection_history f
                     WHERE f.keyword_id = a.keyword_id
                       AND f.status = 'FAILURE'
                       AND (a.last_success_at IS NULL OR f.attempted_at > a.last_success_at)) AS failure_streak
              FROM agg a
            """, nativeQuery = true)
    List<Object[]> summarizeByKeywordIds(@Param("keywordIds") Collection<Long> keywordIds);

    /** 배치 전체의 마지막 시도 시각 (#115 스케줄 상태). */
    @Query("SELECT MAX(h.attemptedAt) FROM KeywordCollectionHistoryEntity h")
    LocalDateTime findLastAttemptAt();

    @Modifying
    @Query("DELETE FROM KeywordCollectionHistoryEntity h WHERE h.attemptedAt < :threshold")
    int deleteByAttemptedAtBefore(@Param("threshold") LocalDateTime threshold);
}
