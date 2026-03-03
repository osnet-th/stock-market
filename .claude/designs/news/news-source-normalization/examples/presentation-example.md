# Presentation 예시

## NewsController

```java
@RestController
@RequestMapping("/api/news")
@RequiredArgsConstructor
public class NewsController {

    private final NewsQueryService newsQueryService;
    private final KeywordNewsBatchService keywordNewsBatchService;

    /**
     * 키워드 ID 기반 뉴스 조회 (페이징)
     */
    @GetMapping
    public ResponseEntity<NewsQueryResponse<NewsDto>> getNewsByKeyword(
            @RequestParam Long keywordId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PageResult<NewsDto> result = newsQueryService.getNewsBySource(
                NewsPurpose.KEYWORD, keywordId, page, size);
        return ResponseEntity.ok(NewsQueryResponse.from(result));
    }

    /**
     * 키워드 뉴스 즉시 수집
     */
    @PostMapping("/collect")
    public ResponseEntity<NewsCollectResponse> collectNews(@RequestBody NewsCollectRequest request) {
        NewsBatchSaveResult result = keywordNewsBatchService.collectByKeyword(
                request.getKeywordId(),
                request.getKeyword(),
                request.getUserId(),
                request.getRegion()
        );
        return ResponseEntity.ok(NewsCollectResponse.from(result));
    }

    // 향후 Stock 추가 시:
    // @GetMapping("/stock")
    // public ResponseEntity<NewsQueryResponse<NewsDto>> getNewsByStock(
    //         @RequestParam Long stockId, ...) {
    //     PageResult<NewsDto> result = newsQueryService.getNewsBySource(
    //             NewsPurpose.STOCK, stockId, page, size);
    //     ...
    // }
}
```