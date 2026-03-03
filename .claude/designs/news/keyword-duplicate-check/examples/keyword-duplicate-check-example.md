# 키워드 중복 검사 구현 예시

## KeywordRepository (도메인)

```java
/**
 * 사용자별 키워드 존재 여부 확인
 */
boolean existsByUserIdAndKeyword(Long userId, String keyword);
```

## KeywordJpaRepository (인프라)

```java
/**
 * 사용자별 키워드 존재 여부 확인
 */
boolean existsByUserIdAndKeyword(Long userId, String keyword);
```

## KeywordRepositoryImpl (인프라)

```java
@Override
public boolean existsByUserIdAndKeyword(Long userId, String keyword) {
    return keywordJpaRepository.existsByUserIdAndKeyword(userId, keyword);
}
```

## KeywordServiceImpl.registerKeyword() 변경

```java
@Transactional
public Keyword registerKeyword(RegisterKeywordRequest request) {
    if (keywordRepository.existsByUserIdAndKeyword(request.getUserId(), request.getKeyword())) {
        throw new IllegalArgumentException("이미 등록된 키워드입니다.");
    }

    Keyword newKeyword = Keyword.create(request.getKeyword(), request.getUserId(), request.getRegion());
    return keywordRepository.save(newKeyword);
}
```