package com.thlee.stock.market.stockmarket.news.infrastructure.persistence.repository;

import com.thlee.stock.market.stockmarket.news.domain.model.Region;
import com.thlee.stock.market.stockmarket.news.domain.repository.KeywordDailyNewsCount;
import com.thlee.stock.market.stockmarket.news.domain.repository.KeywordNewsCount;
import com.thlee.stock.market.stockmarket.news.infrastructure.persistence.NewsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface NewsJpaRepository extends JpaRepository<NewsEntity, Long> {

    Optional<NewsEntity> findByOriginalUrl(String originalUrl);

    /**
     * ES 검색 결과(originalUrl 만 신뢰 가능)를 DB 행으로 되짚기 위한 조회 (#115).
     * original_url 이 UNIQUE 라 URL 하나당 최대 1행이다.
     */
    List<NewsEntity> findByOriginalUrlIn(Collection<String> originalUrls);

    Page<NewsEntity> findByKeywordIdOrderByPublishedAtDesc(Long keywordId, Pageable pageable);

    /**
     * 여러 키워드의 뉴스를 최신순으로 합쳐 조회 (#114 홈 키워드 피드).
     * Pageable 로 상한을 걸어 전체 로드를 막는다.
     */
    List<NewsEntity> findByKeywordIdInOrderByPublishedAtDesc(Collection<Long> keywordIds, Pageable pageable);

    /**
     * 여러 키워드의 특정 시각 이후 수집 건수 (#114 — "오늘 N건" 표시용).
     * 수집 시각 기준이라 publishedAt 이 아니라 createdAt 을 본다.
     */
    long countByKeywordIdInAndCreatedAtGreaterThanEqual(Collection<Long> keywordIds, LocalDateTime since);

    /**
     * 검색어 없는 조회 — 키워드 스코프 안에서 기간·지역 필터를 적용해 최신순 페이징 (#115).
     *
     * <p>목업의 기본 화면("저장된 전체 뉴스")이 이 경로를 탄다. 전문 검색이 아니라서
     * ES 를 거치지 않으며, 그 덕에 정렬이 안정적이고 언론사·읽음 상태를 같은 행에서 바로 읽는다.
     * 파라미터가 null 이면 그 조건을 건너뛴다.
     */
    @Query("""
            SELECT n FROM NewsEntity n
            WHERE n.keywordId IN :keywordIds
              AND (:startAt IS NULL OR n.publishedAt >= :startAt)
              AND (:endAt IS NULL OR n.publishedAt <= :endAt)
              AND (:region IS NULL OR n.region = :region)
            ORDER BY n.publishedAt DESC, n.id DESC
            """)
    Page<NewsEntity> findLatestByScope(@Param("keywordIds") Collection<Long> keywordIds,
                                       @Param("startAt") LocalDateTime startAt,
                                       @Param("endAt") LocalDateTime endAt,
                                       @Param("region") Region region,
                                       Pageable pageable);

    /**
     * 키워드별 총 건수 + 마지막 저장 시각 (#115 레일 통계).
     * 키워드 수만큼 조회하면 N+1 이 되므로 한 방에 묶는다.
     */
    @Query("""
            SELECT new com.thlee.stock.market.stockmarket.news.domain.repository.KeywordNewsCount(
                n.keywordId, COUNT(n), MAX(n.createdAt)
            )
            FROM NewsEntity n
            WHERE n.keywordId IN :keywordIds
            GROUP BY n.keywordId
            """)
    List<KeywordNewsCount> aggregateCountsByKeywordIds(@Param("keywordIds") Collection<Long> keywordIds);

    /**
     * 키워드별·일자별 수집 건수 (#115 스파크라인 + "오늘 +N").
     * 수집 시각(createdAt) 기준이며 since 이후만 집계한다.
     */
    @Query("""
            SELECT new com.thlee.stock.market.stockmarket.news.domain.repository.KeywordDailyNewsCount(
                n.keywordId, CAST(n.createdAt AS LocalDate), COUNT(n)
            )
            FROM NewsEntity n
            WHERE n.keywordId IN :keywordIds
              AND n.createdAt >= :since
            GROUP BY n.keywordId, CAST(n.createdAt AS LocalDate)
            """)
    List<KeywordDailyNewsCount> aggregateDailyCountsByKeywordIds(@Param("keywordIds") Collection<Long> keywordIds,
                                                                 @Param("since") LocalDateTime since);

    @Modifying
    @Query(value = "INSERT INTO news (original_url, title, content, published_at, created_at, keyword_id, region, source) " +
            "VALUES (:originalUrl, :title, :content, :publishedAt, :createdAt, :keywordId, :region, :source) " +
            "ON CONFLICT (original_url) DO NOTHING",
            nativeQuery = true)
    int insertIgnoreDuplicate(@Param("originalUrl") String originalUrl,
                              @Param("title") String title,
                              @Param("content") String content,
                              @Param("publishedAt") LocalDateTime publishedAt,
                              @Param("createdAt") LocalDateTime createdAt,
                              @Param("keywordId") Long keywordId,
                              @Param("region") String region,
                              @Param("source") String source);

    /**
     * 키워드 수정(재구독) 시 기존 기사를 새 키워드로 이관한다 (#115).
     *
     * <p>단독 구독자일 때만 호출된다 — 다른 구독자가 있으면 그들의 기사를 옮기는 셈이 된다.
     * 목업 수정 모달의 "이미 수집된 기사 N건은 이름을 바꿔도 그대로 유지됩니다" 약속을 지키는 부분이다.
     */
    @Modifying
    @Query("UPDATE NewsEntity n SET n.keywordId = :newKeywordId WHERE n.keywordId = :oldKeywordId")
    int reassignKeywordId(@Param("oldKeywordId") Long oldKeywordId,
                          @Param("newKeywordId") Long newKeywordId);

    /** 이관 후 ES 재색인 대상을 모으기 위한 전건 조회 (#115). */
    List<NewsEntity> findAllByKeywordId(Long keywordId);

    void deleteByKeywordId(Long keywordId);
}
