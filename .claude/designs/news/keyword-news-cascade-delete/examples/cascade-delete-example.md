# 키워드-뉴스 연쇄 삭제 구현 예시

## NewsRepository (도메인)

```java
void deleteByPurposeAndSourceId(NewsPurpose purpose, Long sourceId);
```

## NewsJpaRepository (인프라)

```java
void deleteByPurposeAndSourceId(NewsPurpose purpose, Long sourceId);
```

## NewsRepositoryImpl (인프라)

```java
@Override
public void deleteByPurposeAndSourceId(NewsPurpose purpose, Long sourceId) {
    newsJpaRepository.deleteByPurposeAndSourceId(purpose, sourceId);
}
```

## KeywordServiceImpl.deleteKeyword() 변경

```java
private final KeywordRepository keywordRepository;
private final NewsRepository newsRepository;

@Transactional
public void deleteKeyword(Long keywordId) {
    Keyword keyword = keywordRepository.findById(keywordId)
            .orElseThrow(() -> new IllegalArgumentException("키워드를 찾을 수 없습니다."));

    // 관련 뉴스 먼저 삭제
    newsRepository.deleteByPurposeAndSourceId(NewsPurpose.KEYWORD, keyword.getId());

    keywordRepository.delete(keyword);
}
```