/** Stock Evaluation - 종목 평가 (KIS 국내주식 종목정보). 보유 여부 무관 매수 전 리서치 */
let _stockEvalIndexChart = null; // 업종지수 Chart.js 인스턴스 (Alpine 반응형 밖에서 관리)

const StockEvalComponent = {
    stockEval: {
        searchQuery: '',
        searchResults: [],
        searchLoading: false,
        selected: null,          // { stockCode, stockName, ... }
        summary: { data: null, loading: false, error: '', _gen: 0 },  // 요약 카드 (주식기본조회)
        industryIndex: { points: [], loading: false, loaded: false, error: '', _gen: 0 },  // 업종 일자별 지수
        activeTab: 'finance',    // finance | estimate | credit | schedule
        amountUnit: '억',        // 금액 표시 단위: 억 | 조 (재무 대차/손익, 추정 손익에 적용)
        _searchGen: 0,

        financeTypes: [
            { code: 'balance-sheet', label: '대차대조표' },
            { code: 'income-statement', label: '손익계산서' },
            { code: 'financial-ratio', label: '재무비율' },
            { code: 'profit-ratio', label: '수익성비율' },
            { code: 'other-major-ratios', label: '기타주요비율' },
            { code: 'stability-ratio', label: '안정성비율' },
            { code: 'growth-ratio', label: '성장성비율' },
        ],
        finance: { type: 'balance-sheet', divCls: 'ANNUAL', table: null, loaded: false, loading: false, error: '', _gen: 0 },

        estimate: { sections: [], loaded: false, loading: false, error: '', _gen: 0 },
        credit: { data: null, loaded: false, loading: false, error: '', _gen: 0 },

        scheduleTypes: [
            { code: 'dividend', label: '배당' },
            { code: 'purchase-request', label: '주식매수청구' },
            { code: 'merger-split', label: '합병/분할' },
            { code: 'face-value-change', label: '액면교체' },
            { code: 'capital-reduction', label: '자본감소' },
            { code: 'listing', label: '상장정보' },
            { code: 'public-offering', label: '공모주청약' },
            { code: 'forfeited', label: '실권주' },
            { code: 'mandatory-deposit', label: '의무예치' },
            { code: 'paid-in-capital', label: '유상증자' },
            { code: 'bonus-issue', label: '무상증자' },
            { code: 'shareholders-meeting', label: '주주총회' },
        ],
        schedule: { type: 'dividend', fromDate: '', toDate: '', table: null, loaded: false, loading: false, error: '', _gen: 0 },

        // 상위 탭: 한국투자증권(KIS) / DART
        provider: 'kis',   // kis | dart

        // DART 재무상세 컨텍스트 (financial.js 타임라인/공시 로직을 ctx로 재사용).
        // 필드명은 portfolio와 동일해야 공용 메서드가 동작. canvasPrefix로 canvas id 유일화.
        dart: {
            subTab: 'timeline',          // timeline | disclosures
            stockCode: null,
            canvasPrefix: 'eval-',
            timelineData: null, timelineLoading: false, timelineError: null,
            timelineYears: '5', timelineFsDiv: 'CFS',
            timelineExpandedStatements: { IS: true },
            timelineExpandedIndexClasses: {},
            timelineExpandedDetailCategories: {},
            _timelineCharts: [],
            _financialRequestGeneration: 0,
            disclosureData: null, disclosureLoading: false, disclosureError: null,
            disclosureSelectedTypes: [], disclosurePeriod: '1',
        },
    },

    // ==================== 검색 / 선택 ====================
    async stockEvalSearch() {
        const q = this.stockEval.searchQuery.trim();
        if (!q) return;
        const gen = ++this.stockEval._searchGen;
        this.stockEval.searchLoading = true;
        try {
            const results = await API.searchStocks(q) || [];
            if (gen !== this.stockEval._searchGen) return; // 레이스 가드
            this.stockEval.searchResults = results.filter(r => r.exchangeCode === 'KRX'); // 국내(KRX)만
        } catch (e) {
            if (gen !== this.stockEval._searchGen) return;
            console.error('종목 검색 실패:', e);
            this.stockEval.searchResults = [];
        } finally {
            if (gen === this.stockEval._searchGen) this.stockEval.searchLoading = false;
        }
    },

    async stockEvalSelect(stock) {
        this.stockEval.selected = stock;
        this.stockEval.searchResults = [];
        this.stockEval.searchQuery = '';
        this.stockEval.provider = 'kis';
        this.stockEval.activeTab = 'finance';
        this._stockEvalResetTabs();
        this._stockEvalResetDart(stock.stockCode);
        Object.assign(this.stockEval.industryIndex, { points: [], loading: false, loaded: false, error: '' });
        this.destroyIndexChart();
        await this.stockEvalLoadSummary();
        await this.stockEvalLoadFinance(); // 기본 탭
    },

    // === 상위 탭(KIS/DART) + DART 재무상세 ===

    // DART 컨텍스트 초기화 (종목 전환 시). 차트·상태 리셋 후 종목코드 설정
    _stockEvalResetDart(stockCode) {
        const dart = this.stockEval.dart;
        this.resetTimelineState(dart);
        this.resetDisclosureState(dart);
        dart.subTab = 'timeline';
        dart.timelineYears = '5';
        dart.timelineFsDiv = 'CFS';
        dart.disclosurePeriod = '1';
        dart.disclosureSelectedTypes = [];
        dart.stockCode = stockCode;
    },

    setEvalProvider(provider) {
        this.stockEval.provider = provider;
    },

    setEvalDartSubTab(subTab) {
        this.stockEval.dart.subTab = subTab;
    },

    _stockEvalResetTabs() {
        Object.assign(this.stockEval.finance, { type: 'balance-sheet', divCls: 'ANNUAL', table: null, loaded: false, error: '' });
        Object.assign(this.stockEval.estimate, { sections: [], loaded: false, error: '' });
        Object.assign(this.stockEval.credit, { data: null, loaded: false, error: '' });
        Object.assign(this.stockEval.schedule, { type: 'dividend', fromDate: '', toDate: '', table: null, loaded: false, error: '' });
    },

    async stockEvalLoadSummary() {
        if (!this.stockEval.selected) return;
        const code = this.stockEval.selected.stockCode;
        const s = this.stockEval.summary;
        const gen = ++s._gen;
        s.loading = true;
        s.error = '';
        try {
            const data = await API.getStockSummary(code);
            if (gen !== s._gen) return;
            s.data = data;
            if (data && data.industryIndexCode) this.stockEvalLoadIndustryIndex();
        } catch (e) {
            if (gen !== s._gen) return;
            s.data = null;
            s.error = '종목 요약 정보를 불러올 수 없습니다.';
        } finally {
            if (gen === s._gen) s.loading = false;
        }
    },

    // ==================== 업종 일자별 지수 (요약 카드 하단 차트) ====================
    async stockEvalLoadIndustryIndex() {
        const code = this.stockEval.summary.data?.industryIndexCode;
        const idx = this.stockEval.industryIndex;
        if (!code) { idx.points = []; idx.loaded = true; return; }
        const gen = ++idx._gen;
        idx.loading = true;
        idx.error = '';
        try {
            const res = await API.getIndustryIndex(code);
            if (gen !== idx._gen) return;
            idx.points = (res && res.points) ? res.points : [];
            idx.loaded = true;
            this.$nextTick(() => this.renderIndustryIndexChart());
        } catch (e) {
            if (gen !== idx._gen) return;
            idx.points = [];
            idx.error = '업종 지수를 불러올 수 없습니다.';
            this.destroyIndexChart();
        } finally {
            if (gen === idx._gen) idx.loading = false;
        }
    },

    renderIndustryIndexChart() {
        this.destroyIndexChart();
        const pts = this.stockEval.industryIndex.points;
        if (!pts || pts.length === 0) return;
        const canvas = document.getElementById('stock-eval-index-chart');
        if (!canvas || typeof Chart === 'undefined') return;
        _stockEvalIndexChart = new Chart(canvas, {
            type: 'line',
            data: {
                labels: pts.map(p => p.date),
                datasets: [{
                    data: pts.map(p => { const v = parseFloat(p.value); return isNaN(v) ? null : v; }),
                    borderColor: '#6366F1',
                    borderWidth: 1.5,
                    pointRadius: 0,
                    pointHoverRadius: 4,
                    tension: 0.3,
                    fill: false,
                    spanGaps: true,
                }]
            },
            options: {
                animation: false,
                responsive: true,
                maintainAspectRatio: false,
                plugins: { legend: { display: false }, tooltip: { mode: 'index', intersect: false } },
                scales: {
                    x: { ticks: { maxRotation: 45, autoSkip: true, maxTicksLimit: 8, font: { size: 10 } }, grid: { display: false } },
                    y: { ticks: { maxTicksLimit: 5, font: { size: 10 } } }
                }
            }
        });
    },

    destroyIndexChart() {
        if (_stockEvalIndexChart) {
            try { _stockEvalIndexChart.destroy(); } catch (e) { /* ignore */ }
            _stockEvalIndexChart = null;
        }
    },

    stockEvalSelectTab(tab) {
        this.stockEval.activeTab = tab;
        if (tab === 'finance' && !this.stockEval.finance.loaded) this.stockEvalLoadFinance();
        if (tab === 'estimate' && !this.stockEval.estimate.loaded) this.stockEvalLoadEstimate();
        if (tab === 'credit' && !this.stockEval.credit.loaded) this.stockEvalLoadCredit();
        if (tab === 'schedule' && !this.stockEval.schedule.loaded) this.stockEvalLoadSchedule();
    },

    stockEvalReset() {
        this.stockEval.selected = null;
        Object.assign(this.stockEval.summary, { data: null, loading: false, error: '' });
        Object.assign(this.stockEval.industryIndex, { points: [], loading: false, loaded: false, error: '' });
        this.destroyIndexChart();
        this.stockEval.searchResults = [];
        this.stockEval.searchQuery = '';
        this._stockEvalResetTabs();
    },

    // ==================== 재무 탭 ====================
    async stockEvalLoadFinance() {
        if (!this.stockEval.selected) return;
        const code = this.stockEval.selected.stockCode;
        const f = this.stockEval.finance;
        const gen = ++f._gen;
        f.loading = true;
        f.error = '';
        try {
            const table = await API.getStockFinance(code, f.type, f.divCls);
            if (gen !== f._gen) return;
            f.table = table;
            f.loaded = true;
        } catch (e) {
            if (gen !== f._gen) return;
            f.table = null;
            f.error = '재무 정보를 불러올 수 없습니다.';
        } finally {
            if (gen === f._gen) f.loading = false;
        }
    },

    stockEvalSetFinanceType(type) {
        this.stockEval.finance.type = type;
        this.stockEvalLoadFinance();
    },

    stockEvalSetFinanceDivCls(divCls) {
        this.stockEval.finance.divCls = divCls;
        this.stockEvalLoadFinance();
    },

    // ==================== 추정실적 탭 ====================
    async stockEvalLoadEstimate() {
        if (!this.stockEval.selected) return;
        const code = this.stockEval.selected.stockCode;
        const e = this.stockEval.estimate;
        const gen = ++e._gen;
        e.loading = true;
        e.error = '';
        try {
            const res = await API.getStockEstimatePerform(code);
            if (gen !== e._gen) return;
            e.sections = res && res.sections ? res.sections : [];
            e.loaded = true;
        } catch (err) {
            if (gen !== e._gen) return;
            e.sections = [];
            e.error = '추정실적을 불러올 수 없습니다.';
        } finally {
            if (gen === e._gen) e.loading = false;
        }
    },

    // ==================== 신용 탭 ====================
    async stockEvalLoadCredit() {
        if (!this.stockEval.selected) return;
        const code = this.stockEval.selected.stockCode;
        const c = this.stockEval.credit;
        const gen = ++c._gen;
        c.loading = true;
        c.error = '';
        try {
            const res = await API.getStockCreditEligibility(code);
            if (gen !== c._gen) return;
            c.data = res;
            c.loaded = true;
        } catch (err) {
            if (gen !== c._gen) return;
            c.data = null;
            c.error = '신용거래 가능 여부를 불러올 수 없습니다.';
        } finally {
            if (gen === c._gen) c.loading = false;
        }
    },

    // ==================== 일정 탭 ====================
    async stockEvalLoadSchedule() {
        if (!this.stockEval.selected) return;
        const code = this.stockEval.selected.stockCode;
        const s = this.stockEval.schedule;
        const gen = ++s._gen;
        s.loading = true;
        s.error = '';
        try {
            const table = await API.getStockSchedule(code, s.type, s.fromDate, s.toDate);
            if (gen !== s._gen) return;
            s.table = table;
            s.loaded = true;
        } catch (e) {
            if (gen !== s._gen) return;
            s.table = null;
            s.error = '일정 정보를 불러올 수 없습니다.';
        } finally {
            if (gen === s._gen) s.loading = false;
        }
    },

    stockEvalSetScheduleType(type) {
        this.stockEval.schedule.type = type;
        this.stockEvalLoadSchedule();
    },

    // 표 숫자 셀 표시 포맷: 소수점 포함 순수 숫자만 천단위 콤마 (결산년월/종목코드/날짜는 제외)
    fmtNum(v) {
        if (typeof v !== 'string') return v;
        if (/^-?\d+\.\d+$/.test(v)) {
            const n = parseFloat(v);
            return isNaN(n) ? v : n.toLocaleString('ko-KR', { maximumFractionDigits: 2 });
        }
        return v;
    },

    // ==================== 금액 단위(억/조) 토글 ====================
    stockEvalSetUnit(u) {
        this.stockEval.amountUnit = u;
    },

    stockEvalFinanceIsAmount() {
        return ['balance-sheet', 'income-statement'].includes(this.stockEval.finance.type);
    },

    // 억원 금액을 현재 단위(억/조)로 표시 포맷
    fmtAmountByUnit(v) {
        if (typeof v !== 'string' || !/^-?\d+(\.\d+)?$/.test(v)) return this.fmtNum(v);
        const n = parseFloat(v);
        if (isNaN(n)) return v;
        const toJo = this.stockEval.amountUnit === '조';
        const val = toJo ? n / 10000 : n;
        return val.toLocaleString('ko-KR', { maximumFractionDigits: toJo ? 1 : 2 });
    },

    // 재무 표 셀: 결산년월(col0) 원본, 금액 표(대차/손익)는 단위 적용, 비율 표는 일반 콤마
    fmtFinanceCell(cell, ci) {
        if (ci === 0) return cell;
        return this.stockEvalFinanceIsAmount() ? this.fmtAmountByUnit(cell) : this.fmtNum(cell);
    },

    // 추정 표 셀: 항목 라벨(col0)은 단위 치환, (억원) 행은 단위 적용, 그 외 일반 콤마
    fmtEstimateCell(cell, ci, row) {
        if (ci === 0) return this.fmtEstimateLabel(cell);
        return (row && row[0] && row[0].includes('억원')) ? this.fmtAmountByUnit(cell) : this.fmtNum(cell);
    },

    fmtEstimateLabel(label) {
        if (this.stockEval.amountUnit === '조' && typeof label === 'string') {
            return label.replace('(억원)', '(조원)');
        }
        return label;
    },
};
