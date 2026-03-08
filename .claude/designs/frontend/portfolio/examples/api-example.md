# Portfolio API 예시 코드

## api.js 추가 메서드

```javascript
// ==================== Stock Search ====================
searchStocks(name) {
    return this.request('GET', `/api/stocks/search?name=${encodeURIComponent(name)}`);
},

// ==================== Portfolio ====================
getPortfolioItems(userId) {
    return this.request('GET', `/api/portfolio/items?userId=${userId}`);
},

getPortfolioAllocation(userId) {
    return this.request('GET', `/api/portfolio/allocation?userId=${userId}`);
},

// 등록 (타입별)
addStockItem(userId, body) {
    return this.request('POST', `/api/portfolio/items/stock?userId=${userId}`, body);
},

addBondItem(userId, body) {
    return this.request('POST', `/api/portfolio/items/bond?userId=${userId}`, body);
},

addRealEstateItem(userId, body) {
    return this.request('POST', `/api/portfolio/items/real-estate?userId=${userId}`, body);
},

addFundItem(userId, body) {
    return this.request('POST', `/api/portfolio/items/fund?userId=${userId}`, body);
},

addGeneralItem(userId, body) {
    return this.request('POST', `/api/portfolio/items/general?userId=${userId}`, body);
},

// 수정 (타입별)
updateStockItem(userId, itemId, body) {
    return this.request('PUT', `/api/portfolio/items/stock/${itemId}?userId=${userId}`, body);
},

updateBondItem(userId, itemId, body) {
    return this.request('PUT', `/api/portfolio/items/bond/${itemId}?userId=${userId}`, body);
},

updateRealEstateItem(userId, itemId, body) {
    return this.request('PUT', `/api/portfolio/items/real-estate/${itemId}?userId=${userId}`, body);
},

updateFundItem(userId, itemId, body) {
    return this.request('PUT', `/api/portfolio/items/fund/${itemId}?userId=${userId}`, body);
},

updateGeneralItem(userId, itemId, body) {
    return this.request('PUT', `/api/portfolio/items/general/${itemId}?userId=${userId}`, body);
},

// 삭제
deletePortfolioItem(userId, itemId) {
    return this.request('DELETE', `/api/portfolio/items/${itemId}?userId=${userId}`);
},

// 뉴스 토글
togglePortfolioNews(userId, itemId, enabled) {
    return this.request('PATCH', `/api/portfolio/items/${itemId}/news?userId=${userId}`, { enabled });
}
```