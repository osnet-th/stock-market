# KeywordService 변경 예시

## KeywordService 인터페이스

```java
public interface KeywordService {
    Keyword registerKeyword(String keyword, Region region, Long userId);
    List<Keyword> getKeywordsByUser(Long userId);
    List<Keyword> getActiveKeywordsByUser(Long userId);
    List<Keyword> getAllActiveKeywords();
    void activateKeyword(Long keywordId);
    void deactivateKeyword(Long keywordId);
    void unsubscribeKeyword(Long userId, Long keywordId);
}
```

## KeywordServiceImpl 핵심 메서드

```java
@Override
@Transactional
public Keyword registerKeyword(String keywordText, Region region, Long userId) {
    // 1. 기존 키워드 재사용 또는 신규 생성
    Keyword keyword = keywordRepository.findByKeywordAndRegion(keywordText, region)
            .orElseGet(() -> {
                Keyword newKeyword = Keyword.create(keywordText, region);
                return keywordRepository.save(newKeyword);
            });

    // 2. UserKeyword 구독 생성 (이미 존재하면 활성화)
    userKeywordRepository.findByUserIdAndKeywordId(userId, keyword.getId())
            .ifPresentOrElse(
                    UserKeyword::activate,
                    () -> {
                        UserKeyword subscription = UserKeyword.create(userId, keyword.getId());
                        userKeywordRepository.save(subscription);
                    }
            );

    return keyword;
}

@Override
@Transactional(readOnly = true)
public List<Keyword> getKeywordsByUser(Long userId) {
    // user_keyword에서 사용자의 구독 목록 조회 → keyword 조회
    List<UserKeyword> subscriptions = userKeywordRepository.findByUserId(userId);
    return subscriptions.stream()
            .map(sub -> keywordRepository.findById(sub.getKeywordId()))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());
}

@Override
@Transactional
public void unsubscribeKeyword(Long userId, Long keywordId) {
    // 1. UserKeyword 삭제
    userKeywordRepository.deleteByUserIdAndKeywordId(userId, keywordId);

    // 2. 해당 keyword 구독자가 0명이면 keyword + news 일괄 삭제
    boolean hasSubscribers = userKeywordRepository.existsByKeywordId(keywordId);
    if (!hasSubscribers) {
        newsRepository.deleteByKeywordId(keywordId);
        keywordRepository.deleteById(keywordId);
    }
}
```