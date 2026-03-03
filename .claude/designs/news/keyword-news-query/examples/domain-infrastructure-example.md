# Domain & Infrastructure 예시 코드

## PageResult (domain)

```java
package com.thlee.stock.market.stockmarket.common.response;

import java.util.List;

/**
 * 순수 Java 페이징 결과 래퍼 (Spring 의존성 없음)
 */
@Getter
public class PageResult<T> {
    private final List<T> content;
    private final int page;
    private final int size;
    private final long totalElements;
    private final int totalPages;

    public PageResult(List<T> content, int page, int size, long totalElements) {
        this.content = content;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = size > 0 ? (int) Math.ceil((double) totalElements / size) : 0;
    }
}
```

## NewsRepository (domain 포트)

```java
// 기존 메서드 유지 + 추가
PageResult<News> findBySearchKeyword(String searchKeyword, int page, int size);
```

## NewsJpaRepository (infrastructure)

```java
// 기존 메서드 유지 + 추가
Page<NewsEntity> findBySearchKeywordOrderByPublishedAtDesc(String searchKeyword, Pageable pageable);
```

## NewsRepositoryImpl (infrastructure 어댑터)

```java
@Override
public PageResult<News> findBySearchKeyword(String searchKeyword, int page, int size) {
    Pageable pageable = PageRequest.of(page, size);
    Page<NewsEntity> entityPage = newsJpaRepository
            .findBySearchKeywordOrderByPublishedAtDesc(searchKeyword, pageable);

    List<News> newsList = entityPage.getContent().stream()
            .map(NewsMapper::toDomain)
            .collect(Collectors.toList());

    return new PageResult<>(newsList, page, size, entityPage.getTotalElements());
}
```