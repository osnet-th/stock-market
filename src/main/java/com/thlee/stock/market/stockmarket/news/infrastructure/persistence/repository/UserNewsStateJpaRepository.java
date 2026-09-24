package com.thlee.stock.market.stockmarket.news.infrastructure.persistence.repository;

import com.thlee.stock.market.stockmarket.news.infrastructure.persistence.UserNewsStateEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserNewsStateJpaRepository extends JpaRepository<UserNewsStateEntity, Long> {

    Optional<UserNewsStateEntity> findByUserIdAndNewsId(Long userId, Long newsId);

    List<UserNewsStateEntity> findByUserIdAndNewsIdIn(Long userId, Collection<Long> newsIds);

    /**
     * 내가 읽은 뉴스의 originalUrl (#115) — ES 검색에서 제외 대상으로 쓴다.
     * 키워드 스코프로 좁히고 최신 읽음부터 limit 건까지만 가져온다.
     */
    @Query("""
            SELECT n.originalUrl
              FROM UserNewsStateEntity s, NewsEntity n
             WHERE s.newsId = n.id
               AND s.userId = :userId
               AND s.read = true
               AND n.keywordId IN :keywordIds
             ORDER BY s.updatedAt DESC
            """)
    List<String> findReadOriginalUrls(@Param("userId") Long userId,
                                      @Param("keywordIds") Collection<Long> keywordIds,
                                      Pageable pageable);
}
