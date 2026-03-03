# NewsEntity 예시

```java
package com.thlee.stock.market.stockmarket.news.infrastructure.persistence;

/**
 * News JPA Entity
 */
@Entity
@Table(
        name = "news",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "original_url")
        },
        indexes = {
                @Index(name = "idx_news_purpose_source_published",
                       columnList = "purpose, source_id, published_at DESC")
        }
)
@Getter
public class NewsEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "original_url", nullable = false, length = 500)
    private String originalUrl;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "published_at", nullable = false)
    private LocalDateTime publishedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", nullable = false, length = 100)
    private NewsPurpose purpose;

    @Column(name = "source_id", nullable = false)
    private Long sourceId;

    @Enumerated(EnumType.STRING)
    private Region region;

    protected NewsEntity() {
    }

    public NewsEntity(Long id,
                      String originalUrl,
                      Long userId,
                      String title,
                      String content,
                      LocalDateTime publishedAt,
                      LocalDateTime createdAt,
                      NewsPurpose purpose,
                      Long sourceId,
                      Region region) {
        this.id = id;
        this.originalUrl = originalUrl;
        this.userId = userId;
        this.title = title;
        this.content = content;
        this.publishedAt = publishedAt;
        this.createdAt = createdAt;
        this.purpose = purpose;
        this.sourceId = sourceId;
        this.region = region;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
```

## NewsJpaRepository

```java
public interface NewsJpaRepository extends JpaRepository<NewsEntity, Long> {

    Optional<NewsEntity> findByOriginalUrl(String originalUrl);

    List<NewsEntity> findByPurpose(NewsPurpose purpose);

    Page<NewsEntity> findByPurposeAndSourceIdOrderByPublishedAtDesc(
            NewsPurpose purpose, Long sourceId, Pageable pageable);

    @Modifying
    @Query(value = "INSERT INTO news (original_url, user_id, title, content, published_at, created_at, purpose, source_id, region) " +
            "VALUES (:originalUrl, :userId, :title, :content, :publishedAt, :createdAt, :purpose, :sourceId, :region) " +
            "ON CONFLICT (original_url) DO NOTHING",
            nativeQuery = true)
    int insertIgnoreDuplicate(@Param("originalUrl") String originalUrl,
                              @Param("userId") Long userId,
                              @Param("title") String title,
                              @Param("content") String content,
                              @Param("publishedAt") LocalDateTime publishedAt,
                              @Param("createdAt") LocalDateTime createdAt,
                              @Param("purpose") String purpose,
                              @Param("sourceId") Long sourceId,
                              @Param("region") String region);
}
```

## NewsMapper

```java
public class NewsMapper {

    public static NewsEntity toEntity(News news) {
        return new NewsEntity(
                news.getId(),
                news.getOriginalUrl(),
                news.getUserId(),
                news.getTitle(),
                news.getContent(),
                news.getPublishedAt(),
                news.getCreatedAt(),
                news.getPurpose(),
                news.getSourceId(),
                news.getRegion()
        );
    }

    public static News toDomain(NewsEntity entity) {
        return new News(
                entity.getId(),
                entity.getOriginalUrl(),
                entity.getUserId(),
                entity.getTitle(),
                entity.getContent(),
                entity.getPublishedAt(),
                entity.getCreatedAt(),
                entity.getPurpose(),
                entity.getSourceId(),
                entity.getRegion()
        );
    }
}
```

## NewsRepository (포트)

```java
public interface NewsRepository {
    News save(News news);

    boolean insertIgnoreDuplicate(News news);

    Optional<News> findByOriginalUrl(String originalUrl);

    List<News> findByPurpose(NewsPurpose purpose);

    PageResult<News> findByPurposeAndSourceId(NewsPurpose purpose, Long sourceId, int page, int size);
}
```

## NewsRepositoryImpl (어댑터)

```java
@Override
public PageResult<News> findByPurposeAndSourceId(NewsPurpose purpose, Long sourceId, int page, int size) {
    Page<NewsEntity> entityPage = newsJpaRepository
            .findByPurposeAndSourceIdOrderByPublishedAtDesc(purpose, sourceId, PageRequest.of(page, size));

    List<News> newsList = entityPage.getContent().stream()
            .map(NewsMapper::toDomain)
            .collect(Collectors.toList());

    return new PageResult<>(newsList, page, size, entityPage.getTotalElements());
}
```
