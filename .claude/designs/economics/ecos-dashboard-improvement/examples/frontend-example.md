# 프론트엔드 예시

## app.js state 변경

```javascript
ecos: {
    categories: [],
    selectedCategory: null,
    indicators: [],
    loading: false,
    _requestGeneration: 0
}
```

## ecos.js 컴포넌트 변경

```javascript
const EcosComponent = {
    async loadEcosCategories() {
        // 기존과 동일
    },

    async loadEcosIndicators() {
        if (!this.ecos.selectedCategory) return;

        const thisGeneration = ++this.ecos._requestGeneration;
        this.ecos.loading = true;

        try {
            const result = await API.getEcosIndicators(this.ecos.selectedCategory) || [];
            if (thisGeneration !== this.ecos._requestGeneration) return;
            this.ecos.indicators = result;
        } catch (e) {
            if (thisGeneration !== this.ecos._requestGeneration) return;
            console.error('ECOS 지표 로드 실패:', e);
            this.ecos.indicators = [];
        } finally {
            if (thisGeneration === this.ecos._requestGeneration) {
                this.ecos.loading = false;
            }
        }
    },

    async selectEcosCategory(categoryName) {
        this.ecos.selectedCategory = categoryName;
        await this.loadEcosIndicators();
    },

    getKeyIndicators() {
        return this.ecos.indicators.filter(ind => ind.keyIndicator);
    },

    getSortedIndicators() {
        return [...this.ecos.indicators].sort((a, b) =>
            a.className.localeCompare(b.className)
        );
    },

    getCardBorderClass(ind) {
        const change = Format.change(ind.dataValue, ind.previousDataValue);
        if (change.direction === 'none' || change.direction === 'same') {
            return 'border-gray-300';
        }
        const positive = ind.positiveDirection || 'NEUTRAL';
        if (positive === 'NEUTRAL') return 'border-gray-300';

        const isPositiveChange =
            (positive === 'UP' && change.direction === 'up') ||
            (positive === 'DOWN' && change.direction === 'down');

        return isPositiveChange ? 'border-green-500' : 'border-red-500';
    },

    getEcosCategoryLabel(name) {
        const cat = this.ecos.categories.find(c => c.name === name);
        return cat ? cat.label : name;
    }
};
```

## index.html 카드 대시보드 + 테이블

```html
<div x-show="currentPage === 'ecos'" x-cloak>
    <h2 class="text-xl font-bold text-gray-800 mb-6">국내 경제지표</h2>

    <!-- 카테고리 탭 -->
    <div class="flex gap-1 mb-6 overflow-x-auto pb-2 border-b border-gray-200">
        <template x-for="cat in ecos.categories" :key="cat.name">
            <button @click="selectEcosCategory(cat.name)"
                :class="ecos.selectedCategory === cat.name
                    ? 'text-blue-700 font-bold tab-active'
                    : 'text-gray-500 hover:text-gray-700'"
                class="px-4 py-2 text-sm whitespace-nowrap transition"
                x-text="cat.label">
            </button>
        </template>
    </div>

    <!-- 로딩 스켈레톤 -->
    <template x-if="ecos.loading && ecos.indicators.length === 0">
        <div class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-4 gap-4 mb-6">
            <template x-for="i in 4" :key="'skeleton-'+i">
                <div class="bg-white rounded-lg border-l-4 border-gray-200 p-4 shadow-sm animate-pulse">
                    <div class="h-3 bg-gray-200 rounded w-1/2 mb-3"></div>
                    <div class="h-7 bg-gray-200 rounded w-3/4 mb-2"></div>
                    <div class="h-3 bg-gray-200 rounded w-1/3"></div>
                </div>
            </template>
        </div>
    </template>

    <!-- 카드 대시보드 -->
    <template x-if="getKeyIndicators().length > 0">
        <ul class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-4 gap-4 mb-6">
            <template x-for="ind in getKeyIndicators()" :key="ind.keystatName">
                <li>
                    <article class="bg-white rounded-lg border-l-4 p-4 shadow-sm
                                    hover:shadow-md transition-shadow duration-200"
                             :class="getCardBorderClass(ind)">
                        <!-- 지표명 + 툴팁 -->
                        <div class="flex items-center justify-between">
                            <h3 class="text-xs font-medium text-gray-500 uppercase tracking-wide"
                                x-text="ind.keystatName"></h3>
                            <template x-if="ind.description">
                                <div class="group relative">
                                    <span class="text-gray-300 hover:text-gray-500 cursor-help text-xs">&#9432;</span>
                                    <div class="invisible opacity-0 group-hover:visible group-hover:opacity-100
                                                absolute z-10 bottom-full right-0 mb-2
                                                px-3 py-2 text-xs text-white bg-gray-800 rounded-lg shadow-lg
                                                w-48 transition-opacity duration-200"
                                         role="tooltip">
                                        <span x-text="ind.description"></span>
                                    </div>
                                </div>
                            </template>
                        </div>

                        <!-- 현재 값 -->
                        <p class="mt-1 text-2xl font-bold text-gray-900">
                            <span x-text="Format.number(ind.dataValue)"></span>
                            <span class="text-xs font-normal text-gray-400 ml-1"
                                  x-text="ind.unitName" x-show="ind.unitName"></span>
                        </p>

                        <!-- 변동 정보 -->
                        <div class="mt-2 flex items-center gap-1.5">
                            <template x-if="ind.previousDataValue">
                                <span class="text-sm font-medium"
                                      :class="Format.change(ind.dataValue, ind.previousDataValue).className">
                                    <span x-text="Format.change(ind.dataValue, ind.previousDataValue).symbol"></span>
                                    <span x-text="Format.changeRate(ind.dataValue, ind.previousDataValue)"></span>
                                </span>
                            </template>
                            <template x-if="!ind.previousDataValue">
                                <span class="text-sm text-gray-400">-</span>
                            </template>
                        </div>

                        <!-- 해설 -->
                        <p class="mt-2 text-xs text-gray-400 line-clamp-1"
                           x-text="ind.description" x-show="ind.description"></p>
                    </article>
                </li>
            </template>
        </ul>
    </template>

    <!-- 지표 테이블 -->
    <template x-if="!ecos.loading && ecos.indicators.length > 0">
        <div class="bg-white rounded-xl border border-gray-200 overflow-hidden">
            <div class="overflow-x-auto">
                <table class="data-table w-full text-sm">
                    <thead class="bg-gray-50">
                        <tr>
                            <th class="text-left py-3 px-4 text-gray-600 font-semibold">분류</th>
                            <th class="text-left py-3 px-4 text-gray-600 font-semibold">지표명</th>
                            <th class="text-right py-3 px-4 text-gray-600 font-semibold">값</th>
                            <th class="text-right py-3 px-4 text-gray-600 font-semibold">변화</th>
                            <th class="text-center py-3 px-4 text-gray-600 font-semibold">주기</th>
                            <th class="text-left py-3 px-4 text-gray-600 font-semibold">단위</th>
                        </tr>
                    </thead>
                    <tbody>
                        <template x-for="(ind, idx) in getSortedIndicators()" :key="idx">
                            <tr class="border-b border-gray-100 group relative">
                                <td class="py-3 px-4 text-gray-600" x-text="ind.className"></td>
                                <td class="py-3 px-4 text-gray-800 font-medium">
                                    <div class="flex items-center gap-1">
                                        <span x-text="ind.keystatName"></span>
                                        <template x-if="ind.description">
                                            <div class="group/tip relative inline-block">
                                                <span class="text-gray-300 hover:text-gray-500 cursor-help text-xs">&#9432;</span>
                                                <div class="invisible opacity-0 group-hover/tip:visible group-hover/tip:opacity-100
                                                            absolute z-10 bottom-full left-0 mb-2
                                                            px-3 py-2 text-xs text-white bg-gray-800 rounded-lg shadow-lg
                                                            w-56 transition-opacity duration-200"
                                                     role="tooltip">
                                                    <span x-text="ind.description"></span>
                                                </div>
                                            </div>
                                        </template>
                                    </div>
                                </td>
                                <td class="py-3 px-4 text-right font-mono text-gray-800 font-medium"
                                    x-text="Format.number(ind.dataValue)"></td>
                                <td class="py-3 px-4 text-right">
                                    <template x-if="ind.previousDataValue">
                                        <span class="text-sm font-medium"
                                              :class="Format.change(ind.dataValue, ind.previousDataValue).className">
                                            <span x-text="Format.change(ind.dataValue, ind.previousDataValue).symbol"></span>
                                            <span x-text="Format.changeRate(ind.dataValue, ind.previousDataValue)"></span>
                                        </span>
                                    </template>
                                    <template x-if="!ind.previousDataValue">
                                        <span class="text-gray-400">-</span>
                                    </template>
                                </td>
                                <td class="py-3 px-4 text-center">
                                    <span class="bg-gray-100 text-gray-600 text-xs px-2 py-0.5 rounded-full"
                                          x-text="ind.cycle"></span>
                                </td>
                                <td class="py-3 px-4 text-gray-500" x-text="ind.unitName"></td>
                            </tr>
                        </template>
                    </tbody>
                </table>
            </div>
        </div>
    </template>

    <!-- 빈 상태 -->
    <template x-if="!ecos.loading && ecos.indicators.length === 0 && ecos.selectedCategory">
        <div class="bg-white rounded-xl border border-gray-200 p-8 text-center text-gray-400 text-sm">
            해당 카테고리에 지표가 없습니다.
        </div>
    </template>
</div>
```