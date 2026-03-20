# PortfolioService 뉴스 토글 변경 예시

## 변경 후

```java
@Transactional
public void toggleNews(Long userId, Long itemId, boolean enabled) {
    PortfolioItem item = findUserItem(userId, itemId);

    if (enabled) {
        item.enableNews();
        // 종목명으로 keyword 등록 + user_keyword 구독 생성
        String stockName = item.getItemName();
        Region region = item.getRegion();
        keywordService.registerKeyword(stockName, region, userId);
    } else {
        item.disableNews();
        // 종목명에 해당하는 keyword 찾아서 user_keyword 비활성화
        String stockName = item.getItemName();
        Region region = item.getRegion();
        keywordRepository.findByKeywordAndRegion(stockName, region)
                .ifPresent(keyword -> {
                    userKeywordRepository.findByUserIdAndKeywordId(userId, keyword.getId())
                            .ifPresent(UserKeyword::deactivate);
                });
    }

    portfolioItemRepository.save(item);
}
```

## 핵심 포인트

- 뉴스 ON: `keywordService.registerKeyword()`를 재사용하므로 기존 키워드가 있으면 자동으로 재사용됨
- 뉴스 OFF: `UserKeyword`를 비활성화만 함 (삭제 아님). 다른 사용자가 구독 중일 수 있으므로 keyword/news는 유지
- 포트폴리오 항목 삭제 시에는 `unsubscribeKeyword()`를 호출하여 구독자 0명이면 정리