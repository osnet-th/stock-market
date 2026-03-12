# 프론트엔드 예시

## API 클라이언트 추가 (api.js)

```javascript
// 매수이력 조회
getPurchaseHistories(userId, itemId) {
    return this.request('GET', `/api/portfolio/items/stock/${itemId}/purchases?userId=${userId}`);
},

// 매수이력 수정
updatePurchaseHistory(userId, itemId, historyId, body) {
    return this.request('PUT', `/api/portfolio/items/stock/${itemId}/purchases/${historyId}?userId=${userId}`, body);
},

// 매수이력 삭제
deletePurchaseHistory(userId, itemId, historyId) {
    return this.request('DELETE', `/api/portfolio/items/stock/${itemId}/purchases/${historyId}?userId=${userId}`);
},
```

## Alpine.js 상태 추가 (app.js)

```javascript
// portfolio 객체에 추가
purchaseHistories: [],
editingHistory: null,
editHistoryForm: { quantity: '', purchasePrice: '', purchasedAt: '', memo: '' },
```

## 매수이력 관련 메서드 (app.js)

```javascript
// 추가매수 모달 열 때 이력도 함께 로드
async openPurchaseModal(item) {
    this.portfolio.purchaseItem = item;
    this.portfolio.purchaseForm = { quantity: '', purchasePrice: '' };
    this.portfolio.purchaseHistories = [];
    this.portfolio.editingHistory = null;
    this.portfolio.showPurchaseModal = true;
    await this.loadPurchaseHistories(item.id);
},

async loadPurchaseHistories(itemId) {
    try {
        this.portfolio.purchaseHistories = await API.getPurchaseHistories(this.auth.userId, itemId) || [];
    } catch (e) {
        console.error('매수이력 조회 실패:', e);
        this.portfolio.purchaseHistories = [];
    }
},

startEditHistory(history) {
    this.portfolio.editingHistory = history.id;
    this.portfolio.editHistoryForm = {
        quantity: history.quantity,
        purchasePrice: history.purchasePrice,
        purchasedAt: history.purchasedAt || '',
        memo: history.memo || ''
    };
},

cancelEditHistory() {
    this.portfolio.editingHistory = null;
},

async submitEditHistory(historyId) {
    var form = this.portfolio.editHistoryForm;
    var item = this.portfolio.purchaseItem;

    if (!form.quantity || Number(form.quantity) <= 0) {
        alert('수량을 입력해주세요.');
        return;
    }
    if (!form.purchasePrice || Number(form.purchasePrice) <= 0) {
        alert('매수 단가를 입력해주세요.');
        return;
    }

    try {
        await API.updatePurchaseHistory(this.auth.userId, item.id, historyId, {
            quantity: Number(form.quantity),
            purchasePrice: Number(form.purchasePrice),
            purchasedAt: form.purchasedAt || null,
            memo: form.memo || null
        });
        this.portfolio.editingHistory = null;
        await this.loadPurchaseHistories(item.id);
        await this.loadPortfolio();
    } catch (e) {
        console.error('매수이력 수정 실패:', e);
        alert('매수이력 수정에 실패했습니다.');
    }
},

async deleteHistory(historyId) {
    if (!confirm('이 매수 이력을 삭제하시겠습니까?\n삭제 후 수량과 평균단가가 재계산됩니다.')) return;
    var item = this.portfolio.purchaseItem;

    try {
        await API.deletePurchaseHistory(this.auth.userId, item.id, historyId);
        await this.loadPurchaseHistories(item.id);
        await this.loadPortfolio();
    } catch (e) {
        console.error('매수이력 삭제 실패:', e);
        alert(e.message || '매수이력 삭제에 실패했습니다.');
    }
},
```

## 추가매수 모달 HTML 변경 (index.html)

기존 추가매수 모달에 매수이력 목록 섹션 추가:

```html
<!-- 매수 이력 목록 (추가매수 모달 내, 현재 보유 정보 아래에 배치) -->
<template x-if="portfolio.purchaseHistories.length > 0">
    <div class="mt-4">
        <h4 class="text-sm font-semibold text-gray-700 mb-2">매수 이력</h4>
        <div class="space-y-2 max-h-48 overflow-y-auto">
            <template x-for="h in portfolio.purchaseHistories" :key="h.id">
                <div class="bg-gray-50 rounded-lg p-3 text-sm">
                    <!-- 보기 모드 -->
                    <template x-if="portfolio.editingHistory !== h.id">
                        <div>
                            <div class="flex justify-between items-center">
                                <div>
                                    <span class="font-medium text-gray-800"
                                        x-text="Format.number(h.quantity, 0) + '주'"></span>
                                    <span class="text-gray-500 mx-1">×</span>
                                    <span class="text-gray-800"
                                        x-text="Format.number(h.purchasePrice, 0) + '원'"></span>
                                    <span class="text-gray-400 ml-2"
                                        x-text="'= ' + Format.number(h.totalCost, 0) + '원'"></span>
                                </div>
                                <div class="flex gap-2">
                                    <button @click.stop="startEditHistory(h)"
                                        class="text-xs text-blue-600 hover:text-blue-800">수정</button>
                                    <button x-show="portfolio.purchaseHistories.length > 1"
                                        @click.stop="deleteHistory(h.id)"
                                        class="text-xs text-red-500 hover:text-red-700">삭제</button>
                                </div>
                            </div>
                            <div class="flex gap-3 mt-1 text-xs text-gray-400">
                                <span x-show="h.purchasedAt" x-text="h.purchasedAt"></span>
                                <span x-show="h.memo" x-text="h.memo"></span>
                            </div>
                        </div>
                    </template>
                    <!-- 수정 모드 -->
                    <template x-if="portfolio.editingHistory === h.id">
                        <div class="space-y-2">
                            <div class="flex gap-2">
                                <input type="number" x-model="portfolio.editHistoryForm.quantity"
                                    placeholder="수량" min="1"
                                    class="w-1/3 border border-gray-300 rounded px-2 py-1 text-sm">
                                <input type="number" x-model="portfolio.editHistoryForm.purchasePrice"
                                    placeholder="단가" min="1"
                                    class="w-2/3 border border-gray-300 rounded px-2 py-1 text-sm">
                            </div>
                            <div class="flex gap-2">
                                <input type="date" x-model="portfolio.editHistoryForm.purchasedAt"
                                    class="w-1/2 border border-gray-300 rounded px-2 py-1 text-sm">
                                <input type="text" x-model="portfolio.editHistoryForm.memo"
                                    placeholder="메모" maxlength="200"
                                    class="w-1/2 border border-gray-300 rounded px-2 py-1 text-sm">
                            </div>
                            <div class="flex justify-end gap-2">
                                <button @click.stop="cancelEditHistory()"
                                    class="text-xs text-gray-500 hover:text-gray-700">취소</button>
                                <button @click.stop="submitEditHistory(h.id)"
                                    class="text-xs text-blue-600 hover:text-blue-800 font-medium">저장</button>
                            </div>
                        </div>
                    </template>
                </div>
            </template>
        </div>
    </div>
</template>
```