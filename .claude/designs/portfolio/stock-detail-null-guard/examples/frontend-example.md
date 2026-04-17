# frontend example

## 추가 매수 모달 렌더 보호

위치: `src/main/resources/static/index.html`

```html
<template x-if="portfolio.showPurchaseModal && portfolio.purchaseItem">
    <div class="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
        <div class="bg-white rounded-2xl shadow-xl w-full max-w-md mx-4 p-6"
             @click.outside="closePurchaseModal()">

            <p x-text="portfolio.purchaseItem.itemName"></p>

            <template x-if="portfolio.purchaseItem.stockDetail">
                <div>
                    <span x-text="Format.number(portfolio.purchaseItem.stockDetail.quantity, 0) + '주'"></span>
                </div>
            </template>

            <label>
                매수 단가
                <span x-show="portfolio.purchaseItem?.stockDetail?.priceCurrency && portfolio.purchaseItem?.stockDetail?.priceCurrency !== 'KRW'"
                      x-text="'(' + portfolio.purchaseItem?.stockDetail?.priceCurrency + ')'"></span>
            </label>

            <input type="number"
                   :step="portfolio.purchaseItem?.stockDetail?.priceCurrency && portfolio.purchaseItem?.stockDetail?.priceCurrency !== 'KRW' ? '0.01' : '1'"
                   :placeholder="portfolio.purchaseItem?.stockDetail?.priceCurrency && portfolio.purchaseItem?.stockDetail?.priceCurrency !== 'KRW' ? '1주당 매수가 (외화)' : '1주당 매수가'">
        </div>
    </div>
</template>
```

## 모달 닫기 메서드

위치: `src/main/resources/static/js/components/portfolio.js`

```javascript
closePurchaseModal() {
    this.portfolio.showPurchaseModal = false;
    this.portfolio.purchaseItem = null;
    this.portfolio.purchaseForm = { quantity: '', purchasePrice: '' };
    this.portfolio.purchaseHistories = [];
    this.portfolio.editingHistory = null;
},
```

## 버튼 노출 및 이상 데이터 감지

위치: `src/main/resources/static/index.html`, `src/main/resources/static/js/components/portfolio.js`

```html
<button x-show="item.assetType === 'STOCK' && item.stockDetail"
        @click.stop="openPurchaseModal(item)">
    추가 매수
</button>
```

```javascript
getOverseasStocks() {
    return this.portfolio.items.filter(
        (item) => item.assetType === 'STOCK' && item.stockDetail && item.stockDetail.country !== 'KR'
    );
},

async loadPortfolio() {
    const results = await Promise.all([
        API.getPortfolioItems(this.auth.userId),
        API.getPortfolioAllocation(this.auth.userId)
    ]);

    this.portfolio.items = results[0] || [];

    const invalidStockItems = this.portfolio.items.filter(
        (item) => item.assetType === 'STOCK' && !item.stockDetail
    );

    invalidStockItems.forEach((item) => {
        console.warn('STOCK item without stockDetail', {
            id: item.id,
            itemName: item.itemName,
            assetType: item.assetType
        });
    });
}
```
