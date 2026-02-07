package com.thlee.stock.market.stockmarket.news.infrastructure.persistence;

import com.thlee.stock.market.stockmarket.news.domain.model.NewsPurpose;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface NewsJpaRepository extends JpaRepository<NewsEntity, Long> {

    Optional<NewsEntity> findByOriginalUrl(String originalUrl);

    List<NewsEntity> findByPurpose(NewsPurpose purpose);

    @Modifying
    @Query(value = "INSERT INTO news (original_url, user_id, title, content, published_at, created_at, purpose, search_keyword) " +
            "VALUES (:originalUrl, :userId, :title, :content, :publishedAt, :createdAt, :purpose, :searchKeyword) " +
            "ON CONFLICT (original_url) DO NOTHING",
            nativeQuery = true)
    int insertIgnoreDuplicate(@Param("originalUrl") String originalUrl,
                              @Param("userId") Long userId,
                              @Param("title") String title,
                              @Param("content") String content,
                              @Param("publishedAt") LocalDateTime publishedAt,
                              @Param("createdAt") LocalDateTime createdAt,
                              @Param("purpose") String purpose,
                              @Param("searchKeyword") String searchKeyword);
}
