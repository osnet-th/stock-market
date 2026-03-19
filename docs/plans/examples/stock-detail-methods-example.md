# openStockDetail / closeStockDetail + ESC 키 예시

## app.js

```javascript
// openStockDetail은 기존과 동일
async openStockDetail(item) {
    var stockCode = item.stockDetail?.stockCode;
    if (!stockCode || item.stockDetail?.country !== 'KR') return;

    this.portfolio.selectedStockItem = item;
    this.portfolio.financialYear = this.getDefaultYear();
    this.portfolio.financialReportCode = 'ANNUAL';
    this.portfolio.selectedFinancialMenu = null;
    this.portfolio.financialResult = null;

    await this.loadFinancialOptions();
},

closeStockDetail() {
    // 바 차트 인스턴스 정리
    if (this.portfolio.financialChartInstance) {
        this.portfolio.financialChartInstance.destroy();
        this.portfolio.financialChartInstance = null;
    }
    this.portfolio.selectedStockItem = null;
    this.portfolio.selectedFinancialMenu = null;
    this.portfolio.financialResult = null;
}
```

## index.html - ESC 키 핸들링

슬라이드 패널 본체에 `@keydown.escape.window` 추가:

```html
<div x-show="portfolio.selectedStockItem"
     @keydown.escape.window="closeStockDetail()"
     ...>
```

### 변경 포인트

- `closeStockDetail()`에 바 차트 인스턴스 destroy 로직 추가
- `portfolio.financialChartInstance` 상태 변수 추가 필요 (초기값 `null`)
- ESC 키: Alpine.js의 `@keydown.escape.window` 디렉티브로 간단하게 처리