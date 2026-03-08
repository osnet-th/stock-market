# 자산 추가 모달 예시 코드

## app.js — 추가 모달 메서드

```javascript
// ==================== 추가 모달 ====================

openAddModal() {
    this.portfolio.selectedAssetType = null;
    this.portfolio.addForm = {};
    this.portfolio.stockSearch = { query: '', results: [], loading: false, selected: null, debounceTimer: null };
    this.portfolio.showAddModal = true;
},

selectAssetType(type) {
    this.portfolio.selectedAssetType = type;
    this.portfolio.addForm = { region: 'DOMESTIC' };
    this.portfolio.stockSearch = { query: '', results: [], loading: false, selected: null, debounceTimer: null };

    // 일반 자산은 assetType 기본값 설정
    if (type === 'GENERAL') {
        this.portfolio.addForm.assetType = 'CRYPTO';
    }
},

// ==================== 주식 검색 ====================

onStockSearchInput() {
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
        self.searchStock();
    }, 300);
},

async searchStock() {
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

selectStock(stock) {
    this.portfolio.stockSearch.selected = stock;
    this.portfolio.stockSearch.results = [];
    this.portfolio.stockSearch.query = '';
    // 폼에 자동 채움
    this.portfolio.addForm.itemName = stock.stockName;
    this.portfolio.addForm.ticker = stock.stockCode;
    this.portfolio.addForm.exchange = stock.marketType;
},

clearSelectedStock() {
    this.portfolio.stockSearch.selected = null;
    this.portfolio.addForm.itemName = '';
    this.portfolio.addForm.ticker = '';
    this.portfolio.addForm.exchange = '';
},

// ==================== 등록 제출 ====================

async submitAddItem() {
    var type = this.portfolio.selectedAssetType;
    var form = this.portfolio.addForm;
    var userId = this.auth.userId;

    // 공통 검증
    if (!form.itemName || !form.investedAmount) {
        alert('항목명과 투자 금액은 필수입니다.');
        return;
    }
    if (Number(form.investedAmount) <= 0) {
        alert('투자 금액은 0보다 커야 합니다.');
        return;
    }

    try {
        switch (type) {
            case 'STOCK':
                await API.addStockItem(userId, {
                    itemName: form.itemName,
                    investedAmount: Number(form.investedAmount),
                    region: form.region,
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
                await API.addBondItem(userId, {
                    itemName: form.itemName,
                    investedAmount: Number(form.investedAmount),
                    region: form.region,
                    memo: form.memo || null,
                    subType: form.subType || 'GOVERNMENT',
                    maturityDate: form.maturityDate || null,
                    couponRate: form.couponRate ? Number(form.couponRate) : null,
                    creditRating: form.creditRating || null
                });
                break;
            case 'REAL_ESTATE':
                await API.addRealEstateItem(userId, {
                    itemName: form.itemName,
                    investedAmount: Number(form.investedAmount),
                    region: form.region,
                    memo: form.memo || null,
                    subType: form.subType || 'APARTMENT',
                    address: form.address || null,
                    area: form.area ? Number(form.area) : null
                });
                break;
            case 'FUND':
                await API.addFundItem(userId, {
                    itemName: form.itemName,
                    investedAmount: Number(form.investedAmount),
                    region: form.region,
                    memo: form.memo || null,
                    subType: form.subType || 'EQUITY_FUND',
                    managementFee: form.managementFee ? Number(form.managementFee) : null
                });
                break;
            case 'GENERAL':
                await API.addGeneralItem(userId, {
                    assetType: form.assetType,
                    itemName: form.itemName,
                    investedAmount: Number(form.investedAmount),
                    region: form.region,
                    memo: form.memo || null
                });
                break;
        }
        this.portfolio.showAddModal = false;
        await this.loadPortfolio();
    } catch (e) {
        console.error('자산 등록 실패:', e);
        alert('자산 등록에 실패했습니다.');
    }
}
```

## index.html — 추가 모달

```html
<!-- 자산 추가 모달 -->
<div x-show="portfolio.showAddModal" x-cloak
    class="modal-backdrop fixed inset-0 bg-black/30 flex items-center justify-center z-50">
    <div class="bg-white rounded-xl p-6 w-[480px] max-h-[80vh] overflow-y-auto shadow-xl"
        @click.outside="portfolio.showAddModal = false">

        <h3 class="text-lg font-bold text-gray-800 mb-4">자산 추가</h3>

        <!-- 자산 타입 선택 -->
        <template x-if="!portfolio.selectedAssetType">
            <div>
                <p class="text-sm text-gray-500 mb-3">자산 유형을 선택하세요</p>
                <div class="grid grid-cols-2 gap-2">
                    <template x-for="t in [
                        {key:'STOCK', label:'주식', desc:'개별주식, ETF'},
                        {key:'BOND', label:'채권', desc:'국채, 회사채'},
                        {key:'REAL_ESTATE', label:'부동산', desc:'아파트, 오피스텔, 토지'},
                        {key:'FUND', label:'펀드', desc:'주식형, 채권형, 혼합형'},
                        {key:'GENERAL', label:'일반 자산', desc:'암호화폐, 금, 원자재, 현금, 기타'}
                    ]">
                        <button @click="selectAssetType(t.key)"
                            class="border border-gray-200 rounded-lg p-3 text-left hover:border-blue-400 hover:bg-blue-50 transition">
                            <span class="text-sm font-medium text-gray-800" x-text="t.label"></span>
                            <p class="text-xs text-gray-400 mt-0.5" x-text="t.desc"></p>
                        </button>
                    </template>
                </div>
            </div>
        </template>

        <!-- 타입별 폼 -->
        <template x-if="portfolio.selectedAssetType">
            <div>
                <!-- 뒤로가기 -->
                <button @click="portfolio.selectedAssetType = null"
                    class="text-xs text-gray-500 hover:text-gray-700 mb-3 transition">
                    ← 자산 유형 다시 선택
                </button>

                <div class="space-y-3">

                    <!-- ===== 주식: 종목 검색 ===== -->
                    <template x-if="portfolio.selectedAssetType === 'STOCK'">
                        <div>
                            <!-- 종목 검색 -->
                            <div class="mb-3">
                                <label class="block text-sm text-gray-600 mb-1">종목 검색</label>
                                <div class="relative">
                                    <input x-model="portfolio.stockSearch.query"
                                        @input="onStockSearchInput()"
                                        placeholder="종목명을 입력하세요 (예: 삼성전자)"
                                        class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent">
                                    <template x-if="portfolio.stockSearch.loading">
                                        <span class="absolute right-3 top-2.5 text-xs text-gray-400">검색 중...</span>
                                    </template>
                                </div>
                                <!-- 검색 결과 드롭다운 -->
                                <template x-if="portfolio.stockSearch.results.length > 0">
                                    <div class="mt-1 bg-white border border-gray-200 rounded-lg shadow-lg max-h-40 overflow-y-auto">
                                        <template x-for="stock in portfolio.stockSearch.results" :key="stock.stockCode">
                                            <button @click="selectStock(stock)"
                                                class="w-full text-left px-3 py-2 text-sm hover:bg-blue-50 transition border-b border-gray-50 last:border-b-0">
                                                <span class="font-medium text-gray-800" x-text="stock.stockName"></span>
                                                <span class="text-gray-400 ml-1" x-text="'(' + stock.stockCode + ')'"></span>
                                                <span class="text-xs text-gray-400 ml-1" x-text="stock.marketType"></span>
                                            </button>
                                        </template>
                                    </div>
                                </template>
                            </div>

                            <!-- 선택된 종목 표시 -->
                            <template x-if="portfolio.stockSearch.selected">
                                <div class="bg-blue-50 border border-blue-200 rounded-lg px-3 py-2 mb-3 flex items-center justify-between">
                                    <div class="text-sm">
                                        <span class="font-medium text-blue-800" x-text="portfolio.stockSearch.selected.stockName"></span>
                                        <span class="text-blue-600 ml-1" x-text="'(' + portfolio.stockSearch.selected.stockCode + ')'"></span>
                                        <span class="text-xs text-blue-500 ml-1" x-text="portfolio.stockSearch.selected.marketType"></span>
                                    </div>
                                    <button @click="clearSelectedStock()" class="text-xs text-blue-500 hover:text-blue-700">변경</button>
                                </div>
                            </template>

                            <!-- 서브타입 -->
                            <div>
                                <label class="block text-sm text-gray-600 mb-1">유형</label>
                                <select x-model="portfolio.addForm.subType"
                                    class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500">
                                    <option value="INDIVIDUAL">개별주식</option>
                                    <option value="ETF">ETF</option>
                                </select>
                            </div>

                            <!-- 수량, 평균매수가, 배당률 -->
                            <div class="grid grid-cols-3 gap-2">
                                <div>
                                    <label class="block text-sm text-gray-600 mb-1">보유 수량</label>
                                    <input type="number" x-model="portfolio.addForm.quantity" placeholder="0"
                                        class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500">
                                </div>
                                <div>
                                    <label class="block text-sm text-gray-600 mb-1">평균 매수가</label>
                                    <input type="number" x-model="portfolio.addForm.avgBuyPrice" placeholder="0"
                                        class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500">
                                </div>
                                <div>
                                    <label class="block text-sm text-gray-600 mb-1">배당률(%)</label>
                                    <input type="number" step="0.01" x-model="portfolio.addForm.dividendYield" placeholder="0.00"
                                        class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500">
                                </div>
                            </div>
                        </div>
                    </template>

                    <!-- ===== 채권 전용 필드 ===== -->
                    <template x-if="portfolio.selectedAssetType === 'BOND'">
                        <div class="space-y-3">
                            <div>
                                <label class="block text-sm text-gray-600 mb-1">유형</label>
                                <select x-model="portfolio.addForm.subType"
                                    class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500">
                                    <option value="GOVERNMENT">국채</option>
                                    <option value="CORPORATE">회사채</option>
                                </select>
                            </div>
                            <div>
                                <label class="block text-sm text-gray-600 mb-1">만기일</label>
                                <input type="date" x-model="portfolio.addForm.maturityDate"
                                    class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500">
                            </div>
                            <div class="grid grid-cols-2 gap-2">
                                <div>
                                    <label class="block text-sm text-gray-600 mb-1">표면금리(%)</label>
                                    <input type="number" step="0.01" x-model="portfolio.addForm.couponRate" placeholder="0.00"
                                        class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500">
                                </div>
                                <div>
                                    <label class="block text-sm text-gray-600 mb-1">신용등급</label>
                                    <input type="text" x-model="portfolio.addForm.creditRating" placeholder="AAA"
                                        class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500">
                                </div>
                            </div>
                        </div>
                    </template>

                    <!-- ===== 부동산 전용 필드 ===== -->
                    <template x-if="portfolio.selectedAssetType === 'REAL_ESTATE'">
                        <div class="space-y-3">
                            <div>
                                <label class="block text-sm text-gray-600 mb-1">유형</label>
                                <select x-model="portfolio.addForm.subType"
                                    class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500">
                                    <option value="APARTMENT">아파트</option>
                                    <option value="OFFICETEL">오피스텔</option>
                                    <option value="LAND">토지</option>
                                    <option value="COMMERCIAL">상가</option>
                                </select>
                            </div>
                            <div>
                                <label class="block text-sm text-gray-600 mb-1">주소</label>
                                <input type="text" x-model="portfolio.addForm.address" placeholder="주소 입력"
                                    class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500">
                            </div>
                            <div>
                                <label class="block text-sm text-gray-600 mb-1">면적 (m²)</label>
                                <input type="number" step="0.01" x-model="portfolio.addForm.area" placeholder="0.00"
                                    class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500">
                            </div>
                        </div>
                    </template>

                    <!-- ===== 펀드 전용 필드 ===== -->
                    <template x-if="portfolio.selectedAssetType === 'FUND'">
                        <div class="space-y-3">
                            <div>
                                <label class="block text-sm text-gray-600 mb-1">유형</label>
                                <select x-model="portfolio.addForm.subType"
                                    class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500">
                                    <option value="EQUITY_FUND">주식형</option>
                                    <option value="BOND_FUND">채권형</option>
                                    <option value="MIXED_FUND">혼합형</option>
                                </select>
                            </div>
                            <div>
                                <label class="block text-sm text-gray-600 mb-1">운용보수(%)</label>
                                <input type="number" step="0.01" x-model="portfolio.addForm.managementFee" placeholder="0.00"
                                    class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500">
                            </div>
                        </div>
                    </template>

                    <!-- ===== 일반 자산 전용 필드 ===== -->
                    <template x-if="portfolio.selectedAssetType === 'GENERAL'">
                        <div>
                            <label class="block text-sm text-gray-600 mb-1">자산 유형</label>
                            <select x-model="portfolio.addForm.assetType"
                                class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500">
                                <option value="CRYPTO">암호화폐</option>
                                <option value="GOLD">금</option>
                                <option value="COMMODITY">원자재</option>
                                <option value="CASH">현금성 자산</option>
                                <option value="OTHER">기타</option>
                            </select>
                        </div>
                    </template>

                    <!-- ===== 공통 필드 (모든 타입) ===== -->
                    <!-- 주식은 itemName 자동 입력이므로 주식이 아닌 경우만 수동 입력 -->
                    <template x-if="portfolio.selectedAssetType !== 'STOCK'">
                        <div>
                            <label class="block text-sm text-gray-600 mb-1">항목명 *</label>
                            <input type="text" x-model="portfolio.addForm.itemName" placeholder="항목명을 입력하세요"
                                class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500">
                        </div>
                    </template>

                    <div>
                        <label class="block text-sm text-gray-600 mb-1">투자 금액 (원) *</label>
                        <input type="number" x-model="portfolio.addForm.investedAmount" placeholder="0"
                            class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500">
                    </div>

                    <div>
                        <label class="block text-sm text-gray-600 mb-1">리전</label>
                        <select x-model="portfolio.addForm.region"
                            class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500">
                            <option value="DOMESTIC">국내</option>
                            <option value="INTERNATIONAL">해외</option>
                        </select>
                    </div>

                    <div>
                        <label class="block text-sm text-gray-600 mb-1">메모</label>
                        <input type="text" x-model="portfolio.addForm.memo" placeholder="메모 (선택)"
                            class="w-full border border-gray-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500">
                    </div>
                </div>

                <!-- 버튼 -->
                <div class="flex justify-end gap-2 mt-5">
                    <button @click="portfolio.showAddModal = false"
                        class="px-4 py-2 text-sm text-gray-600 hover:text-gray-800 transition">취소</button>
                    <button @click="submitAddItem()"
                        class="bg-blue-600 text-white px-4 py-2 rounded-lg text-sm font-medium hover:bg-blue-700 transition">등록</button>
                </div>
            </div>
        </template>

    </div>
</div>
```