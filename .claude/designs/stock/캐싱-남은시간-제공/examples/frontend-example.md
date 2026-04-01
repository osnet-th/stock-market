# 프론트엔드 캐시 표시

## portfolio.js 변경

```javascript
// 캐시 남은시간 포맷팅 헬퍼
getCacheRemainingText(priceData) {
    if (!priceData || priceData.remainingSeconds === undefined) return '';

    const remaining = priceData.remainingSeconds;
    const minutes = Math.floor(remaining / 60);
    const seconds = remaining % 60;

    if (remaining <= 0) return '0초 후 갱신';
    if (remaining < 60) return `${seconds}초 후 갱신`;
    return `${minutes}분 ${seconds}초 후 갱신`;
},

// 캐싱 경과 시간 포맷팅 헬퍼
getCacheAgoText(priceData) {
    if (!priceData || !priceData.cachedAt) return '';

    const cachedAt = new Date(priceData.cachedAt);
    const now = new Date();
    const diffMs = now - cachedAt;
    const diffSeconds = Math.floor(diffMs / 1000);
    const diffMinutes = Math.floor(diffSeconds / 60);
    const remainSeconds = diffSeconds % 60;

    if (diffSeconds < 60) return `${diffSeconds}초 전 데이터`;
    return `${diffMinutes}분 ${remainSeconds}초 전 데이터`;
},
```

## index.html 변경 (포트폴리오 현재가 영역)

```html
<!-- 기존 현재가 표시 아래에 추가 -->
<template x-if="portfolio.stockPrices[item.stockDetail?.stockCode]">
    <div class="flex items-center gap-1 text-xs text-gray-400 mt-1">
        <span x-text="getCacheAgoText(portfolio.stockPrices[item.stockDetail.stockCode])"></span>
        <span>|</span>
        <span x-text="getCacheRemainingText(portfolio.stockPrices[item.stockDetail.stockCode])"></span>
    </div>
</template>
```

### 표시 예시

| 상태 | 표시 |
|------|------|
| 10초 경과 | `10초 전 데이터 | 29분 50초 후 갱신` |
| 1분 30초 경과 | `1분 30초 전 데이터 | 28분 30초 후 갱신` |
| 15분 경과 | `15분 0초 전 데이터 | 15분 0초 후 갱신` |
| 29분 45초 경과 | `29분 45초 전 데이터 | 15초 후 갱신` |
| 30분 경과 | `30분 0초 전 데이터 | 0초 후 갱신` |
