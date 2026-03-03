# Presentation 예시 코드

## NewsCollectRequest

```java
package com.thlee.stock.market.stockmarket.news.presentation.dto;

import com.thlee.stock.market.stockmarket.news.domain.model.Region;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class NewsCollectRequest {
    private String keyword;
    private Long userId;
    private Region region;
}
```

## NewsCollectResponse

```java
package com.thlee.stock.market.stockmarket.news.presentation.dto;

import com.thlee.stock.market.stockmarket.news.application.dto.NewsBatchSaveResult;
import lombok.Getter;

@Getter
public class NewsCollectResponse {
    private final int successCount;
    private final int ignoredCount;
    private final int failedCount;

    private NewsCollectResponse(int successCount, int ignoredCount, int failedCount) {
        this.successCount = successCount;
        this.ignoredCount = ignoredCount;
        this.failedCount = failedCount;
    }

    public static NewsCollectResponse from(NewsBatchSaveResult result) {
        return new NewsCollectResponse(
                result.getSuccessCount(),
                result.getIgnoredCount(),
                result.getFailedCount()
        );
    }
}
```

## NewsController 추가 엔드포인트

```java
private final KeywordNewsBatchService keywordNewsBatchService;

/**
 * 키워드 뉴스 즉시 수집
 */
@PostMapping("/collect")
public ResponseEntity<NewsCollectResponse> collectNews(@RequestBody NewsCollectRequest request) {
    NewsBatchSaveResult result = keywordNewsBatchService.collectByKeyword(
            request.getKeyword(),
            request.getUserId(),
            request.getRegion()
    );
    return ResponseEntity.ok(NewsCollectResponse.from(result));
}
```