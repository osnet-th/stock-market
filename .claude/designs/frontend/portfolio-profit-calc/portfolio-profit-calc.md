# 포트폴리오 수익률/수익금 계산 (프론트엔드)

## 개요

포트폴리오 로드 시 주식 항목의 현재가를 벌크 조회하여, 수익금/수익률을 계산하고 자산 비중을 (원금+수익금) 기준으로 표시한다.

## 핵심 결정

- **계산 위치**: 프론트엔드(JS)에서만 계산. 백엔드 변경 없음
- **현재가 조회**: 기존 `POST /api/stocks/prices` 벌크 API 사용
- **자산 비중 기준 변경**: 기존 `investedAmount`(원금) → `원금 + 수익금`(평가금액)으로 변경

## 계산 공식

```
평가금액 = 현재가 × 수량
수익금 = 평가금액 - 원금(investedAmount)
수익률(%) = ((현재가 - 평균단가) / 평균단가) × 100
자산 비중(%) = (해당 항목의 평가금액 / 전체 자산 평가금액 합계) × 100
```

- 주식이 아닌 항목(채권, 부동산 등): 현재가 조회 불가 → 평가금액 = 원금(investedAmount) 그대로 사용
- 현재가 조회 실패한 주식: 원금 기준 유지

## UI 변경

### 1. 항목 요약 (getItemSummary)

**현재**:
```
KOSPI:005930 · 10주 · 평균 80,000원
```

**변경 후**:
```
KOSPI:005930 · 10주 · 평균 80,000원 · 현재가 82,000원 · +25,000원(+2.50%)
```

### 2. 항목 목록 금액 표시

**현재** (항목 이름 옆):
```
삼성전자  800,000원
```

**변경 후**:
```
삼성전자  820,000원 (원금 800,000원)
```
- 첫 번째 금액: 평가금액(원금+수익금) - 현재가 조회 성공 시
- 괄호 안: 원금(investedAmount)
- 수익이면 빨간색, 손실이면 파란색으로 평가금액 표시

### 3. 요약 카드

**현재**: `총 투자 금액`만 표시

**변경 후**:
- 총 투자 금액 (원금 합계) - 기존 유지
- 총 평가 금액 (평가금액 합계) - 신규 추가
- 총 수익금 + 수익률 - 신규 추가

### 4. 자산 비중 바 차트

- 기존: `investedAmount` 기반 비중
- 변경: 평가금액(원금+수익금) 기반 비중으로 프론트에서 재계산
- 백엔드 allocation API는 그대로 두고, 프론트에서 현재가 반영된 비중을 직접 계산

## 작업 리스트

### app.js 변경

- [x] 1. `portfolio` 상태에 `stockPrices: {}` 필드 추가 (종목코드 → 현재가 매핑)
- [x] 2. `loadPortfolio()` 에서 포트폴리오 로드 후, 주식 항목들의 현재가를 벌크 조회하여 `stockPrices`에 저장
- [x] 3. `getEvalAmount(item)` 메서드 추가 - 항목의 평가금액 반환 (주식: 현재가×수량, 비주식: investedAmount)
- [x] 4. `getProfitAmount(item)` 메서드 추가 - 수익금 반환 (평가금액 - 원금)
- [x] 5. `getProfitRate(item)` 메서드 추가 - 수익률(%) 반환
- [x] 6. `getTotalEvalAmount()` 메서드 추가 - 전체 평가금액 합계
- [x] 7. `getEvalAllocation()` 메서드 추가 - 평가금액 기반 자산 비중 배열 반환
- [x] 8. `getItemSummary(item)` 수정 - 주식 항목에 현재가/수익금/수익률 표시 추가

### api.js 변경

- [x] 9. `getStockPrices(stocks)` 메서드 추가 - `POST /api/stocks/prices` 호출

### index.html 변경

- [x] 10. 요약 카드 영역: 총 평가금액, 총 수익금/수익률 카드 추가
- [x] 11. 항목 목록 금액: `investedAmount` → 평가금액 (원금) 형태로 변경
- [x] 12. 자산 비중 바 차트: `portfolio.allocation` → `getEvalAllocation()` 사용으로 변경

## 예시 코드

### api.js - getStockPrices

```javascript
getStockPrices(stocks) {
    return this.request('POST', '/api/stocks/prices', { stocks });
}
```

### app.js - loadPortfolio 변경

```javascript
async loadPortfolio() {
    this.portfolio.loading = true;
    try {
        var results = await Promise.all([
            API.getPortfolioItems(this.auth.userId),
            API.getPortfolioAllocation(this.auth.userId)
        ]);
        this.portfolio.items = results[0] || [];
        this.portfolio.allocation = results[1] || [];

        // 주식 항목의 현재가 벌크 조회
        await this.loadStockPrices();

        // ... expandedSections 초기화 (기존 코드)
    } catch (e) { ... }
}
```

### app.js - loadStockPrices

```javascript
async loadStockPrices() {
    var stockItems = this.portfolio.items.filter(function(item) {
        return item.assetType === 'STOCK' && item.stockDetail;
    });
    if (stockItems.length === 0) {
        this.portfolio.stockPrices = {};
        return;
    }

    var stocks = stockItems.map(function(item) {
        return {
            stockCode: item.stockDetail.stockCode,
            marketType: item.stockDetail.market,
            exchangeCode: item.stockDetail.exchangeCode
        };
    });

    try {
        var result = await API.getStockPrices(stocks);
        this.portfolio.stockPrices = result.prices || {};
    } catch (e) {
        console.error('현재가 조회 실패:', e);
        this.portfolio.stockPrices = {};
    }
}
```

### app.js - 계산 메서드들

```javascript
getEvalAmount(item) {
    if (item.assetType === 'STOCK' && item.stockDetail) {
        var priceData = this.portfolio.stockPrices[item.stockDetail.stockCode];
        if (priceData && priceData.currentPrice) {
            return parseFloat(priceData.currentPrice) * item.stockDetail.quantity;
        }
    }
    return item.investedAmount;
},

getProfitAmount(item) {
    return this.getEvalAmount(item) - item.investedAmount;
},

getProfitRate(item) {
    if (item.assetType !== 'STOCK' || !item.stockDetail || !item.stockDetail.avgBuyPrice) return null;
    var priceData = this.portfolio.stockPrices[item.stockDetail.stockCode];
    if (!priceData || !priceData.currentPrice) return null;
    var currentPrice = parseFloat(priceData.currentPrice);
    var avgPrice = parseFloat(item.stockDetail.avgBuyPrice);
    if (avgPrice === 0) return null;
    return ((currentPrice - avgPrice) / avgPrice * 100);
},

getTotalEvalAmount() {
    return this.portfolio.items.reduce(function(sum, item) {
        return sum + this.getEvalAmount(item);
    }.bind(this), 0);
},

getEvalAllocation() {
    var totalEval = this.getTotalEvalAmount();
    if (totalEval === 0) return this.portfolio.allocation;

    var typeMap = {};
    this.portfolio.items.forEach(function(item) {
        if (!typeMap[item.assetType]) {
            typeMap[item.assetType] = { assetType: item.assetType, assetTypeName: this.getAssetTypeLabel(item.assetType), totalAmount: 0 };
        }
        typeMap[item.assetType].totalAmount += this.getEvalAmount(item);
    }.bind(this));

    return Object.values(typeMap).map(function(alloc) {
        alloc.percentage = Math.round(alloc.totalAmount / totalEval * 1000) / 10;
        return alloc;
    });
}
```

## 주의사항

- 현재가 조회 실패 시 원금 기준으로 fallback (에러가 UI를 깨뜨리지 않도록)
- `StockPriceResponse.currentPrice`는 String 타입이므로 `parseFloat()` 필요
- 비주식 항목(채권, 부동산 등)은 현재가 조회 대상이 아니므로 `investedAmount` 그대로 사용