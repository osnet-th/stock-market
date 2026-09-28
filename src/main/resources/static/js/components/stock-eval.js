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
            subTab: 'timeline',          // timeline | disclosures | srim
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

        // S-RIM 즉석 계산기 (저장 없음). 화면 요소는 기업 리포트 S-RIM을 복제하고, 숫자 파싱·표시 헬퍼(_crSrim*)만 재사용한다.
        srim: { amountScale: '0', appliedScale: '0', equity: '', equityDate: '', shares: '', sharesDate: '', requiredReturn: '',
            referencePrice: '', referencePriceDate: '', years: [], result: null, error: '', loading: false,
            fetchLoading: false, fetchError: '', basis: null, priceMetrics: null, autoFilled: {}, sources: {}, _gen: 0, _fetchGen: 0 },
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
        this.stockEval.srim = this._seSrimEmpty();
    },

    setEvalProvider(provider) {
        this.stockEval.provider = provider;
    },

    setEvalDartSubTab(subTab) {
        this.stockEval.dart.subTab = subTab;
    },

    // ==================== S-RIM (DART 탭, 저장 없음) ====================
    // 종목 전환 시 초기화 — 필드 구성은 위 stockEval.srim 초기값과 같게 유지한다
    _seSrimEmpty() {
        return { amountScale: '0', appliedScale: '0', equity: '', equityDate: '', shares: '', sharesDate: '', requiredReturn: '',
            referencePrice: '', referencePriceDate: '', years: [], result: null, error: '', loading: false,
            fetchLoading: false, fetchError: '', basis: null, priceMetrics: null, autoFilled: {}, sources: {}, _gen: 0, _fetchGen: 0 };
    },

    seSrim() {
        return this.stockEval.srim;
    },

    seSrimChanged() {
        const s = this.seSrim();
        s._gen += 1;
        s.result = null;
        s.error = '';
        s.loading = false;
    },

    seSrimChangeUnit() {
        const s = this.seSrim();
        this.seSrimChanged();
        const shift = Number(s.appliedScale) - Number(s.amountScale);
        try {
            const equity = this._crSrimDecimal(s.equity, '지배주주지분', shift);
            const years = s.years.map(row => {
                const copy = { ...row };
                ['previousEquity', 'expectedEquity', 'expectedIncome'].forEach(key => {
                    copy[key] = this._crSrimDecimal(row[key], 'ROE 근거값', shift) ?? '';
                });
                return copy;
            });
            s.equity = equity ?? '';
            s.years = years;
            s.appliedScale = s.amountScale;
        } catch (e) {
            s.amountScale = s.appliedScale;
            s.error = e.message;
        }
    },

    seSrimAddYear() {
        const s = this.seSrim();
        if (s.years.length >= 30) return;
        const filled = s.years.map(r => Number(r.year)).filter(y => Number.isInteger(y) && y > 0);
        const next = filled.length ? Math.max(...filled) + 1 : new Date().getFullYear();
        s.years.push({ year: next <= 2200 ? String(next) : '', mode: 'DIRECT', directRoe: '', previousEquity: '', expectedEquity: '', expectedIncome: '' });
        this.seSrimChanged();
    },

    seSrimRemoveYear(index) {
        this.seSrim().years.splice(index, 1);
        this.seSrimChanged();
    },

    // 버튼 클릭 시에만 리포트 미리보기(스냅샷 조립)를 호출한다 — 무거운 호출이라 로딩·실패를 표시한다.
    async seSrimFetch() {
        const s = this.seSrim();
        const stockCode = this.stockEval.selected?.stockCode;
        if (!stockCode || s.fetchLoading) return;
        const gen = ++s._fetchGen;
        s.fetchLoading = true;
        s.fetchError = '';
        try {
            const preview = await API.previewCompanyReport(stockCode);
            if (gen !== s._fetchGen || stockCode !== this.stockEval.selected?.stockCode) return;
            s.basis = preview?.snapshot?.srimBasis || null;
            s.priceMetrics = preview?.snapshot?.priceMetrics || null;
            if (!s.basis) {
                s.fetchError = '이 종목은 자동으로 채울 재무 데이터가 없습니다. 직접 입력하세요.';
                return;
            }
            this._seSrimApply(s);
        } catch (e) {
            if (gen === s._fetchGen) s.fetchError = e?.message || '재무 데이터를 가져오지 못했습니다.';
        } finally {
            if (gen === s._fetchGen) s.fetchLoading = false;
        }
    },

    _seSrimApply(s) {
        const basis = s.basis;
        const scale = Number(s.amountScale);
        const equitySource = ['지배주주지분', basis.equityReport, basis.equityDate].filter(Boolean).join(' · ');
        const sharesSource = [(basis.sharesCategory || '') + ' 유통주식수(자기주식 차감)', basis.sharesReport, basis.sharesDate]
            .filter(Boolean).join(' · ');
        this._seSrimFill(s, 'equity', basis.equity, v => this._crSrimShift(v, -scale), equitySource);
        this._seSrimFill(s, 'equityDate', basis.equityDate, v => v, equitySource);
        this._seSrimFill(s, 'shares', basis.shares, v => v, sharesSource);
        this._seSrimFill(s, 'sharesDate', basis.sharesDate, v => v, sharesSource);
        const p = s.priceMetrics;
        if (p?.referencePrice != null && p.referencePriceDate) {
            const priceSource = '리포트 기준 주가 · ' + p.referencePriceDate;
            this._seSrimFill(s, 'referencePrice', String(p.referencePrice), v => v, priceSource);
            this._seSrimFill(s, 'referencePriceDate', p.referencePriceDate, v => v, priceSource);
        }
        s.years.forEach(row => this._seSrimFillPreviousEquity(s, row, scale));
        this.seSrimChanged();
    },

    // 비어 있거나 직전 자동 채움 값 그대로인 칸만 갱신한다 (직접 고친 칸 보존)
    _seSrimFill(s, key, raw, toDisplay, source) {
        if (raw == null || raw === '') return;
        const current = s[key];
        const empty = current == null || String(current).trim() === '';
        if (!empty && !this._seSrimIsAutoValue(s, key, current)) return;
        s[key] = toDisplay(String(raw));
        s.autoFilled = { ...s.autoFilled, [key]: key === 'equity' ? this._crSrimShift(String(raw), 0) : String(raw) };
        s.sources = { ...s.sources, [key]: source };
    },

    _seSrimIsAutoValue(s, key, current) {
        const raw = s.autoFilled?.[key];
        const text = String(current ?? '').trim();
        if (raw == null || !text) return false;
        if (key !== 'equity') return text === raw;
        return /^[+-]?\d+(\.\d+)?$/.test(text) && this._crSrimShift(text, Number(s.amountScale)) === raw;
    },

    _seSrimFillPreviousEquity(s, row, scale) {
        if (row.mode !== 'CALCULATED') return;
        const year = Number(row.year);
        const raw = Number.isInteger(year) ? s.basis.yearEndEquities?.[String(year - 1)] : null;
        if (raw == null) return;
        if (String(row.previousEquity ?? '').trim() && !this._seSrimIsAutoRow(s, row)) return;
        row.previousEquity = this._crSrimShift(String(raw), -scale);
        row._autoPrevious = this._crSrimShift(String(raw), 0);
    },

    _seSrimIsAutoRow(s, row) {
        const current = String(row.previousEquity ?? '').trim();
        if (row._autoPrevious == null || !/^[+-]?\d+(\.\d+)?$/.test(current)) return false;
        return this._crSrimShift(current, Number(s.amountScale)) === row._autoPrevious;
    },

    seSrimSource(key) {
        const s = this.seSrim();
        return this._seSrimIsAutoValue(s, key, s[key]) ? (s.sources?.[key] || '') : '';
    },

    seSrimRowSource(row) {
        return this._seSrimIsAutoRow(this.seSrim(), row) ? (Number(row.year) - 1) + '년 말 지배주주지분 (사업보고서)' : '';
    },

    _seSrimInput() {
        const s = this.seSrim();
        const scale = Number(s.amountScale);
        return { equity: this._crSrimDecimal(s.equity, '지배주주지분', scale), equityDate: s.equityDate || null,
            shares: this._crSrimDecimal(s.shares, '유통주식수'), sharesDate: s.sharesDate || null,
            requiredReturn: this._crSrimDecimal(s.requiredReturn, '요구수익률', -2), currency: 'KRW',
            referencePrice: this._crSrimDecimal(s.referencePrice, '비교 주가'), referencePriceDate: s.referencePriceDate || null,
            years: s.years.map(row => {
                const text = String(row.year ?? '').trim();
                if (text && !/^\d{4}$/.test(text)) throw new Error('연도를 네 자리 정수로 입력하세요.');
                return { year: text ? Number(text) : null, mode: row.mode,
                    directRoe: this._crSrimDecimal(row.directRoe, '예상 ROE', -2),
                    previousEquity: this._crSrimDecimal(row.previousEquity, '전기말 지분', scale),
                    expectedEquity: this._crSrimDecimal(row.expectedEquity, '당기말 지분', scale),
                    expectedIncome: this._crSrimDecimal(row.expectedIncome, '예상 순이익', scale) };
            }) };
    },

    // 기존 S-RIM 계산 미리보기 API 재사용 — 결과는 화면 상태에만 두고 저장하지 않는다
    async seSrimCalculate() {
        this.seSrimChanged();
        const s = this.seSrim();
        const gen = s._gen;
        s.loading = true;
        try {
            const result = await API.calculateCompanyReportSrim(this._seSrimInput());
            if (gen === s._gen) s.result = result;
        } catch (e) {
            if (gen === s._gen) s.error = e?.message || 'S-RIM 계산에 실패했습니다.';
        } finally {
            if (gen === s._gen) s.loading = false;
        }
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
        if (this.stockEval.amountUnit === '조') {
            // 조 변환: 반올림하지 않고 소수점 2자리에서 버림 (공용 Format.truncTo2)
            return Format.truncTo2(n / 10000).toLocaleString('ko-KR', { maximumFractionDigits: 2 });
        }
        return n.toLocaleString('ko-KR', { maximumFractionDigits: 2 });
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

    // ===== 추정 손익(output2) 전용 렌더링 =====
    // 백엔드가 6행(금액/증감률 × 3지표)으로 주는 "추정 손익" 섹션을 3줄+증감률 인라인으로 재구성.

    // "추정 손익" 섹션 판별 (짝수 행 = 금액+증감률 쌍일 때만 전용 표 적용, 아니면 generic 폴백)
    isEstimateIncomeSection(sec) {
        return !!(sec && sec.title === '추정 손익'
            && sec.table && Array.isArray(sec.table.rows)
            && sec.table.rows.length >= 2 && sec.table.rows.length % 2 === 0);
    },

    // 기간 헤더: columns[1..] → { label, est }. "2026.12E" → { label:'2026', est:true }
    estimateIncomePeriods(sec) {
        const cols = (sec && sec.table && sec.table.columns) ? sec.table.columns.slice(1) : [];
        return cols.map(function (c) {
            const s = String(c);
            return { label: s.replace(/\.\d{2}E?$/, ''), est: /E$/.test(s) };
        });
    },

    // 6행 → 3그룹(지표별 금액+증감률). cell = { amt, chg, dir, est }
    estimateIncomeGroups(sec) {
        const table = (sec && sec.table) ? sec.table : {};
        const rows = table.rows || [];
        const cols = table.columns || [];
        const groups = [];
        for (let i = 0; i + 1 < rows.length; i += 2) {
            const amtRow = rows[i] || [];
            const chgRow = rows[i + 1] || [];
            const metric = String(amtRow[0] || '').replace(/\s*\(억원\)\s*/, '').trim();
            const cells = [];
            for (let j = 1; j < amtRow.length; j++) {
                const n = parseFloat(chgRow[j]);
                cells.push({
                    amt: amtRow[j],
                    chg: chgRow[j],
                    dir: isNaN(n) ? 'none' : (n > 0 ? 'up' : (n < 0 ? 'down' : 'flat')),
                    est: /E$/.test(String(cols[j] || '')),
                });
            }
            groups.push({ metric: metric, cells: cells });
        }
        return groups;
    },

    // 증감률 표시: ▲/▼ + 절대값% (국내 관례: 증가 빨강, 감소 파랑)
    fmtChangePct(v) {
        const n = parseFloat(v);
        if (isNaN(n)) return '';
        const sym = n > 0 ? '▲' : (n < 0 ? '▼' : '');
        return (sym ? sym + ' ' : '') + Math.abs(n).toLocaleString('ko-KR', { maximumFractionDigits: 1 }) + '%';
    },

    changeClass(dir) {
        return dir === 'up' ? 'text-red-500' : dir === 'down' ? 'text-blue-500' : 'text-gray-400';
    },

    estimateUnitLabel() {
        return this.stockEval.amountUnit === '조' ? '조원' : '억원';
    },
};
