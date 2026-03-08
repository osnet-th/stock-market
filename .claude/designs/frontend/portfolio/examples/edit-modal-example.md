# 수정 모달 예시 코드

## app.js — 수정 모달 메서드

```javascript
// ==================== 수정 모달 ====================

openEditModal(item) {
    this.portfolio.editingItem = item;
    this.portfolio.stockSearch = { query: '', results: [], loading: false, selected: null, debounceTimer: null };

    // 기존 데이터로 폼 채움
    var form = {
        itemName: item.itemName,
        investedAmount: item.investedAmount,
        memo: item.memo || ''
    };

    switch (item.assetType) {
        case 'STOCK':
            if (item.stockDetail) {
                form.subType = item.stockDetail.subType;
                form.ticker = item.stockDetail.ticker;
                form.exchange = item.stockDetail.exchange;
                form.quantity = item.stockDetail.quantity;
                form.avgBuyPrice = item.stockDetail.avgBuyPrice;
                form.dividendYield = item.stockDetail.dividendYield;
            }
            break;
        case 'BOND':
            if (item.bondDetail) {
                form.subType = item.bondDetail.subType;
                form.maturityDate = item.bondDetail.maturityDate;
                form.couponRate = item.bondDetail.couponRate;
                form.creditRating = item.bondDetail.creditRating;
            }
            break;
        case 'REAL_ESTATE':
            if (item.realEstateDetail) {
                form.subType = item.realEstateDetail.subType;
                form.address = item.realEstateDetail.address;
                form.area = item.realEstateDetail.area;
            }
            break;
        case 'FUND':
            if (item.fundDetail) {
                form.subType = item.fundDetail.subType;
                form.managementFee = item.fundDetail.managementFee;
            }
            break;
    }

    this.portfolio.editForm = form;
    this.portfolio.showEditModal = true;
},

// 수정 시 주식 검색 (추가 모달과 동일 로직 재사용)
onEditStockSearchInput() {
    var self = this;
    if (self.portfolio.stockSearch.debounceTimer) {
        clearTimeout(self.portfolio.stockSearch.debounceTimer);
    }
    var query = self.portfolio.stockSearch.query.trim();
    if (query.length < 1) {
        self.portfolio.stockSearch.results = [];
        return;
    }
    self.portfolio.stockSearch.debounceTimer = setTimeout(function() {
        self.searchStockForEdit();
    }, 300);
},

async searchStockForEdit() {
    var query = this.portfolio.stockSearch.query.trim();
    if (!query) return;
    this.portfolio.stockSearch.loading = true;
    try {
        this.portfolio.stockSearch.results = await API.searchStocks(query) || [];
    } catch (e) {
        console.error('종목 검색 실패:', e);
        this.portfolio.stockSearch.results = [];
    } finally {
        this.portfolio.stockSearch.loading = false;
    }
},

selectStockForEdit(stock) {
    this.portfolio.stockSearch.selected = stock;
    this.portfolio.stockSearch.results = [];
    this.portfolio.stockSearch.query = '';
    this.portfolio.editForm.itemName = stock.stockName;
    this.portfolio.editForm.ticker = stock.stockCode;
    this.portfolio.editForm.exchange = stock.marketType;
},

async submitEditItem() {
    var item = this.portfolio.editingItem;
    var form = this.portfolio.editForm;
    var userId = this.auth.userId;

    if (!form.itemName || !form.investedAmount) {
        alert('항목명과 투자 금액은 필수입니다.');
        return;
    }
    if (Number(form.investedAmount) <= 0) {
        alert('투자 금액은 0보다 커야 합니다.');
        return;
    }

    try {
        switch (item.assetType) {
            case 'STOCK':
                await API.updateStockItem(userId, item.id, {
                    itemName: form.itemName,
                    investedAmount: Number(form.investedAmount),
                    memo: form.memo || null,
                    subType: form.subType || 'INDIVIDUAL',
                    ticker: form.ticker,
                    exchange: form.exchange,
                    quantity: form.quantity ? Number(form.quantity) : null,
                    avgBuyPrice: form.avgBuyPrice ? Number(form.avgBuyPrice) : null,
                    dividendYield: form.dividendYield ? Number(form.dividendYield) : null
                });
                break;
            case 'BOND':
                await API.updateBondItem(userId, item.id, {
                    itemName: form.itemName,
                    investedAmount: Number(form.investedAmount),
                    memo: form.memo || null,
                    subType: form.subType || 'GOVERNMENT',
                    maturityDate: form.maturityDate || null,
                    couponRate: form.couponRate ? Number(form.couponRate) : null,
                    creditRating: form.creditRating || null
                });
                break;
            case 'REAL_ESTATE':
                await API.updateRealEstateItem(userId, item.id, {
                    itemName: form.itemName,
                    investedAmount: Number(form.investedAmount),
                    memo: form.memo || null,
                    subType: form.subType || 'APARTMENT',
                    address: form.address || null,
                    area: form.area ? Number(form.area) : null
                });
                break;
            case 'FUND':
                await API.updateFundItem(userId, item.id, {
                    itemName: form.itemName,
                    investedAmount: Number(form.investedAmount),
                    memo: form.memo || null,
                    subType: form.subType || 'EQUITY_FUND',
                    managementFee: form.managementFee ? Number(form.managementFee) : null
                });
                break;
            default:
                await API.updateGeneralItem(userId, item.id, {
                    itemName: form.itemName,
                    investedAmount: Number(form.investedAmount),
                    memo: form.memo || null
                });
                break;
        }
        this.portfolio.showEditModal = false;
        this.portfolio.editingItem = null;
        await this.loadPortfolio();
    } catch (e) {
        console.error('자산 수정 실패:', e);
        alert('수정에 실패했습니다.');
    }
}
```

## index.html — 수정 모달

```html
<!-- 자산 수정 모달 -->
<div x-show="portfolio.showEditModal" x-cloak
    class="modal-backdrop fixed inset-0 bg-black/30 flex items-center justify-center z-50">
    <div class="bg-white rounded-xl p-6 w-[480px] max-h-[80vh] overflow-y-auto shadow-xl"
        @click.outside="portfolio.showEditModal = false">

        <h3 class="text-lg font-bold text-gray-800 mb-1">자산 수정</h3>
        <template x-if="portfolio.editingItem">
            <p class="text-xs text-gray-400 mb-4">
                <span :class="getAssetTypeBadgeClass(portfolio.editingItem.assetType)"
                    class="px-2 py-0.5 rounded-full"
                    x-text="getAssetTypeLabel(portfolio.editingItem.assetType)"></span>
            </p>
        </template>

        <template x-if="portfolio.editingItem">
            <div class="space-y-3">

                <!-- ===== 주식 수정: 종목 재검색 가능 ===== -->
                <template x-if="portfolio.editingItem.assetType === 'STOCK'">
                    <div>
                        <!-- 종목 재검색 -->
                        <div class="mb-3">
                            <label class="block text-sm text-gray-600 mb-1">종목 변경 (선택)</label>
                            <div class="relative">
                                <input x-model="portfolio.stockSearch.query"
                                    @input="onEditStockSearchInput()"
                                    placeholder="다른 종목으로 변경하려면 검색"
                                    class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent">
                                <template x-if="portfolio.stockSearch.loading">
                                    <span class="absolute right-3 top-2.5 text-xs text-gray-400">검색 중...</span>
                                </template>
                            </div>
                            <template x-if="portfolio.stockSearch.results.length > 0">
                                <div class="mt-1 bg-white border border-gray-200 rounded-lg shadow-lg max-h-40 overflow-y-auto">
                                    <template x-for="stock in portfolio.stockSearch.results" :key="stock.stockCode">
                                        <button @click="selectStockForEdit(stock)"
                                            class="w-full text-left px-3 py-2 text-sm hover:bg-blue-50 transition border-b border-gray-50 last:border-b-0">
                                            <span class="font-medium text-gray-800" x-text="stock.stockName"></span>
                                            <span class="text-gray-400 ml-1" x-text="'(' + stock.stockCode + ')'"></span>
                                            <span class="text-xs text-gray-400 ml-1" x-text="stock.marketType"></span>
                                        </button>
                                    </template>
                                </div>
                            </template>
                        </div>

                        <div>
                            <label class="block text-sm text-gray-600 mb-1">유형</label>
                            <select x-model="portfolio.editForm.subType"
                                class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500">
                                <option value="INDIVIDUAL">개별주식</option>
                                <option value="ETF">ETF</option>
                            </select>
                        </div>
                        <div class="grid grid-cols-3 gap-2">
                            <div>
                                <label class="block text-sm text-gray-600 mb-1">보유 수량</label>
                                <input type="number" x-model="portfolio.editForm.quantity" placeholder="0"
                                    class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500">
                            </div>
                            <div>
                                <label class="block text-sm text-gray-600 mb-1">평균 매수가</label>
                                <input type="number" x-model="portfolio.editForm.avgBuyPrice" placeholder="0"
                                    class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500">
                            </div>
                            <div>
                                <label class="block text-sm text-gray-600 mb-1">배당률(%)</label>
                                <input type="number" step="0.01" x-model="portfolio.editForm.dividendYield" placeholder="0.00"
                                    class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500">
                            </div>
                        </div>
                    </div>
                </template>

                <!-- ===== 채권 수정 ===== -->
                <template x-if="portfolio.editingItem.assetType === 'BOND'">
                    <div class="space-y-3">
                        <div>
                            <label class="block text-sm text-gray-600 mb-1">유형</label>
                            <select x-model="portfolio.editForm.subType"
                                class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500">
                                <option value="GOVERNMENT">국채</option>
                                <option value="CORPORATE">회사채</option>
                            </select>
                        </div>
                        <div>
                            <label class="block text-sm text-gray-600 mb-1">만기일</label>
                            <input type="date" x-model="portfolio.editForm.maturityDate"
                                class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500">
                        </div>
                        <div class="grid grid-cols-2 gap-2">
                            <div>
                                <label class="block text-sm text-gray-600 mb-1">표면금리(%)</label>
                                <input type="number" step="0.01" x-model="portfolio.editForm.couponRate" placeholder="0.00"
                                    class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500">
                            </div>
                            <div>
                                <label class="block text-sm text-gray-600 mb-1">신용등급</label>
                                <input type="text" x-model="portfolio.editForm.creditRating" placeholder="AAA"
                                    class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500">
                            </div>
                        </div>
                    </div>
                </template>

                <!-- ===== 부동산 수정 ===== -->
                <template x-if="portfolio.editingItem.assetType === 'REAL_ESTATE'">
                    <div class="space-y-3">
                        <div>
                            <label class="block text-sm text-gray-600 mb-1">유형</label>
                            <select x-model="portfolio.editForm.subType"
                                class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500">
                                <option value="APARTMENT">아파트</option>
                                <option value="OFFICETEL">오피스텔</option>
                                <option value="LAND">토지</option>
                                <option value="COMMERCIAL">상가</option>
                            </select>
                        </div>
                        <div>
                            <label class="block text-sm text-gray-600 mb-1">주소</label>
                            <input type="text" x-model="portfolio.editForm.address" placeholder="주소 입력"
                                class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500">
                        </div>
                        <div>
                            <label class="block text-sm text-gray-600 mb-1">면적 (m²)</label>
                            <input type="number" step="0.01" x-model="portfolio.editForm.area" placeholder="0.00"
                                class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500">
                        </div>
                    </div>
                </template>

                <!-- ===== 펀드 수정 ===== -->
                <template x-if="portfolio.editingItem.assetType === 'FUND'">
                    <div class="space-y-3">
                        <div>
                            <label class="block text-sm text-gray-600 mb-1">유형</label>
                            <select x-model="portfolio.editForm.subType"
                                class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500">
                                <option value="EQUITY_FUND">주식형</option>
                                <option value="BOND_FUND">채권형</option>
                                <option value="MIXED_FUND">혼합형</option>
                            </select>
                        </div>
                        <div>
                            <label class="block text-sm text-gray-600 mb-1">운용보수(%)</label>
                            <input type="number" step="0.01" x-model="portfolio.editForm.managementFee" placeholder="0.00"
                                class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500">
                        </div>
                    </div>
                </template>

                <!-- ===== 공통 필드 ===== -->
                <div>
                    <label class="block text-sm text-gray-600 mb-1">항목명 *</label>
                    <input type="text" x-model="portfolio.editForm.itemName" placeholder="항목명"
                        class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500">
                </div>
                <div>
                    <label class="block text-sm text-gray-600 mb-1">투자 금액 (원) *</label>
                    <input type="number" x-model="portfolio.editForm.investedAmount" placeholder="0"
                        class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500">
                </div>
                <div>
                    <label class="block text-sm text-gray-600 mb-1">메모</label>
                    <input type="text" x-model="portfolio.editForm.memo" placeholder="메모 (선택)"
                        class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500">
                </div>

                <!-- 버튼 -->
                <div class="flex justify-end gap-2 mt-5">
                    <button @click="portfolio.showEditModal = false; portfolio.editingItem = null"
                        class="px-4 py-2 text-sm text-gray-600 hover:text-gray-800 transition">취소</button>
                    <button @click="submitEditItem()"
                        class="bg-blue-600 text-white px-4 py-2 rounded-lg text-sm font-medium hover:bg-blue-700 transition">수정</button>
                </div>
            </div>
        </template>

    </div>
</div>
```