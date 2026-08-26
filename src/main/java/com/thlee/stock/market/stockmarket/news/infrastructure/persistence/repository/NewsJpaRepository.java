package com.thlee.stock.market.stockmarket.news.infrastructure.persistence.repository;

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

    @Modifying
    @Query(value = "INSERT INTO news (original_url, title, content, published_at, created_at, keyword_id, region) " +
            "VALUES (:originalUrl, :title, :content, :publishedAt, :createdAt, :keywordId, :region) " +
            "ON CONFLICT (original_url) DO NOTHING",
            nativeQuery = true)
    int insertIgnoreDuplicate(@Param("originalUrl") String originalUrl,
                              @Param("title") String title,
                              @Param("content") String content,
                              @Param("publishedAt") LocalDateTime publishedAt,
                              @Param("createdAt") LocalDateTime createdAt,
                              @Param("keywordId") Long keywordId,
                              @Param("region") String region);

    void deleteByKeywordId(Long keywordId);
}
