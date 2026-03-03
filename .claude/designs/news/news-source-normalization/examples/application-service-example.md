# Application 서비스 예시

## NewsSaveRequest

```java
@Getter
public class NewsSaveRequest {
    private final String originalUrl;
    private final Long userId;
    private final String title;
    private final String content;
    private final LocalDateTime publishedAt;
    private final Long sourceId;
    private final Region region;

    public NewsSaveRequest(String originalUrl,
                           Long userId,
                           String title,
                           String content,
                           LocalDateTime publishedAt,
                           Long sourceId,
                           Region region) {
        this.originalUrl = originalUrl;
        this.userId = userId;
        this.title = title;
        this.content = content;
        this.publishedAt = publishedAt;
        this.sourceId = sourceId;
        this.region = region;
    }
}
```

## NewsDto

```java
@Getter
public class NewsDto {
    private final Long id;
    private final String originalUrl;
    private final String title;
    private final String content;
    private final LocalDateTime publishedAt;
    private final LocalDateTime createdAt;
    private final NewsPurpose purpose;
    private final Long sourceId;

    // 생성자, from() 메서드에서 searchKeyword → sourceId 변경
}
```

## KeywordSearchContext

```java
public record KeywordSearchContext(
        Long userId,
        Long keywordId,      // keyword.getId()
        Region region,
        NewsResultDto news
) {
}
```

## NewsQueryService

```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NewsQueryService {

    private final NewsRepository newsRepository;

    // 기존 getByPurpose, findExistingUrls 유지

    /**
     * purpose + sourceId 기반 뉴스 조회
     */
    public PageResult<NewsDto> getNewsBySource(NewsPurpose purpose, Long sourceId, int page, int size) {
        PageResult<News> result = newsRepository.findByPurposeAndSourceId(purpose, sourceId, page, size);

        List<NewsDto> dtoList = result.getContent().stream()
                .map(NewsDto::from)
                .collect(Collectors.toList());

        return new PageResult<>(dtoList, result.getPage(), result.getSize(), result.getTotalElements());
    }
}
```

## NewsSaveService 변경 부분

```java
// save 메서드 내 News.create() 호출
News news = News.create(
        request.getOriginalUrl(),
        request.getUserId(),
        request.getTitle(),
        request.getContent(),
        request.getPublishedAt(),
        purpose,
        request.getSourceId(),   // searchKeyword → sourceId
        request.getRegion()
);
```

## KeywordNewsBatchService 인터페이스 변경

```java
public interface KeywordNewsBatchService {
    /**
     * 활성화된 키워드 기반으로 뉴스를 조회하고 저장
     * @return 저장된 신규 뉴스 건수
     */
    int executeKeywordNewsBatch();

    /**
     * 단건 키워드 즉시 뉴스 수집
     * @param keywordId keyword.id (news 저장 시 source_id로 사용)
     * @param keyword   외부 뉴스 API 검색용 텍스트
     */
    NewsBatchSaveResult collectByKeyword(Long keywordId, String keyword, Long userId, Region region);
}
```

## KeywordNewsBatchServiceImpl 변경 부분

### collectByKeyword (단건 수집)

```java
@Override
public NewsBatchSaveResult collectByKeyword(Long keywordId, String keyword, Long userId, Region region) {
    // 1. 외부 뉴스 API 검색은 keyword TEXT로 수행
    List<NewsResultDto> searchResults = newsSearchService.search(keyword, region);
    if (searchResults.isEmpty()) {
        return new NewsBatchSaveResult(0, 0, 0);
    }

    // 2. context 생성 시 keywordId 사용
    List<KeywordSearchContext> contexts = searchResults.stream()
            .map(dto -> new KeywordSearchContext(userId, keywordId, region, dto))
            .toList();

    List<KeywordSearchContext> newContexts = filterNewNews(contexts);
    if (newContexts.isEmpty()) {
        return new NewsBatchSaveResult(0, 0, 0);
    }

    // 3. saveRequest에 keywordId를 sourceId로 전달
    List<NewsSaveRequest> saveRequests = newContexts.stream()
            .map(context -> new NewsSaveRequest(
                    context.news().getUrl(),
                    context.userId(),
                    context.news().getTitle(),
                    context.news().getContent(),
                    context.news().getPublishedAt(),
                    context.keywordId(),    // sourceId
                    context.region()
            ))
            .toList();

    return newsSaveService.saveBatch(saveRequests, NewsPurpose.KEYWORD);
}
```

### executeKeywordNewsBatch (배치 수집)

```java
// executeKeywordNewsBatch 내 context 생성 부분
for (Keyword keyword : activeKeywords) {
    List<NewsResultDto> searchResults = newsSearchService.search(keyword.getKeyword(), keyword.getRegion());
    for (NewsResultDto dto : searchResults) {
        searchedContexts.add(new KeywordSearchContext(
                keyword.getUserId(),
                keyword.getId(),        // keyword text → keyword id
                keyword.getRegion(),
                dto
        ));
    }
}

// saveRequests 생성 부분
List<NewsSaveRequest> saveRequests = newContexts.stream()
        .map(context -> new NewsSaveRequest(
                context.news().getUrl(),
                context.userId(),
                context.news().getTitle(),
                context.news().getContent(),
                context.news().getPublishedAt(),
                context.keywordId(),    // searchKeyword → keywordId (sourceId)
                context.region()
        ))
        .toList();
```