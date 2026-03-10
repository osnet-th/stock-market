package com.thlee.stock.market.stockmarket.news.infrastructure.persistence;

import com.thlee.stock.market.stockmarket.news.domain.model.NewsPurpose;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface NewsSourceJpaRepository extends JpaRepository<NewsSourceEntity, Long> {

    @Modifying
    @Query(value = "INSERT INTO news_source (news_id, user_id, purpose, source_id, created_at) " +
            "VALUES (:newsId, :userId, :purpose, :sourceId, :createdAt) " +
            "ON CONFLICT (news_id, user_id, purpose, source_id) DO NOTHING",
            nativeQuery = true)
    int insertIgnoreDuplicate(@Param("newsId") Long newsId,
                              @Param("userId") Long userId,
                              @Param("purpose") String purpose,
                              @Param("sourceId") Long sourceId,
                              @Param("createdAt") LocalDateTime createdAt);

    /**
     * 특정 사용자가 이미 수집한 뉴스의 URL 목록 조회
     */
    @Query("SELECT n.originalUrl FROM NewsEntity n " +
           "JOIN NewsSourceEntity ns ON n.id = ns.newsId " +
           "WHERE ns.userId = :userId AND n.originalUrl IN :urls")
    List<String> findExistingUrlsByUser(@Param("urls") List<String> urls,
                                       @Param("userId") Long userId);

    Page<NewsSourceEntity> findByPurposeAndSourceIdOrderByCreatedAtDesc(
            NewsPurpose purpose, Long sourceId, Pageable pageable);

    void deleteByPurposeAndSourceId(NewsPurpose purpose, Long sourceId);
}