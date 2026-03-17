# 헬퍼 함수 예시

## app.js — getItemsByType 근처에 추가

```javascript
getDomesticStocks() {
    return this.portfolio.items.filter(function(item) {
        return item.assetType === 'STOCK' && item.stockDetail?.country === 'KR';
    });
},

getOverseasStocks() {
    return this.portfolio.items.filter(function(item) {
        return item.assetType === 'STOCK' && item.stockDetail?.country !== 'KR';
    });
},
```