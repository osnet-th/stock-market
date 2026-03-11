# 포트폴리오 재무정보 화면 예시 (v2 — 메뉴 선택 방식)

## app.js 상태 및 메서드

```javascript
// portfolio 객체 내부에 추가할 상태
financialOptions: null,
financialResult: null,            // 선택한 메뉴의 API 응답 결과
financialLoading: false,
selectedStockItem: null,
financialYear: '',
financialReportCode: 'ANNUAL',
selectedFinancialMenu: null,      // 현재 선택된 메뉴 key
financialIndexClass: 'PROFITABILITY',  // 재무지표 메뉴용
financialFsDiv: 'CFS',           // 전체재무제표 메뉴용 (CFS: 연결, OFS: 개별)
financialMenus: [
    { key: 'accounts', label: '재무계정' },
    { key: 'indices', label: '재무지표' },
    { key: 'full-statements', label: '전체재무제표' },
    { key: 'stock-quantities', label: '주식수량' },
    { key: 'dividends', label: '배당정보' },
    { key: 'lawsuits', label: '소송현황' },
    { key: 'private-fund', label: '사모자금사용' },
    { key: 'public-fund', label: '공모자금사용' }
],

getYearOptions() {
    var currentYear = new Date().getFullYear();
    return Array.from({ length: 6 }, function(_, i) { return String(currentYear - i); });
},

getDefaultYear() {
    return String(new Date().getFullYear() - 1);
},

async loadFinancialOptions() {
    if (this.portfolio.financialOptions) return;
    try {
        this.portfolio.financialOptions = await API.getFinancialOptions();
    } catch (e) {
        console.error('재무 옵션 로드 실패:', e);
    }
},

async openStockDetail(item) {
    var stockCode = item.stockDetail?.stockCode;
    if (!stockCode || item.stockDetail?.country !== 'KR') return;

    this.portfolio.selectedStockItem = item;
    this.portfolio.financialYear = this.getDefaultYear();
    this.portfolio.financialReportCode = 'ANNUAL';
    this.portfolio.selectedFinancialMenu = null;
    this.portfolio.financialResult = null;

    await this.loadFinancialOptions();
    // 메뉴 목록만 표시, API 호출 없음
},

closeStockDetail() {
    this.portfolio.selectedStockItem = null;
    this.portfolio.selectedFinancialMenu = null;
    this.portfolio.financialResult = null;
},

async selectFinancialMenu(menuKey) {
    this.portfolio.selectedFinancialMenu = menuKey;
    this.portfolio.financialResult = null;
    await this.loadSelectedFinancial();
},

async onFinancialFilterChange() {
    if (!this.portfolio.selectedFinancialMenu) return;
    await this.loadSelectedFinancial();
},

async loadSelectedFinancial() {
    var stockCode = this.portfolio.selectedStockItem?.stockDetail?.stockCode;
    var menu = this.portfolio.selectedFinancialMenu;
    if (!stockCode || !menu) return;

    var year = this.portfolio.financialYear;
    var reportCode = this.portfolio.financialReportCode;

    this.portfolio.financialLoading = true;
    try {
        var result;
        switch (menu) {
            case 'accounts':
                result = await API.getFinancialAccounts(stockCode, year, reportCode);
                break;
            case 'indices':
                result = await API.getFinancialIndices(stockCode, year, reportCode, this.portfolio.financialIndexClass);
                break;
            case 'full-statements':
                result = await API.getFullFinancialStatements(stockCode, year, reportCode, this.portfolio.financialFsDiv);
                break;
            case 'stock-quantities':
                result = await API.getFinancialStockQuantities(stockCode, year, reportCode);
                break;
            case 'dividends':
                result = await API.getFinancialDividends(stockCode, year, reportCode);
                break;
            case 'lawsuits':
                result = await API.getLawsuits(stockCode, year + '-01-01', year + '-12-31');
                break;
            case 'private-fund':
                result = await API.getPrivateFundUsages(stockCode, year, reportCode);
                break;
            case 'public-fund':
                result = await API.getPublicFundUsages(stockCode, year, reportCode);
                break;
        }
        this.portfolio.financialResult = result || [];
    } catch (e) {
        console.error('재무정보 조회 실패:', e);
        this.portfolio.financialResult = [];
    } finally {
        this.portfolio.financialLoading = false;
    }
},
```

## index.html 재무 상세 패널

```html
<template x-if="portfolio.selectedStockItem?.id === item.id">
    <div class="mt-3 p-4 bg-gray-50 rounded-lg border border-gray-200"
         x-transition:enter="transition ease-out duration-200"
         x-transition:enter-start="opacity-0 transform -translate-y-2"
         x-transition:enter-end="opacity-100 transform translate-y-0">

        <!-- 헤더 -->
        <div class="flex justify-between items-center mb-3">
            <h4 class="font-semibold text-sm">
                <span x-text="item.itemName"></span>
                (<span x-text="item.stockDetail?.stockCode"></span>) 재무 상세
            </h4>
            <button @click.stop="closeStockDetail()"
                    class="text-gray-400 hover:text-gray-600 text-lg">&times;</button>
        </div>

        <!-- 보유 정보 -->
        <div class="flex gap-4 text-sm text-gray-600 mb-3 pb-3 border-b border-gray-200">
            <span>수량: <span class="font-semibold" x-text="Format.number(item.stockDetail?.quantity)"></span>주</span>
            <span>평단가: <span class="font-semibold" x-text="'&#8361;' + Format.number(item.stockDetail?.avgBuyPrice)"></span></span>
            <span>투자금: <span class="font-semibold" x-text="'&#8361;' + Format.number(item.investedAmount)"></span></span>
        </div>

        <!-- 메뉴 버튼 목록 -->
        <div class="flex flex-wrap gap-2 mb-4">
            <template x-for="menu in portfolio.financialMenus" :key="menu.key">
                <button @click.stop="selectFinancialMenu(menu.key)"
                        class="text-xs px-3 py-1.5 rounded-full border transition"
                        :class="portfolio.selectedFinancialMenu === menu.key
                            ? 'bg-indigo-600 text-white border-indigo-600'
                            : 'bg-white text-gray-600 border-gray-300 hover:border-indigo-400 hover:text-indigo-600'"
                        x-text="menu.label">
                </button>
            </template>
        </div>

        <!-- 메뉴 선택 시: 파라미터 + 결과 -->
        <template x-if="portfolio.selectedFinancialMenu">
            <div>
                <!-- 파라미터 드롭다운 -->
                <div class="flex flex-wrap gap-3 mb-4">
                    <!-- 연도 (소송 제외 모든 메뉴) -->
                    <div class="flex items-center gap-2">
                        <label class="text-xs text-gray-500">연도</label>
                        <select x-model="portfolio.financialYear"
                                @change="onFinancialFilterChange()"
                                class="text-sm border border-gray-300 rounded px-2 py-1">
                            <template x-for="year in getYearOptions()" :key="year">
                                <option :value="year" x-text="year + '년'"></option>
                            </template>
                        </select>
                    </div>
                    <!-- 보고서 (소송 제외) -->
                    <div class="flex items-center gap-2"
                         x-show="portfolio.selectedFinancialMenu !== 'lawsuits' && portfolio.financialOptions">
                        <label class="text-xs text-gray-500">보고서</label>
                        <select x-model="portfolio.financialReportCode"
                                @change="onFinancialFilterChange()"
                                class="text-sm border border-gray-300 rounded px-2 py-1">
                            <template x-for="opt in portfolio.financialOptions?.reportCodes" :key="opt.code">
                                <option :value="opt.code" x-text="opt.label"></option>
                            </template>
                        </select>
                    </div>
                    <!-- 지표분류 (재무지표 메뉴만) -->
                    <div class="flex items-center gap-2"
                         x-show="portfolio.selectedFinancialMenu === 'indices' && portfolio.financialOptions">
                        <label class="text-xs text-gray-500">지표분류</label>
                        <select x-model="portfolio.financialIndexClass"
                                @change="onFinancialFilterChange()"
                                class="text-sm border border-gray-300 rounded px-2 py-1">
                            <template x-for="opt in portfolio.financialOptions?.indexClassCodes" :key="opt.code">
                                <option :value="opt.code" x-text="opt.label"></option>
                            </template>
                        </select>
                    </div>
                    <!-- 재무제표구분 (전체재무제표 메뉴만) -->
                    <div class="flex items-center gap-2"
                         x-show="portfolio.selectedFinancialMenu === 'full-statements'">
                        <label class="text-xs text-gray-500">구분</label>
                        <select x-model="portfolio.financialFsDiv"
                                @change="onFinancialFilterChange()"
                                class="text-sm border border-gray-300 rounded px-2 py-1">
                            <option value="CFS">연결</option>
                            <option value="OFS">개별</option>
                        </select>
                    </div>
                    <!-- 로딩 스피너 -->
                    <div x-show="portfolio.financialLoading"
                         class="flex items-center text-xs text-gray-400">
                        <svg class="animate-spin h-4 w-4 mr-1" viewBox="0 0 24 24">
                            <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" fill="none"></circle>
                            <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
                        </svg>
                        조회 중...
                    </div>
                </div>

                <!-- 결과 영역 -->
                <template x-if="portfolio.financialResult !== null && !portfolio.financialLoading">
                    <div>
                        <!-- 빈 결과 -->
                        <template x-if="portfolio.financialResult.length === 0">
                            <div class="text-center py-6 text-gray-400 text-sm">데이터 없음</div>
                        </template>

                        <!-- 결과 테이블 (공통: 배열을 테이블로 렌더링) -->
                        <template x-if="portfolio.financialResult.length > 0">
                            <div class="overflow-x-auto">
                                <table class="w-full text-sm">
                                    <thead>
                                        <tr class="border-b border-gray-200">
                                            <template x-for="key in Object.keys(portfolio.financialResult[0])" :key="key">
                                                <th class="text-left py-2 px-2 text-xs text-gray-500 font-medium" x-text="key"></th>
                                            </template>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        <template x-for="(row, idx) in portfolio.financialResult" :key="idx">
                                            <tr class="border-b border-gray-50">
                                                <template x-for="key in Object.keys(row)" :key="key">
                                                    <td class="py-2 px-2 text-gray-700" x-text="row[key]"></td>
                                                </template>
                                            </tr>
                                        </template>
                                    </tbody>
                                </table>
                            </div>
                        </template>
                    </div>
                </template>
            </div>
        </template>

        <!-- 메뉴 미선택 시 안내 -->
        <template x-if="!portfolio.selectedFinancialMenu">
            <div class="text-center py-6 text-gray-400 text-sm">
                위 메뉴를 선택하여 재무정보를 조회하세요
            </div>
        </template>
    </div>
</template>
```

## format.js 추가 유틸

```javascript
compactNumber(value) {
    if (!value) return '-';
    var num = typeof value === 'string' ? parseInt(value.replace(/,/g, '')) : value;
    if (isNaN(num)) return value;

    var absNum = Math.abs(num);
    var sign = num < 0 ? '-' : '';

    if (absNum >= 1_0000_0000_0000) {
        return sign + (absNum / 1_0000_0000_0000).toFixed(1) + '조';
    }
    if (absNum >= 1_0000_0000) {
        return sign + (absNum / 1_0000_0000).toFixed(0) + '억';
    }
    if (absNum >= 1_0000) {
        return sign + (absNum / 1_0000).toFixed(0) + '만';
    }
    return sign + Format.number(absNum);
},
```