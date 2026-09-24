package com.thlee.stock.market.stockmarket.newsjournal.infrastructure.persistence;

import com.thlee.stock.market.stockmarket.newsjournal.domain.model.EventImpact;
import com.thlee.stock.market.stockmarket.newsjournal.domain.repository.NewsEventCategoryCount;
import com.thlee.stock.market.stockmarket.newsjournal.domain.repository.NewsEventImpactCount;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface NewsEventJpaRepository extends JpaRepository<NewsEventEntity, Long> {

    Optional<NewsEventEntity> findByIdAndUserId(Long id, Long userId);

    /**
     * 필터 리스트 조회.
     *
     * <p>{@code qLike} 는 소문자 + LIKE 와일드카드(`%…%`) + `!` escape 가 적용된 패턴으로,
     * 제목/WHAT/WHY/HOW 본문과 키워드·분류명(EXISTS)을 통합 검색한다.
     * {@code keywords} 는 AND 매칭 — 사건이 가진 키워드 중 필터 키워드와 일치하는 수가
     * {@code keywordCount} 와 같아야 한다. 비활성 시 호출 측이 {@code keywordCount=0} 과
     * 절대 매칭되지 않는 sentinel 리스트를 넘겨 빈 IN 리스트 렌더링을 피한다.
     */
    @Query("""
            SELECT e FROM NewsEventEntity e
             WHERE e.userId = :userId
               AND (:impact IS NULL OR e.impact = :impact)
               AND (:categoryId IS NULL OR e.categoryId = :categoryId)
               AND (:fromDate IS NULL OR e.occurredDate >= :fromDate)
               AND (:toDate IS NULL OR e.occurredDate <= :toDate)
               AND (:qLike IS NULL
                    OR LOWER(e.title) LIKE :qLike ESCAPE '!'
                    OR LOWER(e.what) LIKE :qLike ESCAPE '!'
                    OR LOWER(e.why) LIKE :qLike ESCAPE '!'
                    OR LOWER(e.how) LIKE :qLike ESCAPE '!'
                    OR EXISTS (SELECT 1 FROM NewsEventKeywordEntity qk
                                WHERE qk.eventId = e.id AND LOWER(qk.keyword) LIKE :qLike ESCAPE '!')
                    OR EXISTS (SELECT 1 FROM NewsEventCategoryEntity qc
                                WHERE qc.id = e.categoryId AND LOWER(qc.name) LIKE :qLike ESCAPE '!'))
               AND (:keywordCount = 0 OR (SELECT COUNT(DISTINCT fk.keyword) FROM NewsEventKeywordEntity fk
                                           WHERE fk.eventId = e.id AND fk.keyword IN :keywords) = :keywordCount)
             ORDER BY e.occurredDate DESC, e.id DESC
            """)
    List<NewsEventEntity> findList(
            @Param("userId") Long userId,
            @Param("impact") EventImpact impact,
            @Param("categoryId") Long categoryId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("qLike") String qLike,
            @Param("keywords") List<String> keywords,
            @Param("keywordCount") long keywordCount,
            Pageable pageable
    );

    @Query("""
            SELECT COUNT(e) FROM NewsEventEntity e
             WHERE e.userId = :userId
               AND (:impact IS NULL OR e.impact = :impact)
               AND (:categoryId IS NULL OR e.categoryId = :categoryId)
               AND (:fromDate IS NULL OR e.occurredDate >= :fromDate)
               AND (:toDate IS NULL OR e.occurredDate <= :toDate)
               AND (:qLike IS NULL
                    OR LOWER(e.title) LIKE :qLike ESCAPE '!'
                    OR LOWER(e.what) LIKE :qLike ESCAPE '!'
                    OR LOWER(e.why) LIKE :qLike ESCAPE '!'
                    OR LOWER(e.how) LIKE :qLike ESCAPE '!'
                    OR EXISTS (SELECT 1 FROM NewsEventKeywordEntity qk
                                WHERE qk.eventId = e.id AND LOWER(qk.keyword) LIKE :qLike ESCAPE '!')
                    OR EXISTS (SELECT 1 FROM NewsEventCategoryEntity qc
                                WHERE qc.id = e.categoryId AND LOWER(qc.name) LIKE :qLike ESCAPE '!'))
               AND (:keywordCount = 0 OR (SELECT COUNT(DISTINCT fk.keyword) FROM NewsEventKeywordEntity fk
                                           WHERE fk.eventId = e.id AND fk.keyword IN :keywords) = :keywordCount)
            """)
    long countList(
            @Param("userId") Long userId,
            @Param("impact") EventImpact impact,
            @Param("categoryId") Long categoryId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("qLike") String qLike,
            @Param("keywords") List<String> keywords,
            @Param("keywordCount") long keywordCount
    );

    /** 사용자별 시장영향 그룹 합계 (화면 통계용). 0건 임팩트는 결과에 없음 — 호출 측이 보정. */
    @Query("""
            SELECT new com.thlee.stock.market.stockmarket.newsjournal.domain.repository.NewsEventImpactCount(
                e.impact, COUNT(e)
            )
            FROM NewsEventEntity e
            WHERE e.userId = :userId
            GROUP BY e.impact
            """)
    List<NewsEventImpactCount> countByImpactGroupedByImpact(@Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM NewsEventEntity e WHERE e.id = :id AND e.userId = :userId")
    int deleteByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    /**
     * 최근 등록 N건 (대시보드 요약). created_at DESC + id DESC 안정 정렬.
     * categoryCounts 쿼리와 동일하게 NULL category_id 행은 제외해 응답 일관성 유지(legacy backfill 잔재 호환).
     * limit 은 Pageable.getPageSize() 로 강제.
     */
    @Query("""
            SELECT e FROM NewsEventEntity e
             WHERE e.userId = :userId
               AND e.categoryId IS NOT NULL
             ORDER BY e.createdAt DESC, e.id DESC
            """)
    List<NewsEventEntity> findRecentByUserId(@Param("userId") Long userId, Pageable pageable);

    /** 사용자별 카테고리 ID 그룹 합계. NULL 카테고리 행은 제외(레거시 backfill 잔재 호환). */
    @Query("""
            SELECT new com.thlee.stock.market.stockmarket.newsjournal.domain.repository.NewsEventCategoryCount(
                e.categoryId, COUNT(e)
            )
            FROM NewsEventEntity e
            WHERE e.userId = :userId
              AND e.categoryId IS NOT NULL
            GROUP BY e.categoryId
            """)
    List<NewsEventCategoryCount> countByCategoryGroupedByCategoryId(@Param("userId") Long userId);
}