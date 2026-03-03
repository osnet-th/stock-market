# 키워드-뉴스 연쇄 삭제 구현 예시

## NewsRepository (도메인)

```java
/**
 * 사용자별 검색 키워드로 뉴스 삭제
 */
void deleteByUserIdAndSearchKeyword(Long userId, String searchKeyword);
```

## NewsJpaRepository (인프라)

```java
/**
 * 사용자별 검색 키워드로 뉴스 삭제
 */
void deleteByUserIdAndSearchKeyword(Long userId, String searchKeyword);
```

## NewsRepositoryImpl (인프라)

```java
@Override
public void deleteByUserIdAndSearchKeyword(Long userId, String searchKeyword) {
    newsJpaRepository.deleteByUserIdAndSearchKeyword(userId, searchKeyword);
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
    newsRepository.deleteByUserIdAndSearchKeyword(keyword.getUserId(), keyword.getKeyword());

    keywordRepository.delete(keyword);
}
```