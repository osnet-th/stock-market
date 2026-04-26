/**
 * StocknoteComponent — 주식 기록 기능 (내 투자 노트).
 *
 * 소유 프로퍼티: stocknote
 * 외부 프로퍼티: auth (userId), API (api.js)
 *
 * 설계 원칙 (docs/plans/2026-04-23-001-feat-stock-note-plan.md):
 *  - Chart.js 인스턴스는 Alpine reactive proxy 바깥 모듈 스코프 Map 에 보관
 *    (심화 권고 19, ecos-timeseries-chart-visualization.md 학습).
 *  - 기록/조회 한 화면 3탭 (대시보드 / 종목 차트 / 기록 리스트) + 우측 드로워.
 *  - 기본 골격 (Phase 6). 드로워(Phase 7), Chart.js mixed(Phase 8), 폴리싱(Phase 9) 후속.
 */

// Chart.js 인스턴스 레지스트리 — Alpine reactive 바깥 모듈 스코프 (심화 19)
const _stocknoteChartRegistry = new Map();

// 드로워 폼 초기값 (함수 호출로 deep-copy)
function _stocknoteEmptyForm() {
    return {
        stockCode: '',
        stockName: '',
        marketType: '',
        exchangeCode: '',
        direction: 'UP',
        changePercent: '',
        noteDate: new Date().toISOString().slice(0, 10),
        preReflected: false,
        triggerText: '',
        interpretationText: '',
        riskText: '',
        initialJudgment: 'NEUTRAL',
        // 고정 태그 (멀티 선택)
        triggerTypes: [],     // TriggerType[]
        characters: [],       // RiseCharacter[]
        supplyActors: [],     // SupplyActor[]
        // 자유 태그
        customTags: [],       // string[]
        // 실적 연결
        revenueImpact: '',
        profitImpact: '',
        cashflowImpact: '',
        oneTime: false,
        structural: false,
        // 밸류에이션
        per: '',
        pbr: '',
        evEbitda: '',
        vsAverage: ''
    };
}

function _stocknoteDraftKey(userId) {
    return `stocknote_draft_${userId || 'anon'}`;
}

const StocknoteComponent = {
    stocknote: {
        activeTab: 'dashboard',          // 'dashboard' | 'stock' | 'list'
        loading: false,

        // 대시보드
        dashboard: null,                 // DashboardResponse

        // 종목 차트 탭
        selectedStockCode: null,
        stockChartData: null,            // ChartDataResponse
        stockChartAvailable: false,      // 일봉 공급 여부 (prices 비어 있으면 false)

        // 기록 리스트 탭
        notes: [],                       // Item[]
        totalCount: 0,
        filters: {
            stockCode: '',
            from: '',
            to: '',
            direction: '',
            character: '',
            judgmentResult: '',
            page: 0,
            size: 20
        },

        // 상세/검증 패널
        detail: null,                    // StockNoteDetailResponse
        detailOpen: false,
        verificationForm: { judgmentResult: 'CORRECT', verificationNote: '', saving: false, error: '' },
        similar: null,                   // SimilarPatternResponse
        similarLoading: false,

        // 드로워 (Phase 7 3-section 아코디언 폼)
        drawerOpen: false,
        drawer: {
            saving: false,
            errors: [],
            form: _stocknoteEmptyForm(),
            stockSearch: { query: '', results: [], loading: false, timer: null },
            tagInput: '',
            tagSuggestions: []
        },

        // 고정 enum 선택값들 (프론트 표시용)
        FIXED_ENUMS: {
            trigger: ['DISCLOSURE', 'EARNINGS', 'NEWS', 'POLICY', 'INDUSTRY', 'SUPPLY', 'THEME', 'ETC'],
            character: ['FUNDAMENTAL', 'EXPECTATION', 'SUPPLY_DEMAND', 'THEME', 'REVALUATION'],
            supplyActor: ['FOREIGN', 'INSTITUTION', 'RETAIL', 'SHORT_COVERING', 'ETF_FLOW'],
            userJudgment: ['MORE_UPSIDE', 'NEUTRAL', 'OVERHEATED', 'CATALYST_EXHAUSTED'],
            impactLevel: ['HIGH', 'MEDIUM', 'LOW'],
            vsAverage: ['ABOVE', 'AVERAGE', 'BELOW']
        },

        // 배지 (dashboard.pendingVerificationCount 동기화)
        pendingVerificationBadge: 0
    },

    // =========================================================================
    // 초기화 & 탭 전환
    // =========================================================================

    async loadStocknote() {
        if (!this.checkLoggedIn || !this.checkLoggedIn()) return;
        this.stocknote.activeTab = 'dashboard';
        await this.loadStocknoteDashboard();
    },

    async switchStocknoteTab(tab) {
        this.destroyStocknoteCharts();
        this.stocknote.activeTab = tab;
        if (tab === 'dashboard') {
            await this.loadStocknoteDashboard();
        } else if (tab === 'list') {
            await this.loadStocknoteList();
        } else if (tab === 'stock') {
            // 종목 선택 전까지는 안내 표시
            if (this.stocknote.selectedStockCode) {
                await this.loadStocknoteStockChart(this.stocknote.selectedStockCode);
            }
        }
    },

    // =========================================================================
    // 데이터 로드
    // =========================================================================

    async loadStocknoteDashboard() {
        this.stocknote.loading = true;
        try {
            this.stocknote.dashboard = await API.getStockNoteDashboard();
            this.stocknote.pendingVerificationBadge = this.stocknote.dashboard?.pendingVerificationCount || 0;
        } catch (e) {
            console.error('stocknote dashboard 로드 실패:', e);
        } finally {
            this.stocknote.loading = false;
        }
    },

    async loadStocknoteList() {
        this.stocknote.loading = true;
        try {
            const query = { ...this.stocknote.filters };
            // 빈 문자열 필터는 전송하지 않음
            Object.keys(query).forEach(k => {
                if (query[k] === '' || query[k] === null) delete query[k];
            });
            const response = await API.getStockNoteList(query);
            this.stocknote.notes = response.items || [];
            this.stocknote.totalCount = response.totalCount || 0;
        } catch (e) {
            console.error('stocknote list 로드 실패:', e);
            this.stocknote.notes = [];
        } finally {
            this.stocknote.loading = false;
        }
    },

    async loadStocknoteStockChart(stockCode) {
        this.stocknote.loading = true;
        try {
            const data = await API.getStockNoteChart(stockCode, 90);
            this.stocknote.stockChartData = data;
            this.stocknote.stockChartAvailable = Array.isArray(data.prices) && data.prices.length > 0;
            this.stocknote.selectedStockCode = stockCode;
            // Alpine x-show 가 display:none → block 으로 적용된 뒤 캔버스 사이즈가 확정되어야
            // Chart.js 가 정상 크기로 렌더된다. $nextTick + 더블 RAF 로 레이아웃 commit 보장.
            this.$nextTick(() => {
                requestAnimationFrame(() => requestAnimationFrame(() => this.renderStocknoteStockChart()));
            });
        } catch (e) {
            console.error('stocknote chart 로드 실패:', e);
            this.stocknote.stockChartData = null;
            this.stocknote.stockChartAvailable = false;
        } finally {
            this.stocknote.loading = false;
        }
    },

    async openStocknoteDetail(noteId) {
        try {
            this.stocknote.detail = await API.getStockNoteDetail(noteId);
            this.stocknote.detailOpen = true;
            this.initStocknoteVerificationForm();
            // 유사 패턴은 백그라운드 로드 (패널 오픈 지연 회피)
            this.loadStocknoteSimilarPatterns(noteId);
        } catch (e) {
            console.error('stocknote detail 로드 실패:', e);
        }
    },

    closeStocknoteDetail() {
        this.stocknote.detailOpen = false;
        this.stocknote.detail = null;
    },

    // =========================================================================
    // 드로워 (Phase 7: 3-section 폼)
    // =========================================================================

    openStocknoteDrawer() {
        this.stocknote.drawer.errors = [];
        this.restoreStocknoteDraft();
        this.stocknote.drawerOpen = true;
    },

    closeStocknoteDrawer() {
        this.stocknote.drawerOpen = false;
    },

    resetStocknoteDrawer() {
        this.stocknote.drawer.form = _stocknoteEmptyForm();
        this.stocknote.drawer.errors = [];
        this.stocknote.drawer.stockSearch = { query: '', results: [], loading: false, timer: null };
        this.stocknote.drawer.tagInput = '';
        this.stocknote.drawer.tagSuggestions = [];
        this.clearStocknoteDraft();
    },

    // --- localStorage draft (key scope: userId) -----------------------------

    _getStocknoteUserId() {
        return this.auth && this.auth.userId ? this.auth.userId : null;
    },

    persistStocknoteDraft() {
        try {
            const userId = this._getStocknoteUserId();
            localStorage.setItem(_stocknoteDraftKey(userId),
                    JSON.stringify(this.stocknote.drawer.form));
        } catch (_) { /* noop */ }
    },

    restoreStocknoteDraft() {
        try {
            const userId = this._getStocknoteUserId();
            const raw = localStorage.getItem(_stocknoteDraftKey(userId));
            if (!raw) return;
            const parsed = JSON.parse(raw);
            this.stocknote.drawer.form = { ..._stocknoteEmptyForm(), ...parsed };
        } catch (_) { /* noop */ }
    },

    clearStocknoteDraft() {
        try {
            const userId = this._getStocknoteUserId();
            localStorage.removeItem(_stocknoteDraftKey(userId));
        } catch (_) { /* noop */ }
    },

    // --- 종목 검색 autocomplete (portfolio.js 패턴 차용) ---------------------

    onStocknoteStockSearchInput() {
        if (this.stocknote.drawer.stockSearch.timer) {
            clearTimeout(this.stocknote.drawer.stockSearch.timer);
        }
        this.stocknote.drawer.stockSearch.timer = setTimeout(
                () => this.searchStocknoteStock(), 300);
    },

    async searchStocknoteStock() {
        const q = (this.stocknote.drawer.stockSearch.query || '').trim();
        if (q.length < 1) {
            this.stocknote.drawer.stockSearch.results = [];
            return;
        }
        this.stocknote.drawer.stockSearch.loading = true;
        try {
            this.stocknote.drawer.stockSearch.results = await API.searchStocks(q) || [];
        } catch (e) {
            this.stocknote.drawer.stockSearch.results = [];
        } finally {
            this.stocknote.drawer.stockSearch.loading = false;
        }
    },

    selectStocknoteStock(stock) {
        const form = this.stocknote.drawer.form;
        form.stockCode = stock.stockCode;
        form.stockName = stock.stockName || '';
        form.marketType = stock.marketType || '';
        form.exchangeCode = stock.exchangeCode || '';
        this.stocknote.drawer.stockSearch.query = '';
        this.stocknote.drawer.stockSearch.results = [];
        this.persistStocknoteDraft();
    },

    clearStocknoteStockSelection() {
        const form = this.stocknote.drawer.form;
        form.stockCode = '';
        form.stockName = '';
        form.marketType = '';
        form.exchangeCode = '';
        this.persistStocknoteDraft();
    },

    // --- 태그 선택/입력 ------------------------------------------------------

    toggleStocknoteTag(listKey, value) {
        const arr = this.stocknote.drawer.form[listKey];
        const idx = arr.indexOf(value);
        if (idx >= 0) arr.splice(idx, 1);
        else arr.push(value);
        this.persistStocknoteDraft();
    },

    isStocknoteTagChecked(listKey, value) {
        return this.stocknote.drawer.form[listKey].indexOf(value) >= 0;
    },

    async onStocknoteCustomTagInput() {
        const prefix = (this.stocknote.drawer.tagInput || '').trim();
        if (prefix.length < 1) {
            this.stocknote.drawer.tagSuggestions = [];
            return;
        }
        try {
            const res = await API.getStockNoteCustomTags(prefix, 10);
            this.stocknote.drawer.tagSuggestions = (res.items || []).map(i => i.tagValue);
        } catch (_) {
            this.stocknote.drawer.tagSuggestions = [];
        }
    },

    addStocknoteCustomTag(value) {
        const raw = (value == null ? this.stocknote.drawer.tagInput : value).trim();
        if (!raw) return;
        const form = this.stocknote.drawer.form;
        if (form.customTags.length >= 20) {
            this.stocknote.drawer.errors = ['자유 태그는 최대 20개까지 추가할 수 있습니다.'];
            return;
        }
        if (form.customTags.indexOf(raw) < 0) {
            form.customTags.push(raw);
        }
        this.stocknote.drawer.tagInput = '';
        this.stocknote.drawer.tagSuggestions = [];
        this.persistStocknoteDraft();
    },

    removeStocknoteCustomTag(index) {
        this.stocknote.drawer.form.customTags.splice(index, 1);
        this.persistStocknoteDraft();
    },

    // --- validation + submit -------------------------------------------------

    validateStocknoteForm() {
        const form = this.stocknote.drawer.form;
        const errors = [];
        if (!form.stockCode) errors.push('종목을 선택해주세요.');
        if (!form.marketType) errors.push('marketType 이 누락되었습니다.');
        if (!form.exchangeCode) errors.push('exchangeCode 가 누락되었습니다.');
        if (!form.noteDate) errors.push('기록 날짜를 입력해주세요.');
        if (form.noteDate && form.noteDate > new Date().toISOString().slice(0, 10)) {
            errors.push('미래 날짜는 기록할 수 없습니다.');
        }
        if (!form.direction) errors.push('등락 방향을 선택해주세요.');
        if (!form.initialJudgment) errors.push('내 판단을 선택해주세요.');
        if ((form.triggerText || '').length > 4000) errors.push('직접 트리거 텍스트가 4000자를 넘을 수 없습니다.');
        if ((form.interpretationText || '').length > 4000) errors.push('시장 해석 텍스트가 4000자를 넘을 수 없습니다.');
        if ((form.riskText || '').length > 4000) errors.push('반대 논리 텍스트가 4000자를 넘을 수 없습니다.');
        this.stocknote.drawer.errors = errors;
        return errors.length === 0;
    },

    async submitStocknoteNote() {
        if (!this.validateStocknoteForm()) return;
        const form = this.stocknote.drawer.form;
        const tags = [
            ...form.triggerTypes.map(v => ({ source: 'TRIGGER', value: v })),
            ...form.characters.map(v => ({ source: 'CHARACTER', value: v })),
            ...form.supplyActors.map(v => ({ source: 'SUPPLY', value: v })),
            ...form.customTags.map(v => ({ source: 'CUSTOM', value: v }))
        ];
        const body = {
            stockCode: form.stockCode,
            marketType: form.marketType,
            exchangeCode: form.exchangeCode,
            direction: form.direction,
            changePercent: form.changePercent === '' ? null : Number(form.changePercent),
            noteDate: form.noteDate,
            triggerText: form.triggerText || null,
            interpretationText: form.interpretationText || null,
            riskText: form.riskText || null,
            preReflected: !!form.preReflected,
            initialJudgment: form.initialJudgment,
            tags: tags,
            per: form.per === '' ? null : Number(form.per),
            pbr: form.pbr === '' ? null : Number(form.pbr),
            evEbitda: form.evEbitda === '' ? null : Number(form.evEbitda),
            vsAverage: form.vsAverage || null,
            revenueImpact: form.revenueImpact || null,
            profitImpact: form.profitImpact || null,
            cashflowImpact: form.cashflowImpact || null,
            oneTime: !!form.oneTime,
            structural: !!form.structural
        };
        this.stocknote.drawer.saving = true;
        try {
            await API.createStockNote(body);
            this.stocknote.drawer.saving = false;
            this.stocknote.drawerOpen = false;
            this.resetStocknoteDrawer();
            await Promise.all([this.loadStocknoteDashboard(), this.loadStocknoteList()]);
        } catch (e) {
            this.stocknote.drawer.saving = false;
            const msg = (e && e.message) ? e.message : '저장 실패';
            this.stocknote.drawer.errors = [msg];
            console.error('stocknote create 실패:', e);
        }
    },

    // =========================================================================
    // 검증 (Phase 8 에서 연동 강화)
    // =========================================================================

    initStocknoteVerificationForm() {
        const v = this.stocknote.detail && this.stocknote.detail.verification;
        this.stocknote.verificationForm = {
            judgmentResult: v ? v.judgmentResult : 'CORRECT',
            verificationNote: v ? (v.verificationNote || '') : '',
            saving: false,
            error: ''
        };
    },

    async submitStocknoteVerification() {
        if (!this.stocknote.detail) return;
        const noteId = this.stocknote.detail.note.id;
        const form = this.stocknote.verificationForm;
        form.saving = true;
        form.error = '';
        try {
            await API.upsertStockNoteVerification(noteId, {
                judgmentResult: form.judgmentResult,
                verificationNote: form.verificationNote || null
            });
            await Promise.all([
                this.openStocknoteDetail(noteId),
                this.loadStocknoteDashboard(),
                this.loadStocknoteList()
            ]);
            this.initStocknoteVerificationForm();
            await this.loadStocknoteSimilarPatterns(noteId);
        } catch (e) {
            form.error = (e && e.message) ? e.message : '검증 저장 실패';
            console.error('stocknote verification upsert 실패:', e);
        } finally {
            form.saving = false;
        }
    },

    async removeStocknoteVerification() {
        if (!this.stocknote.detail || !this.stocknote.detail.verification) return;
        const noteId = this.stocknote.detail.note.id;
        if (!confirm('검증을 삭제하면 본문 잠금이 해제됩니다. 진행할까요?')) return;
        try {
            await API.deleteStockNoteVerification(noteId);
            await Promise.all([
                this.openStocknoteDetail(noteId),
                this.loadStocknoteDashboard(),
                this.loadStocknoteList()
            ]);
            this.initStocknoteVerificationForm();
            await this.loadStocknoteSimilarPatterns(noteId);
        } catch (e) {
            console.error('stocknote verification delete 실패:', e);
            alert('검증 삭제 실패');
        }
    },

    async retryStocknoteSnapshot(snapshotType) {
        if (!this.stocknote.detail) return;
        const noteId = this.stocknote.detail.note.id;
        if (!confirm(`${snapshotType} 스냅샷을 다시 시도하시겠습니까?`)) return;
        try {
            this.stocknote.detail = await API.retryStockNoteSnapshot(noteId, snapshotType);
        } catch (e) {
            console.error('stocknote snapshot retry 실패:', e);
            alert('스냅샷 재시도 실패: ' + ((e && e.message) || '알 수 없는 오류'));
        }
    },

    async loadStocknoteSimilarPatterns(noteId, directionFilter = null) {
        this.stocknote.similar = null;
        this.stocknote.similarLoading = true;
        try {
            this.stocknote.similar = await API.getStockNoteSimilarPatterns(noteId, directionFilter);
        } catch (e) {
            console.error('stocknote similar-patterns 로드 실패:', e);
            this.stocknote.similar = null;
        } finally {
            this.stocknote.similarLoading = false;
        }
    },

    async renderStocknoteStockChart() {
        // Chart.js mixed chart (line + scatter). 일봉 연동 도달 시 자동 활성.
        const data = this.stocknote.stockChartData;
        const canvas = document.getElementById('stocknote-stock-chart');
        if (!canvas) return;
        const existing = _stocknoteChartRegistry.get('stock');
        if (existing) {
            try { existing.destroy(); } catch (_) { /* noop */ }
            _stocknoteChartRegistry.delete('stock');
        }
        if (!data || !data.prices || data.prices.length === 0) {
            return; // 일봉 없음 — 상단 안내 문구만 표시
        }
        const labels = data.prices.map(p => p.date);
        // noteDate 가 labels 에 정확히 없을 때(주말/공휴일/장중 작성 등) 가장 가까운 이전 영업일 인덱스에 매핑.
        const resolveNoteIndex = (noteDate) => {
            const exact = labels.indexOf(noteDate);
            if (exact !== -1) return exact;
            for (let i = labels.length - 1; i >= 0; i--) {
                if (labels[i] <= noteDate) return i;
            }
            return -1; // labels 시작일보다 이른 noteDate → 점 미표시
        };
        const lineData = data.prices.map(p => p.close);
        // mixed line+scatter + category 축 조합은 Chart.js 버전별 매칭 이슈가 있어 모든 dataset 을
        // labels 기반 sparse line 으로 통일한다 (scatter 효과는 showLine:false + pointRadius 로 표현).
        const upData = labels.map(() => null);
        const downData = labels.map(() => null);
        const noteMetaByIndex = {}; // {idx: {noteId, verified, summary, originalDate}}
        (data.notes || []).forEach(n => {
            if (n.priceAtNote == null) return;
            const idx = resolveNoteIndex(n.noteDate);
            if (idx < 0) return; // labels 범위 밖 노트는 차트에 표시 안 함 (잘못된 위치 매핑 방지)
            const meta = { noteId: n.noteId, verified: n.verified, summary: n.summary, originalDate: n.noteDate };
            if (n.direction === 'UP') {
                upData[idx] = n.priceAtNote;
            } else if (n.direction === 'DOWN') {
                downData[idx] = n.priceAtNote;
            }
            noteMetaByIndex[idx] = meta;
        });

        const self = this;
        const chart = new Chart(canvas.getContext('2d'), {
            type: 'line',
            data: {
                labels: labels,
                datasets: [
                    { label: '종가', data: lineData, borderColor: '#3b82f6',
                      borderWidth: 1.5, tension: 0, pointRadius: 0, pointHitRadius: 0 },
                    { label: '상승 기록', data: upData, showLine: false, spanGaps: false,
                      backgroundColor: '#10b981', borderColor: '#10b981',
                      pointStyle: 'triangle', pointRadius: 9, pointHoverRadius: 11, pointHitRadius: 12 },
                    { label: '하락 기록', data: downData, showLine: false, spanGaps: false,
                      backgroundColor: '#ef4444', borderColor: '#ef4444',
                      pointStyle: 'triangle', rotation: 180, pointRadius: 9, pointHoverRadius: 11, pointHitRadius: 12 }
                ]
            },
            options: {
                animation: false,
                responsive: true,
                maintainAspectRatio: false,
                scales: {
                    x: { type: 'category', display: false },
                    y: { position: 'right' }
                },
                onClick: (evt, elements) => {
                    if (!elements.length) return;
                    const { datasetIndex, index } = elements[0];
                    if (datasetIndex === 0) return; // 종가 라인은 클릭 무시
                    const meta = noteMetaByIndex[index];
                    if (meta && meta.noteId) self.openStocknoteDetail(meta.noteId);
                },
                plugins: {
                    tooltip: {
                        callbacks: {
                            title: (items) => {
                                if (!items.length) return '';
                                const di = items[0].datasetIndex;
                                const idx = items[0].dataIndex;
                                if (di > 0 && noteMetaByIndex[idx]) {
                                    return `${noteMetaByIndex[idx].originalDate} (기록일)`;
                                }
                                return labels[idx];
                            },
                            label: (ctx) => {
                                if (ctx.datasetIndex === 0) return `종가: ${ctx.parsed.y}`;
                                const meta = noteMetaByIndex[ctx.dataIndex];
                                if (meta && meta.summary) {
                                    return `[${meta.verified ? '검증' : '미검증'}] ${meta.summary}`;
                                }
                                return `기록 ${meta && meta.verified ? '(검증)' : '(미검증)'}`;
                            }
                        }
                    }
                }
            }
        });
        _stocknoteChartRegistry.set('stock', chart);
    },

    // =========================================================================
    // Chart.js 인스턴스 정리 (Phase 8 에서 실제 렌더 구현 시 채움)
    // =========================================================================

    destroyStocknoteCharts() {
        _stocknoteChartRegistry.forEach(chart => {
            try { chart.destroy(); } catch (_) { /* noop */ }
        });
        _stocknoteChartRegistry.clear();
    },

    // =========================================================================
    // 유틸
    // =========================================================================

    stocknoteTagLabel(tag) {
        // 고정 enum 라벨 매핑 (간이). 자유 태그는 값 그대로.
        const FIXED_LABELS = {
            // RiseCharacter
            FUNDAMENTAL: '실적형', EXPECTATION: '기대형', SUPPLY_DEMAND: '수급형',
            THEME: '테마형', REVALUATION: '리레이팅형',
            // TriggerType
            DISCLOSURE: '공시', EARNINGS: '실적', NEWS: '뉴스', POLICY: '정책',
            INDUSTRY: '업황', SUPPLY: '수급', ETC: '기타',
            // SupplyActor
            FOREIGN: '외국인', INSTITUTION: '기관', RETAIL: '개인',
            SHORT_COVERING: '숏커버링', ETF_FLOW: 'ETF 수급'
        };
        if (tag.source === 'CUSTOM') return tag.value;
        return FIXED_LABELS[tag.value] || tag.value;
    },

    stocknoteDirectionLabel(dir) {
        return dir === 'UP' ? '▲ 상승' : (dir === 'DOWN' ? '▼ 하락' : dir);
    },

    stocknoteJudgmentLabel(j) {
        const MAP = { MORE_UPSIDE: '추가 상승', NEUTRAL: '중립', OVERHEATED: '과열', CATALYST_EXHAUSTED: '재료 소멸' };
        return MAP[j] || j;
    },

    stocknoteVerificationLabel(r) {
        const MAP = { CORRECT: '적중', WRONG: '오판', PARTIAL: '부분 적중' };
        return MAP[r] || '미검증';
    }
};