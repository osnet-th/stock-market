# 인프라 Repository 예시

## NewsSourceRepository 도메인 인터페이스 (신규)

```java
package com.thlee.stock.market.stockmarket.news.domain.repository;

import com.thlee.stock.market.stockmarket.common.response.PageResult;
import com.thlee.stock.market.stockmarket.news.domain.model.NewsSource;
import com.thlee.stock.market.stockmarket.news.domain.model.NewsPurpose;

import java.util.List;

public interface NewsSourceRepository {

    boolean insertIgnoreDuplicate(NewsSource newsSource);

    /**
     * 특정 사용자가 특정 URL 목록 중 이미 수집한 URL 조회
     */
    List<String> findExistingUrlsByUser(List<String> urls, Long userId);

    /**
     * purpose + sourceId 기반 뉴스 ID 목록 조회 (페이징)
     */
    PageResult<Long> findNewsIdsByPurposeAndSourceId(NewsPurpose purpose, Long sourceId, int page, int size);

    void deleteByPurposeAndSourceId(NewsPurpose purpose, Long sourceId);
}
```

## NewsSourceJpaRepository (신규)

```java
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
```

## NewsSourceRepositoryImpl (신규)

```java
package com.thlee.stock.market.stockmarket.news.infrastructure.persistence;

import com.thlee.stock.market.stockmarket.common.response.PageResult;
import com.thlee.stock.market.stockmarket.news.domain.model.NewsSource;
import com.thlee.stock.market.stockmarket.news.domain.model.NewsPurpose;
import com.thlee.stock.market.stockmarket.news.domain.repository.NewsSourceRepository;
import com.thlee.stock.market.stockmarket.news.infrastructure.persistence.mapper.NewsSourceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class NewsSourceRepositoryImpl implements NewsSourceRepository {

    private final NewsSourceJpaRepository newsSourceJpaRepository;

    @Override
    public boolean insertIgnoreDuplicate(NewsSource newsSource) {
        int inserted = newsSourceJpaRepository.insertIgnoreDuplicate(
                newsSource.getNewsId(),
                newsSource.getUserId(),
                newsSource.getPurpose().name(),
                newsSource.getSourceId(),
                newsSource.getCreatedAt()
        );
        return inserted > 0;
    }

    @Override
    public List<String> findExistingUrlsByUser(List<String> urls, Long userId) {
        return newsSourceJpaRepository.findExistingUrlsByUser(urls, userId);
    }

    @Override
    public PageResult<Long> findNewsIdsByPurposeAndSourceId(NewsPurpose purpose, Long sourceId, int page, int size) {
        Page<NewsSourceEntity> entityPage = newsSourceJpaRepository
                .findByPurposeAndSourceIdOrderByCreatedAtDesc(purpose, sourceId, PageRequest.of(page, size));

        List<Long> newsIds = entityPage.getContent().stream()
                .map(NewsSourceEntity::getNewsId)
                .toList();

        return new PageResult<>(newsIds, page, size, entityPage.getTotalElements());
    }

    @Override
    public void deleteByPurposeAndSourceId(NewsPurpose purpose, Long sourceId) {
        newsSourceJpaRepository.deleteByPurposeAndSourceId(purpose, sourceId);
    }
}
```

## NewsSourceMapper (신규)

```java
package com.thlee.stock.market.stockmarket.news.infrastructure.persistence.mapper;

import com.thlee.stock.market.stockmarket.news.domain.model.NewsSource;
import com.thlee.stock.market.stockmarket.news.infrastructure.persistence.NewsSourceEntity;

public class NewsSourceMapper {

    public static NewsSourceEntity toEntity(NewsSource newsSource) {
        return new NewsSourceEntity(
                newsSource.getId(),
                newsSource.getNewsId(),
                newsSource.getUserId(),
                newsSource.getPurpose(),
                newsSource.getSourceId(),
                newsSource.getCreatedAt()
        );
    }

    public static NewsSource toDomain(NewsSourceEntity entity) {
        return new NewsSource(
                entity.getId(),
                entity.getNewsId(),
                entity.getUserId(),
                entity.getPurpose(),
                entity.getSourceId(),
                entity.getCreatedAt()
        );
    }
}
```

## NewsRepository 도메인 인터페이스 (변경 후)

```java
package com.thlee.stock.market.stockmarket.news.domain.repository;

import com.thlee.stock.market.stockmarket.news.domain.model.News;

import java.util.List;

public interface NewsRepository {

    /**
     * 뉴스 저장 (중복 URL 무시, 저장된 뉴스 반환)
     */
    News saveIgnoreDuplicate(News news);

    List<News> findByIds(List<Long> ids);
}
```

## NewsJpaRepository (변경 후)

```java
package com.thlee.stock.market.stockmarket.news.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface NewsJpaRepository extends JpaRepository<NewsEntity, Long> {

    Optional<NewsEntity> findByOriginalUrl(String originalUrl);

    List<NewsEntity> findByIdIn(List<Long> ids);

    @Modifying
    @Query(value = "INSERT INTO news (original_url, title, content, published_at, created_at, region) " +
            "VALUES (:originalUrl, :title, :content, :publishedAt, :createdAt, :region) " +
            "ON CONFLICT (original_url) DO NOTHING",
            nativeQuery = true)
    int insertIgnoreDuplicate(@Param("originalUrl") String originalUrl,
                              @Param("title") String title,
                              @Param("content") String content,
                              @Param("publishedAt") LocalDateTime publishedAt,
                              @Param("createdAt") LocalDateTime createdAt,
                              @Param("region") String region);
}
```

## NewsRepositoryImpl (변경 후)

```java
package com.thlee.stock.market.stockmarket.news.infrastructure.persistence;

import com.thlee.stock.market.stockmarket.news.domain.model.News;
import com.thlee.stock.market.stockmarket.news.domain.repository.NewsRepository;
import com.thlee.stock.market.stockmarket.news.infrastructure.persistence.mapper.NewsMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class NewsRepositoryImpl implements NewsRepository {

    private final NewsJpaRepository newsJpaRepository;

    @Override
    public News saveIgnoreDuplicate(News news) {
        newsJpaRepository.insertIgnoreDuplicate(
                news.getOriginalUrl(),
                news.getTitle(),
                news.getContent(),
                news.getPublishedAt(),
                news.getCreatedAt(),
                news.getRegion() != null ? news.getRegion().name() : null
        );

        // INSERT 성공/실패 상관없이 URL로 조회하여 반환 (이미 존재하면 기존 레코드 반환)
        return newsJpaRepository.findByOriginalUrl(news.getOriginalUrl())
                .map(NewsMapper::toDomain)
                .orElseThrow(() -> new IllegalStateException("저장된 뉴스를 찾을 수 없습니다."));
    }

    @Override
    public List<News> findByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return newsJpaRepository.findByIdIn(ids).stream()
                .map(NewsMapper::toDomain)
                .collect(Collectors.toList());
    }
}
```

## NewsMapper (변경 후)

```java
package com.thlee.stock.market.stockmarket.news.infrastructure.persistence.mapper;

import com.thlee.stock.market.stockmarket.news.domain.model.News;
import com.thlee.stock.market.stockmarket.news.infrastructure.persistence.NewsEntity;

public class NewsMapper {

    public static NewsEntity toEntity(News news) {
        return new NewsEntity(
                news.getId(),
                news.getOriginalUrl(),
                news.getTitle(),
                news.getContent(),
                news.getPublishedAt(),
                news.getCreatedAt(),
                news.getRegion()
        );
    }

    public static News toDomain(NewsEntity entity) {
        return new News(
                entity.getId(),
                entity.getOriginalUrl(),
                entity.getTitle(),
                entity.getContent(),
                entity.getPublishedAt(),
                entity.getCreatedAt(),
                entity.getRegion()
        );
    }
}
```