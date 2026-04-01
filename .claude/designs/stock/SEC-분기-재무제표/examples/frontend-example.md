# 프론트엔드 연간/분기 토글

## api.js 추가

```javascript
getSecQuarterlyStatements(ticker) {
    return this.request('GET', `/api/stocks/${ticker}/sec/financial/statements/quarterly`);
},
```

## portfolio.js 상태 추가

```javascript
secQuarterlyPeriod: 'annual',   // 'annual' | 'quarterly'
secQuarterlyData: null,         // 분기 데이터 캐시
```

## financial.js 변경

```javascript
async loadSecFinancial(menuKey) {
    const ticker = this.portfolio.selectedStockItem?.stockDetail?.stockCode;
    if (!ticker) return;

    const thisGeneration = ++this.portfolio._financialRequestGeneration;
    this.portfolio.financialLoading = true;
    this.portfolio.secFinancialError = null;

    try {
        if (menuKey === 'sec-metrics') {
            // 투자지표는 연간 전용 (기존 로직 유지)
            if (!this.portfolio.secMetricsData) {
                this.portfolio.secMetricsData = await API.getSecInvestmentMetrics(ticker);
            }
            if (thisGeneration !== this.portfolio._financialRequestGeneration) return;
            this.portfolio.financialResult = this.portfolio.secMetricsData.map(m => ({
                name: m.name,
                formattedValue: this.formatSecMetricValue(m.value, m.unit),
                description: m.description
            }));
        } else {
            const isQuarterly = this.portfolio.secQuarterlyPeriod === 'quarterly';

            if (isQuarterly) {
                if (!this.portfolio.secQuarterlyData) {
                    this.portfolio.secQuarterlyData = await API.getSecQuarterlyStatements(ticker);
                }
                if (thisGeneration !== this.portfolio._financialRequestGeneration) return;
                const targetType = this._secStatementTypeMap[menuKey];
                const statement = this.portfolio.secQuarterlyData.find(s => s.statementType === targetType);
                this.portfolio.financialResult = statement?.items
                    ? this.buildSecTableRows(statement.items) : [];
            } else {
                if (!this.portfolio.secFinancialData) {
                    this.portfolio.secFinancialData = await API.getSecFinancialStatements(ticker);
                }
                if (thisGeneration !== this.portfolio._financialRequestGeneration) return;
                const targetType = this._secStatementTypeMap[menuKey];
                const statement = this.portfolio.secFinancialData.find(s => s.statementType === targetType);
                this.portfolio.financialResult = statement?.items
                    ? this.buildSecTableRows(statement.items) : [];
            }
        }
    } catch (e) {
        // ... 기존 에러 처리 동일
    } finally {
        // ... 기존 동일
    }
},

// 토글 전환 시 호출
async toggleSecPeriod(period) {
    this.portfolio.secQuarterlyPeriod = period;
    const menu = this.portfolio.selectedFinancialMenu;
    if (menu && menu.startsWith('sec-') && menu !== 'sec-metrics') {
        await this.loadSecFinancial(menu);
    }
},
```

## buildSecTableRows 변경

키가 String이므로 기존 `Number` 변환 로직 수정:

```javascript
buildSecTableRows(items) {
    if (!items || items.length === 0) return [];

    // 키가 "2024" 또는 "2024Q1" 형식
    const periods = Object.keys(items[0].values || {}).sort().reverse();

    return items.map(item => {
        const row = { label: item.label };
        for (const period of periods) {
            const val = item.values ? item.values[period] : null;
            row['p' + period] = val !== null && val !== undefined ? Format.usd(val) : '-';
        }
        return row;
    });
},

getSecTableColumns(menuKey) {
    if (menuKey === 'sec-metrics') {
        return this.secFinancialColumns['sec-metrics'];
    }

    const result = this.portfolio.financialResult;
    if (!result || result.length === 0) return this.secFinancialColumns[menuKey];

    const firstRow = result[0];
    const cols = [{ key: 'label', label: '항목', type: 'text' }];
    const periodKeys = Object.keys(firstRow).filter(k => k.startsWith('p')).sort().reverse();
    for (const pk of periodKeys) {
        const period = pk.substring(1);
        // "2024" → "2024년", "2024Q1" → "2024 Q1"
        const label = period.includes('Q') ? period.replace('Q', ' Q') : period + '년';
        cols.push({ key: pk, label: label, type: 'text' });
    }
    return cols;
},
```

## index.html 토글 UI

```html
<!-- SEC 재무제표 연간/분기 토글 (투자지표 제외) -->
<div x-show="isSecMenu() && portfolio.selectedFinancialMenu !== 'sec-metrics'"
     class="flex gap-1 mb-3">
    <button @click="toggleSecPeriod('annual')"
            :class="portfolio.secQuarterlyPeriod === 'annual'
                ? 'bg-blue-600 text-white' : 'bg-gray-200 text-gray-600'"
            class="px-3 py-1 text-xs rounded-l font-medium transition">연간</button>
    <button @click="toggleSecPeriod('quarterly')"
            :class="portfolio.secQuarterlyPeriod === 'quarterly'
                ? 'bg-blue-600 text-white' : 'bg-gray-200 text-gray-600'"
            class="px-3 py-1 text-xs rounded-r font-medium transition">분기</button>
</div>
```
