# Application 예시 코드

## KeywordNewsBatchService 인터페이스 추가

```java
/**
 * 단건 키워드 즉시 뉴스 수집
 */
NewsBatchSaveResult collectByKeyword(String keyword, Long userId, Region region);
```

## KeywordNewsBatchServiceImpl 구현

```java
@Override
public NewsBatchSaveResult collectByKeyword(String keyword, Long userId, Region region) {
    List<NewsResultDto> searchResults = newsSearchService.search(keyword, region);
    if (searchResults.isEmpty()) {
        return new NewsBatchSaveResult(0, 0, 0);
    }

    List<KeywordSearchContext> contexts = searchResults.stream()
            .map(dto -> new KeywordSearchContext(userId, keyword, region, dto))
            .toList();

    List<KeywordSearchContext> newContexts = filterNewNews(contexts);
    if (newContexts.isEmpty()) {
        return new NewsBatchSaveResult(0, 0, 0);
    }

    List<NewsSaveRequest> saveRequests = newContexts.stream()
            .map(context -> new NewsSaveRequest(
                    context.news().getUrl(),
                    context.userId(),
                    context.news().getTitle(),
                    context.news().getContent(),
                    context.news().getPublishedAt(),
                    context.searchKeyword(),
                    context.region()
            ))
            .toList();

    return newsSaveService.saveBatch(saveRequests, NewsPurpose.KEYWORD);
}
```