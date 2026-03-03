# Application & Presentation 예시 코드

## NewsQueryService (application)

```java
// 기존 메서드 유지 + 추가
public 
<NewsDto> getNewsByKeyword(String keyword, int page, int size) {
    PageResult<News> result = newsRepository.findBySearchKeyword(keyword, page, size);

    List<NewsDto> dtoList = result.getContent().stream()
            .map(NewsDto::from)
            .collect(Collectors.toList());

    return new PageResult<>(dtoList, result.getPage(), result.getSize(), result.getTotalElements());
}
```

## NewsQueryResponse (presentation DTO)

```java
package com.thlee.stock.market.stockmarket.news.presentation.dto;

import java.util.List;

/**
 * 뉴스 조회 페이징 응답 DTO
 */
@Getter
public class NewsQueryResponse<T> {
    private final List<T> content;
    private final int page;
    private final int size;
    private final long totalElements;
    private final int totalPages;

    public NewsQueryResponse(List<T> content, int page, int size, long totalElements, int totalPages) {
        this.content = content;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
    }

    public static <T> NewsQueryResponse<T> from(PageResult<T> pageResult) {
        return new NewsQueryResponse<>(
                pageResult.getContent(),
                pageResult.getPage(),
                pageResult.getSize(),
                pageResult.getTotalElements(),
                pageResult.getTotalPages()
        );
    }
}
```

## NewsController (presentation)

```java
package com.thlee.stock.market.stockmarket.news.presentation;

@RestController
@RequestMapping("/api/news")
@RequiredArgsConstructor
public class NewsController {

    private final NewsQueryService newsQueryService;

    /**
     * 키워드 기반 뉴스 조회 (페이징)
     */
    @GetMapping
    public ResponseEntity<NewsQueryResponse<NewsDto>> getNewsByKeyword(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PageResult<NewsDto> result = newsQueryService.getNewsByKeyword(keyword, page, size);
        return ResponseEntity.ok(NewsQueryResponse.from(result));
    }
}
```