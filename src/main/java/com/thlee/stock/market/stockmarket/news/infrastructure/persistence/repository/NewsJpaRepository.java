package com.thlee.stock.market.stockmarket.news.infrastructure.persistence.repository;

import com.thlee.stock.market.stockmarket.news.infrastructure.persistence.NewsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Optional;

public interface NewsJpaRepository extends JpaRepository<NewsEntity, Long> {

    Optional<NewsEntity> findByOriginalUrl(String originalUrl);

    Page<NewsEntity> findByKeywordIdOrderByPublishedAtDesc(Long keywordId, Pageable pageable);

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
