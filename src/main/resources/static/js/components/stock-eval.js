/** Stock Evaluation - 종목 평가 (KIS 국내주식 종목정보). 보유 여부 무관 매수 전 리서치 */
const StockEvalComponent = {
    stockEval: {
        searchQuery: '',
        searchResults: [],
        searchLoading: false,
        selected: null,          // { stockCode, stockName, ... }
        basicInfo: null,
        basicInfoLoading: false,
        error: '',
        activeTab: 'finance',    // finance | estimate | credit | schedule
        _searchGen: 0,
        _infoGen: 0,

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
        this.stockEval.activeTab = 'finance';
        this._stockEvalResetTabs();
        await this.stockEvalLoadBasicInfo();
        await this.stockEvalLoadFinance(); // 기본 탭
    },

    _stockEvalResetTabs() {
        Object.assign(this.stockEval.finance, { type: 'balance-sheet', divCls: 'ANNUAL', table: null, loaded: false, error: '' });
        Object.assign(this.stockEval.estimate, { sections: [], loaded: false, error: '' });
        Object.assign(this.stockEval.credit, { data: null, loaded: false, error: '' });
        Object.assign(this.stockEval.schedule, { type: 'dividend', fromDate: '', toDate: '', table: null, loaded: false, error: '' });
    },

    async stockEvalLoadBasicInfo() {
        if (!this.stockEval.selected) return;
        const code = this.stockEval.selected.stockCode;
        const gen = ++this.stockEval._infoGen;
        this.stockEval.basicInfoLoading = true;
        this.stockEval.error = '';
        try {
            const info = await API.getStockBasicInfo(code);
            if (gen !== this.stockEval._infoGen) return;
            this.stockEval.basicInfo = info;
        } catch (e) {
            if (gen !== this.stockEval._infoGen) return;
            this.stockEval.basicInfo = null;
            this.stockEval.error = '종목 기본정보를 불러올 수 없습니다.';
        } finally {
            if (gen === this.stockEval._infoGen) this.stockEval.basicInfoLoading = false;
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
        this.stockEval.basicInfo = null;
        this.stockEval.error = '';
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
};
