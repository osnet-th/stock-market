# 포트폴리오 메인 화면 예시 코드

## app.js — portfolio 상태 및 메서드

```javascript
// ==================== Portfolio State ====================
portfolio: {
    items: [],
    allocation: [],
    loading: false,
    showAddModal: false,
    showEditModal: false,
    editingItem: null,
    selectedAssetType: null,
    addForm: {},
    editForm: {},
    stockSearch: {
        query: '',
        results: [],
        loading: false,
        selected: null,
        debounceTimer: null
    },
    expandedSections: {}
},

// menus 배열에 추가
// { key: 'portfolio', label: '포트폴리오', icon: 'portfolio' }

// navigateTo switch에 추가
// case 'portfolio':
//     await this.loadPortfolio();
//     break;

// ==================== Portfolio Methods ====================

// 자산 타입 설정 (한글명, 색상)
assetTypeConfig: {
    STOCK:       { label: '주식',     color: 'blue',   barColor: 'bg-blue-500' },
    BOND:        { label: '채권',     color: 'green',  barColor: 'bg-green-500' },
    REAL_ESTATE: { label: '부동산',   color: 'yellow', barColor: 'bg-yellow-500' },
    FUND:        { label: '펀드',     color: 'purple', barColor: 'bg-purple-500' },
    CRYPTO:      { label: '암호화폐', color: 'orange', barColor: 'bg-orange-500' },
    GOLD:        { label: '금',       color: 'amber',  barColor: 'bg-amber-500' },
    COMMODITY:   { label: '원자재',   color: 'red',    barColor: 'bg-red-500' },
    CASH:        { label: '현금',     color: 'gray',   barColor: 'bg-gray-500' },
    OTHER:       { label: '기타',     color: 'slate',  barColor: 'bg-slate-500' }
},

getAssetTypeLabel(type) {
    return this.assetTypeConfig[type]?.label || type;
},

getAssetTypeBadgeClass(type) {
    var color = this.assetTypeConfig[type]?.color || 'gray';
    return 'bg-' + color + '-100 text-' + color + '-700';
},

getAssetTypeBarClass(type) {
    return this.assetTypeConfig[type]?.barColor || 'bg-gray-500';
},

async loadPortfolio() {
    this.portfolio.loading = true;
    try {
        var results = await Promise.all([
            API.getPortfolioItems(this.auth.userId),
            API.getPortfolioAllocation(this.auth.userId)
        ]);
        this.portfolio.items = results[0] || [];
        this.portfolio.allocation = results[1] || [];

        // 항목이 있는 섹션은 기본 펼침
        var sections = {};
        this.portfolio.items.forEach(function(item) {
            if (sections[item.assetType] === undefined) {
                sections[item.assetType] = true;
            }
        });
        this.portfolio.expandedSections = sections;
    } catch (e) {
        console.error('포트폴리오 로드 실패:', e);
        this.portfolio.items = [];
        this.portfolio.allocation = [];
    } finally {
        this.portfolio.loading = false;
    }
},

getItemsByType(type) {
    return this.portfolio.items.filter(function(item) { return item.assetType === type; });
},

getTotalInvested() {
    return this.portfolio.items.reduce(function(sum, item) { return sum + item.investedAmount; }, 0);
},

getNewsEnabledCount() {
    return this.portfolio.items.filter(function(item) { return item.newsEnabled; }).length;
},

toggleSection(assetType) {
    this.portfolio.expandedSections[assetType] = !this.portfolio.expandedSections[assetType];
},

// 항목 카드에 표시할 핵심 정보
getItemSummary(item) {
    switch (item.assetType) {
        case 'STOCK':
            if (!item.stockDetail) return '';
            var parts = [item.stockDetail.exchange + ':' + item.stockDetail.ticker];
            if (item.stockDetail.subType === 'ETF') parts.push('ETF');
            if (item.stockDetail.quantity) parts.push(item.stockDetail.quantity + '주');
            return parts.join(' · ');
        case 'BOND':
            if (!item.bondDetail) return '';
            var bondParts = [];
            if (item.bondDetail.subType === 'GOVERNMENT') bondParts.push('국채');
            else if (item.bondDetail.subType === 'CORPORATE') bondParts.push('회사채');
            if (item.bondDetail.maturityDate) bondParts.push('만기 ' + item.bondDetail.maturityDate);
            if (item.bondDetail.couponRate) bondParts.push(item.bondDetail.couponRate + '%');
            return bondParts.join(' · ');
        case 'REAL_ESTATE':
            if (!item.realEstateDetail) return '';
            var reParts = [];
            var reSubTypes = { APARTMENT: '아파트', OFFICETEL: '오피스텔', LAND: '토지', COMMERCIAL: '상가' };
            if (item.realEstateDetail.subType) reParts.push(reSubTypes[item.realEstateDetail.subType] || item.realEstateDetail.subType);
            if (item.realEstateDetail.address) reParts.push(item.realEstateDetail.address);
            return reParts.join(' · ');
        case 'FUND':
            if (!item.fundDetail) return '';
            var fundParts = [];
            var fundSubTypes = { EQUITY_FUND: '주식형', BOND_FUND: '채권형', MIXED_FUND: '혼합형' };
            if (item.fundDetail.subType) fundParts.push(fundSubTypes[item.fundDetail.subType] || item.fundDetail.subType);
            if (item.fundDetail.managementFee) fundParts.push('보수 ' + item.fundDetail.managementFee + '%');
            return fundParts.join(' · ');
        default:
            return item.memo || '';
    }
},

async deleteItem(itemId) {
    if (!confirm('자산 항목을 삭제하시겠습니까?\n관련 뉴스도 함께 삭제됩니다.')) return;
    try {
        await API.deletePortfolioItem(this.auth.userId, itemId);
        await this.loadPortfolio();
    } catch (e) {
        console.error('항목 삭제 실패:', e);
        alert('삭제에 실패했습니다.');
    }
},

async toggleNews(item) {
    try {
        await API.togglePortfolioNews(this.auth.userId, item.id, !item.newsEnabled);
        await this.loadPortfolio();
    } catch (e) {
        console.error('뉴스 토글 실패:', e);
        alert('뉴스 설정 변경에 실패했습니다.');
    }
}
```

## index.html — 포트폴리오 메인 화면

```html
<!-- ========== PORTFOLIO ========== -->
<div x-show="currentPage === 'portfolio'" x-cloak>
    <div class="flex items-center justify-between mb-6">
        <h2 class="text-xl font-bold text-gray-800">포트폴리오</h2>
        <button @click="openAddModal()"
            class="bg-blue-600 text-white px-4 py-2 rounded-lg text-sm font-medium hover:bg-blue-700 transition">
            + 자산 추가
        </button>
    </div>

    <!-- 로딩 -->
    <template x-if="portfolio.loading">
        <div class="flex justify-center py-12">
            <div class="spinner"></div>
        </div>
    </template>

    <template x-if="!portfolio.loading">
        <div>
            <!-- 빈 상태 -->
            <template x-if="portfolio.items.length === 0">
                <div class="bg-white rounded-xl border border-gray-200 p-12 text-center">
                    <p class="text-gray-400 text-sm mb-4">등록된 자산이 없습니다.</p>
                    <button @click="openAddModal()"
                        class="bg-blue-600 text-white px-4 py-2 rounded-lg text-sm font-medium hover:bg-blue-700 transition">
                        첫 번째 자산 추가하기
                    </button>
                </div>
            </template>

            <template x-if="portfolio.items.length > 0">
                <div>
                    <!-- 요약 카드 -->
                    <div class="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
                        <div class="bg-white rounded-xl border border-gray-200 p-5">
                            <p class="text-sm text-gray-500 mb-1">총 투자 금액</p>
                            <p class="text-2xl font-bold text-gray-800">
                                <span x-text="Format.number(getTotalInvested(), 0)"></span>
                                <span class="text-sm font-normal text-gray-500">원</span>
                            </p>
                        </div>
                        <div class="bg-white rounded-xl border border-gray-200 p-5">
                            <p class="text-sm text-gray-500 mb-1">자산 항목</p>
                            <p class="text-2xl font-bold text-gray-800">
                                <span x-text="portfolio.items.length"></span>
                                <span class="text-sm font-normal text-gray-500">개</span>
                            </p>
                        </div>
                        <div class="bg-white rounded-xl border border-gray-200 p-5">
                            <p class="text-sm text-gray-500 mb-1">뉴스 수집 활성</p>
                            <p class="text-2xl font-bold text-gray-800">
                                <span x-text="getNewsEnabledCount()"></span>
                                <span class="text-sm font-normal text-gray-500">개</span>
                            </p>
                        </div>
                    </div>

                    <!-- 자산 비중 바 차트 -->
                    <div class="bg-white rounded-xl border border-gray-200 p-5 mb-6">
                        <h3 class="text-sm font-bold text-gray-700 mb-4">자산 비중</h3>
                        <div class="space-y-3">
                            <template x-for="alloc in portfolio.allocation" :key="alloc.assetType">
                                <div class="flex items-center gap-3">
                                    <span class="text-xs text-gray-600 w-16 text-right" x-text="alloc.assetTypeName"></span>
                                    <div class="flex-1 bg-gray-100 rounded-full h-5 overflow-hidden">
                                        <div :class="getAssetTypeBarClass(alloc.assetType)"
                                            class="h-full rounded-full transition-all duration-500"
                                            :style="'width: ' + alloc.percentage + '%'">
                                        </div>
                                    </div>
                                    <span class="text-xs font-mono text-gray-700 w-12 text-right"
                                        x-text="alloc.percentage + '%'"></span>
                                    <span class="text-xs text-gray-400 w-28 text-right"
                                        x-text="Format.number(alloc.totalAmount, 0) + '원'"></span>
                                </div>
                            </template>
                        </div>
                    </div>

                    <!-- 카테고리별 접이식 섹션 -->
                    <div class="space-y-3">
                        <template x-for="alloc in portfolio.allocation" :key="'section-' + alloc.assetType">
                            <div class="bg-white rounded-xl border border-gray-200 overflow-hidden">
                                <!-- 섹션 헤더 -->
                                <button @click="toggleSection(alloc.assetType)"
                                    class="w-full flex items-center justify-between px-5 py-3 hover:bg-gray-50 transition">
                                    <div class="flex items-center gap-3">
                                        <span x-text="portfolio.expandedSections[alloc.assetType] ? '▼' : '▶'"
                                            class="text-xs text-gray-400"></span>
                                        <span :class="getAssetTypeBadgeClass(alloc.assetType)"
                                            class="text-xs px-2 py-0.5 rounded-full font-medium"
                                            x-text="alloc.assetTypeName"></span>
                                        <span class="text-sm text-gray-500"
                                            x-text="getItemsByType(alloc.assetType).length + '건'"></span>
                                    </div>
                                    <span class="text-sm font-mono text-gray-600"
                                        x-text="Format.number(alloc.totalAmount, 0) + '원 (' + alloc.percentage + '%)'"></span>
                                </button>

                                <!-- 섹션 내용 -->
                                <div x-show="portfolio.expandedSections[alloc.assetType]" class="border-t border-gray-100">
                                    <template x-for="item in getItemsByType(alloc.assetType)" :key="item.id">
                                        <div class="px-5 py-3 border-b border-gray-50 last:border-b-0 flex items-center justify-between hover:bg-gray-50 transition">
                                            <div>
                                                <div class="flex items-center gap-2">
                                                    <span class="text-sm font-medium text-gray-800" x-text="item.itemName"></span>
                                                    <span class="text-xs text-gray-400" x-text="Format.number(item.investedAmount, 0) + '원'"></span>
                                                </div>
                                                <p class="text-xs text-gray-400 mt-0.5" x-text="getItemSummary(item)"></p>
                                            </div>
                                            <div class="flex items-center gap-2">
                                                <!-- 뉴스 토글 -->
                                                <button @click.stop="toggleNews(item)"
                                                    :class="item.newsEnabled ? 'text-green-600 hover:text-green-800' : 'text-gray-400 hover:text-gray-600'"
                                                    class="text-xs font-medium transition"
                                                    x-text="item.newsEnabled ? '뉴스 ON' : '뉴스 OFF'">
                                                </button>
                                                <!-- 수정 -->
                                                <button @click.stop="openEditModal(item)"
                                                    class="text-xs text-blue-600 hover:text-blue-800 font-medium transition">수정</button>
                                                <!-- 삭제 -->
                                                <button @click.stop="deleteItem(item.id)"
                                                    class="text-xs text-red-500 hover:text-red-700 font-medium transition">삭제</button>
                                            </div>
                                        </div>
                                    </template>
                                </div>
                            </div>
                        </template>
                    </div>
                </div>
            </template>
        </div>
    </template>
</div>
```

## index.html — 대시보드 홈 요약 카드 추가

```html
<!-- 기존 grid-cols-3 → grid-cols-4 변경 -->
<div class="grid grid-cols-1 md:grid-cols-4 gap-4 mb-8">
    <!-- 기존 3개 카드 유지 -->

    <!-- 포트폴리오 요약 카드 추가 -->
    <div @click="navigateTo('portfolio')" class="summary-card bg-white rounded-xl border border-gray-200 p-5 cursor-pointer">
        <p class="text-sm text-gray-500 mb-1">포트폴리오</p>
        <p class="text-2xl font-bold text-gray-800" x-text="homeSummary.portfolioItemCount || 0"></p>
        <p class="text-xs text-gray-400 mt-1">
            자산 항목 · 총 <span x-text="Format.number(homeSummary.portfolioTotalAmount || 0, 0)" class="font-medium text-blue-600"></span>원
        </p>
    </div>
</div>
```

## app.js — loadHomeSummary에 포트폴리오 요약 추가

```javascript
// loadHomeSummary 내부에 추가
try {
    var portfolioItems = await API.getPortfolioItems(this.auth.userId) || [];
    this.homeSummary.portfolioItemCount = portfolioItems.length;
    this.homeSummary.portfolioTotalAmount = portfolioItems.reduce(function(sum, item) { return sum + item.investedAmount; }, 0);
} catch (e) { /* skip */ }
```