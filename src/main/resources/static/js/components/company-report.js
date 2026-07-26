/** Company Report - 기업분석리포트 (등록/조회). 7단계 위저드 작성 + 임시저장(draft), 정량 스냅샷 자동 산출 */
const CompanyReportComponent = {
    companyReport: {
        view: 'list',            // list | form(위저드) | detail

        // ==== 목록 ====
        loading: false,
        error: null,
        items: [],
        page: 0,
        size: 20,
        totalCount: 0,
        filterStockName: '',

        // ==== 위저드 ====
        step: 1,                 // 1~7
        isDraftFlow: true,       // true: 신규/임시저장 이어쓰기(임시저장 버튼), false: 완성 리포트 수정(저장 버튼)
        draftNotice: '',         // "임시저장됨" 안내 (수 초 후 사라짐)
        _draftNoticeTimer: null,

        // ==== 종목 검색 (1단계) ====
        searchQuery: '',
        searchResults: [],
        searchLoading: false,
        _searchGen: 0,
        selected: null,          // { stockCode, stockName }

        // ==== 자동 산출 데이터 (preview API 또는 draft 스냅샷) ====
        preview: null,           // { snapshot, valuation }
        previewLoading: false,
        previewError: null,
        _previewGen: 0,
        _previewChartRendered: false,

        // ==== 폼 ====
        mode: 'create',          // create | edit
        editingId: null,
        saving: false,
        formError: null,
        form: {
            manual: {
                history: [], philosophyNote: '',
                customers: [], suppliers: [], partnerAssessment: '',
                competitors: [], performanceNote: '',
                financialChanges: [],
                shareholderEvents: [], shareholderNote: '',
                judgmentComment: '',
                revenueForecasts: [], amountUnit: '억',
                metricInputs: { price: '', shares: '', netIncome: '', equity: '', revenue: '', operatingCf: '', eps: '', bps: '' }
            },
            grades: {
                assetUndervalue: '', earningsUndervalue: '', financialHealth: '',
                profitability: '', growth: '', businessCompetence: '', shareholderPolicy: ''
            },
            // 화면 입력은 % 단위, 전송 시 소수로 변환
            params: {
                discountRate: '10', optimisticGrowthRate: '20', optimisticGrowthYears: 5,
                ratios: {
                    cash: '100', securities: '100', receivables: '85', inventory: '50',
                    investments: '50', tangible: '50', intangible: '0', otherCurrent: '0'
                }
            }
        },

        // ==== 상세 ====
        detail: null,
        detailLoading: false,
        detailError: null,
        refreshing: false,

        // ==== DART 정기보고서 바로가기 (최근 10년) ====
        disclosures: { open: false, loading: false, error: null, stockCode: null, items: [] },
        _disclosureGen: 0,

        // ==== 월봉 주가 히스토리 (위저드 5단계 + 상세 주가지표) — 상장 이후 전구간, 실시간 조회 ====
        priceHistory: { stockCode: '', points: [], loading: false, error: null, _gen: 0 },

        _charts: [],

        // 정적 설정
        wizardSteps: [
            { n: 1, label: '종목 선택' },
            { n: 2, label: '회사 개요' },
            { n: 3, label: '실적 추이' },
            { n: 4, label: '재무제표·지표' },
            { n: 5, label: '기업가치' },
            { n: 6, label: '주주·위험' },
            { n: 7, label: '투자판단' }
        ],
        gradeItems: [
            { key: 'assetUndervalue', label: '자산으로 본 저평가' },
            { key: 'earningsUndervalue', label: '수익 창출 능력으로 본 저평가' },
            { key: 'financialHealth', label: '재무건전성' },
            { key: 'profitability', label: '수익성' },
            { key: 'growth', label: '성장성' },
            { key: 'businessCompetence', label: '사업역량' },
            { key: 'shareholderPolicy', label: '주주 중시 자세' }
        ],
        riskItems: [
            { key: 'currentLiabilitiesExceedCurrentAssets', label: '유동부채 > 유동자산' },
            { key: 'lowEquityRatio', label: '자기자본비율 20% 이하' },
            { key: 'negativeEquity', label: '순자산 마이너스(채무초과)' },
            { key: 'receivablesSurge', label: '매출채권 급증' },
            { key: 'inventorySurge', label: '재고자산 급증' },
            { key: 'recentCapitalIncrease', label: '최근 5년 유상증자 이력' }
        ]
    },

    // ==================== 목록 ====================
    async companyReportLoad() {
        const cr = this.companyReport;
        if (cr.loading) return;
        cr.loading = true;
        cr.error = null;
        try {
            const res = await API.getCompanyReports({
                stockName: cr.filterStockName.trim() || undefined, page: cr.page, size: cr.size
            });
            cr.items = res?.items || [];
            cr.totalCount = res?.totalCount ?? 0;
        } catch (e) {
            cr.error = e?.message || '리포트 목록 조회에 실패했습니다.';
        } finally {
            cr.loading = false;
        }
    },

    companyReportTotalPages() {
        const cr = this.companyReport;
        return Math.max(1, Math.ceil(cr.totalCount / cr.size));
    },

    async companyReportGoPage(delta) {
        const cr = this.companyReport;
        const next = cr.page + delta;
        if (next < 0 || next >= this.companyReportTotalPages()) return;
        cr.page = next;
        await this.companyReportLoad();
    },

    // 목록 클릭: 작성중이면 위저드 재개, 완성이면 상세
    async companyReportOpenItem(item) {
        if (item.draft) {
            await this.companyReportResumeDraft(item.id);
        } else {
            await this.companyReportOpenDetail(item.id);
        }
    },

    companyReportBackToList() {
        this._crDestroyCharts();
        this.companyReport.view = 'list';
        this.companyReport.detail = null;
        this.companyReportLoad();
    },

    // 위저드 나가기 (임시저장 안 된 변경분 유실 안내)
    companyReportExitWizard() {
        if (!confirm('작성 화면을 나갈까요? 임시저장하지 않은 변경 내용은 사라집니다.')) return;
        this.companyReportBackToList();
    },

    // ==================== 페이지 재진입 ====================
    async companyReportOnEnter() {
        const cr = this.companyReport;
        if (cr.view === 'detail' && cr.detail?.snapshot) {
            this.$nextTick(() => this._crRenderPerfChart(cr.detail.snapshot, 'report-detail-perf', cr.detail.manual));
            this._crLoadPriceHistory(cr.detail.stockCode);
            return;
        }
        if (cr.view === 'form') {
            cr._previewChartRendered = false;
            this._crRenderStepChart();
            return;
        }
        await this.companyReportLoad();
    },

    // ==================== 종목 검색 (1단계) ====================
    async companyReportSearch() {
        const cr = this.companyReport;
        const q = cr.searchQuery.trim();
        if (!q) return;
        const gen = ++cr._searchGen;
        cr.searchLoading = true;
        try {
            const results = await API.searchStocks(q) || [];
            if (gen !== cr._searchGen) return;
            cr.searchResults = results.filter(r => r.exchangeCode === 'KRX'); // 국내(KRX)만
        } catch (e) {
            if (gen !== cr._searchGen) return;
            console.error('종목 검색 실패:', e);
            cr.searchResults = [];
        } finally {
            if (gen === cr._searchGen) cr.searchLoading = false;
        }
    },

    async companyReportSelectStock(stock) {
        const cr = this.companyReport;
        cr.selected = stock;
        cr.searchResults = [];
        cr.searchQuery = '';
        this.companyReportLoadDisclosures(stock.stockCode); // preview와 병렬(가벼운 호출)
        await this.companyReportLoadPreview();
    },

    // ==================== 자동 산출 데이터 로드 ====================
    // 로딩 중 다른 종목을 선택해도 마지막 선택만 반영 (세대 카운터 레이스 가드)
    async companyReportLoadPreview() {
        const cr = this.companyReport;
        if (!cr.selected) return;
        const gen = ++cr._previewGen;
        cr.previewLoading = true;
        cr.previewError = null;
        cr.preview = null;
        this._crDestroyCharts();
        try {
            const result = await API.previewCompanyReport(cr.selected.stockCode);
            if (gen !== cr._previewGen) return;
            cr.preview = result;
            this._crRenderStepChart();
        } catch (e) {
            if (gen !== cr._previewGen) return;
            cr.previewError = e?.message || '자동 산출 데이터를 불러오지 못했습니다.';
        } finally {
            if (gen === cr._previewGen) cr.previewLoading = false;
        }
    },

    // ==================== DART 정기보고서 바로가기 (최근 10년) ====================
    // preview(최대 1분)와 별개의 가벼운 호출. 정기공시(A)만 조회 후 사업/반기/분기만 필터.
    _crResetDisclosures() {
        const cr = this.companyReport;
        cr._disclosureGen++; // 진행 중이던 이전 로드 무효화 (리셋 후 stale 결과 유입 방지)
        const d = cr.disclosures;
        d.loading = false; d.error = null; d.stockCode = null; d.items = [];
    },

    async companyReportLoadDisclosures(stockCode) {
        const cr = this.companyReport;
        if (!stockCode) return;
        if (cr.disclosures.stockCode === stockCode && cr.disclosures.items.length > 0) return; // 같은 종목 재로드 방지
        const gen = ++cr._disclosureGen;
        const d = cr.disclosures;
        d.loading = true;
        d.error = null;
        d.stockCode = stockCode;
        d.items = [];
        try {
            const from = this._formatYmd(this._crYearsAgo(10));
            const to = this._formatYmd(new Date());
            const rows = await API.getDisclosures(stockCode, from, to, ['A']) || [];
            if (gen !== cr._disclosureGen) return;
            d.items = rows
                .map(r => ({
                    reportName: r.reportName, receiptDate: r.receiptDate, viewerUrl: r.viewerUrl,
                    kind: this._crReportKind(r.reportName), period: this._crReportPeriod(r.reportName),
                    // 정정은 remark 또는 보고서명 '[기재정정]' 등 접두 양쪽에서 감지
                    correction: this.isCorrectionDisclosure(r.remark) || (r.reportName || '').indexOf('정정') !== -1
                }))
                .filter(r => r.kind);
        } catch (e) {
            if (gen !== cr._disclosureGen) return;
            console.error('정기보고서 조회 실패:', e);
            d.error = '정기보고서 목록을 불러오지 못했습니다.';
        } finally {
            if (gen === cr._disclosureGen) d.loading = false;
        }
    },

    _crYearsAgo(years) {
        const d = new Date();
        d.setFullYear(d.getFullYear() - years);
        return d;
    },

    // 보고서명 → 종류 (사업/반기/분기), 그 외는 null(제외)
    _crReportKind(name) {
        const n = name || '';
        if (n.indexOf('사업보고서') !== -1) return '사업';
        if (n.indexOf('반기보고서') !== -1) return '반기';
        if (n.indexOf('분기보고서') !== -1) return '분기';
        return null;
    },

    // 보고서명의 대상 기간 "(YYYY.MM)" 추출 → {year, month}. 없으면 null
    _crReportPeriod(name) {
        const m = /\((\d{4})\.(\d{2})/.exec(name || '');
        return m ? { year: m[1], month: m[2] } : null;
    },

    // 연도별 그룹(최신 연도 우선), 연도 내는 기간(월) 오름차순
    crDisclosureByYear() {
        const items = this.companyReport.disclosures.items || [];
        const map = {};
        items.forEach(r => {
            const year = (r.period && r.period.year) || (r.receiptDate ? r.receiptDate.slice(0, 4) : '—');
            (map[year] = map[year] || []).push(r);
        });
        return Object.keys(map).sort((a, b) => b.localeCompare(a)).map(year => ({
            year,
            reports: map[year].sort((a, b) => {
                const ma = (a.period && a.period.month) || '99';
                const mb = (b.period && b.period.month) || '99';
                return ma !== mb ? ma.localeCompare(mb) : (a.receiptDate || '').localeCompare(b.receiptDate || '');
            })
        }));
    },

    // 링크 라벨: 분기는 1Q/3Q 구분해 표시
    crReportKindLabel(r) {
        if (r.kind === '분기' && r.period) {
            if (r.period.month === '03') return '분기 1Q';
            if (r.period.month === '09') return '분기 3Q';
        }
        return r.kind;
    },

    crReportKindClass(kind) {
        if (kind === '사업') return 'bg-indigo-50 text-indigo-700 border-indigo-200';
        if (kind === '반기') return 'bg-emerald-50 text-emerald-700 border-emerald-200';
        return 'bg-gray-50 text-gray-600 border-gray-200'; // 분기
    },

    // ==================== 위저드 이동 ====================
    companyReportGoStep(n) {
        const cr = this.companyReport;
        if (n < 1 || n > 7) return;
        if (n === 1 && cr.mode === 'edit') return;      // 수정/재개 시 종목 변경 불가
        if (n > 1 && !cr.selected) return;              // 종목 선택 전에는 이동 불가
        cr.step = n;
        this._crRenderStepChart();
    },

    companyReportNextStep() {
        this.companyReportGoStep(this.companyReport.step + 1);
    },

    companyReportPrevStep() {
        this.companyReportGoStep(this.companyReport.step - 1);
    },

    // 3단계(실적 추이) 진입 시에만 차트 렌더 (1회). 5단계(기업가치) 진입 시 월봉 주가 로드/렌더.
    _crRenderStepChart() {
        const cr = this.companyReport;
        if (cr.step === 5 && cr.selected) {
            this._crLoadPriceHistory(cr.selected.stockCode);
        }
        if (cr.step !== 3 || !cr.preview?.snapshot || cr._previewChartRendered) return;
        cr._previewChartRendered = true;
        this.$nextTick(() => this._crRenderPerfChart(cr.preview.snapshot, 'report-preview-perf', cr.form.manual));
    },

    // ==================== 월봉 주가 히스토리 (상장 이후 전구간) ====================
    // 같은 종목은 1회만 로드해 위저드/상세 양쪽 canvas에 재사용. 종목이 바뀌면 새로 로드.
    async _crLoadPriceHistory(stockCode) {
        const cr = this.companyReport;
        if (!stockCode) return;
        const ph = cr.priceHistory;
        if (ph.stockCode === stockCode && (ph.loading || ph.points.length > 0)) {
            this._crRenderPriceChartsForCurrentView(); // 이미 로드됨 → 렌더만
            return;
        }
        const gen = ++ph._gen;
        ph.stockCode = stockCode;
        ph.loading = true;
        ph.error = null;
        ph.points = [];
        try {
            const res = await API.getStockPriceHistory(stockCode, 'M');
            if (gen !== ph._gen) return;
            ph.points = res?.points || [];
            if (ph.points.length === 0) {
                ph.error = '주가 데이터가 없습니다.';
            }
        } catch (e) {
            if (gen !== ph._gen) return;
            ph.error = '주가 데이터를 불러오지 못했습니다.';
        } finally {
            if (gen === ph._gen) {
                ph.loading = false;
                this._crRenderPriceChartsForCurrentView();
            }
        }
    },

    _crRenderPriceChartsForCurrentView() {
        const cr = this.companyReport;
        if (cr.view === 'form' && cr.step === 5) {
            this.$nextTick(() => this._crRenderPriceHistoryChart('report-wizard-price-history'));
        } else if (cr.view === 'detail') {
            this.$nextTick(() => this._crRenderPriceHistoryChart('report-detail-price-history'));
        }
    },

    // 월봉 종가 라인 차트. 이미 그려진 canvas(Chart.getChart)는 재렌더하지 않는다.
    _crRenderPriceHistoryChart(canvasId) {
        const ph = this.companyReport.priceHistory;
        if (!ph.points.length || typeof Chart === 'undefined') return;
        const canvas = document.getElementById(canvasId);
        if (!canvas || Chart.getChart(canvas)) return;
        const labels = ph.points.map(p => (p.date || '').slice(0, 7)); // YYYY-MM
        const data = ph.points.map(p => {
            const n = parseFloat(p.close);
            return isNaN(n) ? null : n;
        });
        const chart = new Chart(canvas, {
            type: 'line',
            data: {
                labels,
                datasets: [{
                    label: '월봉 종가(원)', data,
                    borderColor: '#2563eb', backgroundColor: 'rgba(37,99,235,0.08)',
                    fill: true, pointRadius: 0, borderWidth: 1.5, tension: 0.1
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                interaction: { mode: 'index', intersect: false },
                plugins: { legend: { display: false } },
                scales: {
                    x: { ticks: { maxTicksLimit: 12, maxRotation: 0 } },
                    y: { title: { display: true, text: '원' } }
                }
            }
        });
        this.companyReport._charts.push(chart);
    },

    // ==================== 작성 시작 / 수정 / 재개 ====================
    companyReportOpenCreate() {
        const cr = this.companyReport;
        this._crDestroyCharts();
        cr.view = 'form';
        cr.mode = 'create';
        cr.isDraftFlow = true;
        cr.step = 1;
        cr.editingId = null;
        cr.selected = null;
        cr.preview = null;
        cr.previewError = null;
        cr.formError = null;
        cr.draftNotice = '';
        cr.searchQuery = '';
        cr.searchResults = [];
        this._crResetDisclosures();
        this._crResetForm();
    },

    // 완성 리포트 수정: 상세 → 위저드 2단계 (스냅샷은 상세 데이터 재사용)
    companyReportOpenEdit() {
        const cr = this.companyReport;
        const d = cr.detail;
        if (!d) return;
        this._crEnterWizardFrom(d, false, 2);
    },

    // 작성중 리포트 재개: 저장된 단계부터
    async companyReportResumeDraft(id) {
        const cr = this.companyReport;
        cr.error = null;
        try {
            const d = await API.getCompanyReport(id);
            this._crEnterWizardFrom(d, true, d.draftStep || 2);
        } catch (e) {
            cr.error = e?.message || '작성중 리포트를 불러오지 못했습니다.';
        }
    },

    _crEnterWizardFrom(detail, isDraftFlow, step) {
        const cr = this.companyReport;
        this._crDestroyCharts();
        cr.view = 'form';
        cr.mode = 'edit';
        cr.isDraftFlow = isDraftFlow;
        cr.editingId = detail.id;
        cr.selected = { stockCode: detail.stockCode, stockName: detail.stockName };
        cr.preview = detail.snapshot
            ? { snapshot: detail.snapshot, valuation: detail.valuation, suggestedGrades: detail.suggestedGrades }
            : null;
        cr.previewError = null;
        cr.formError = null;
        cr.draftNotice = '';
        this._crResetDisclosures();
        this.companyReportLoadDisclosures(detail.stockCode);
        this._crPopulateForm(detail);
        cr.step = step;
        this._crRenderStepChart();
    },

    // ==================== 저장 (임시저장 / 작성 완료) ====================
    async companyReportSaveDraft() {
        await this._crSave(true, this.companyReport.step);
    },

    async companyReportSaveFinal() {
        await this._crSave(false, null);
    },

    async _crSave(draft, draftStep) {
        const cr = this.companyReport;
        if (cr.mode === 'create' && !cr.selected) {
            cr.formError = '종목을 먼저 선택하세요.';
            return;
        }
        if (cr.saving) return;
        cr.saving = true;
        cr.formError = null;
        try {
            const body = this._crBuildBody(draft, draftStep);
            await this._crPersist(body, draft);
        } catch (e) {
            cr.formError = e?.message || '저장에 실패했습니다.';
        } finally {
            cr.saving = false;
        }
    },

    // 첫 임시저장은 생성(스냅샷 조립 포함, 최대 1분), 이후는 빠른 갱신. 완료 저장 시 상세로 이동.
    async _crPersist(body, draft) {
        const cr = this.companyReport;
        if (cr.editingId != null) {
            await API.updateCompanyReport(cr.editingId, body);
        } else {
            body.stockCode = cr.selected.stockCode;
            const res = await API.createCompanyReport(body);
            cr.editingId = res.id;
            cr.mode = 'edit';
        }
        if (draft) {
            this._crShowDraftNotice();
        } else {
            await this.companyReportOpenDetail(cr.editingId);
        }
    },

    _crShowDraftNotice() {
        const cr = this.companyReport;
        cr.draftNotice = '임시저장되었습니다 (' + new Date().toLocaleTimeString('ko-KR') + ')';
        if (cr._draftNoticeTimer) clearTimeout(cr._draftNoticeTimer);
        cr._draftNoticeTimer = setTimeout(() => { cr.draftNotice = ''; }, 4000);
    },

    // ==================== 상세 ====================
    async companyReportOpenDetail(id) {
        const cr = this.companyReport;
        cr.view = 'detail';
        cr.detailLoading = true;
        cr.detailError = null;
        this._crDestroyCharts();
        this._crResetDisclosures();
        try {
            cr.detail = await API.getCompanyReport(id);
            this.companyReportLoadDisclosures(cr.detail?.stockCode);
            this._crLoadPriceHistory(cr.detail?.stockCode);
            this.$nextTick(() => this._crRenderPerfChart(cr.detail?.snapshot, 'report-detail-perf', cr.detail?.manual));
        } catch (e) {
            cr.detailError = e?.message || '리포트 조회에 실패했습니다.';
        } finally {
            cr.detailLoading = false;
        }
    },

    async companyReportRefresh() {
        const cr = this.companyReport;
        if (!cr.detail || cr.refreshing) return;
        cr.refreshing = true;
        cr.detailError = null;
        try {
            const refreshed = await API.refreshCompanyReport(cr.detail.id);
            this._crDestroyCharts();
            cr.detail = refreshed;
            this._crLoadPriceHistory(cr.detail?.stockCode);
            this.$nextTick(() => this._crRenderPerfChart(cr.detail?.snapshot, 'report-detail-perf', cr.detail?.manual));
        } catch (e) {
            cr.detailError = e?.message || '데이터 새로고침에 실패했습니다.';
        } finally {
            cr.refreshing = false;
        }
    },

    async companyReportDelete(id) {
        if (!confirm('이 리포트를 삭제할까요?')) return;
        try {
            await API.deleteCompanyReport(id);
            this.companyReportBackToList();
        } catch (e) {
            alert(e?.message || '삭제에 실패했습니다.');
        }
    },

    // ==================== 폼 내부 헬퍼 ====================
    _crResetForm() {
        this.companyReport.form = {
            manual: {
                history: [], philosophyNote: '',
                customers: [], suppliers: [], partnerAssessment: '',
                competitors: [], performanceNote: '',
                financialChanges: [],
                shareholderEvents: [], shareholderNote: '',
                judgmentComment: '',
                revenueForecasts: [], amountUnit: '억',
                metricInputs: { price: '', shares: '', netIncome: '', equity: '', revenue: '', operatingCf: '', eps: '', bps: '' }
            },
            grades: {
                assetUndervalue: '', earningsUndervalue: '', financialHealth: '',
                profitability: '', growth: '', businessCompetence: '', shareholderPolicy: ''
            },
            params: {
                discountRate: '10', optimisticGrowthRate: '20', optimisticGrowthYears: 5,
                ratios: {
                    cash: '100', securities: '100', receivables: '85', inventory: '50',
                    investments: '50', tangible: '50', intangible: '0', otherCurrent: '0'
                }
            }
        };
        this._crSeedEmptyRows();
    },

    _crPopulateForm(detail) {
        this._crResetForm();
        const f = this.companyReport.form;
        const m = detail.manual || {};
        ['history', 'customers', 'suppliers', 'competitors', 'financialChanges', 'shareholderEvents', 'revenueForecasts']
            .forEach(key => { f.manual[key] = (m[key] || []).map(row => ({ ...row })); });
        ['philosophyNote', 'partnerAssessment', 'performanceNote', 'shareholderNote', 'judgmentComment']
            .forEach(key => { f.manual[key] = m[key] || ''; });
        f.manual.amountUnit = m.amountUnit || '억';
        Object.keys(f.manual.metricInputs)
            .forEach(key => { f.manual.metricInputs[key] = (m.metricInputs && m.metricInputs[key]) || ''; });
        const grades = detail.grades || {};
        this.companyReport.gradeItems.forEach(item => { f.grades[item.key] = grades[item.key] || ''; });
        const p = detail.valuationParams;
        if (!p) return;
        const toPct = v => v == null ? '' : String(parseFloat((v * 100).toFixed(4)));
        f.params.discountRate = toPct(p.discountRate);
        f.params.optimisticGrowthRate = toPct(p.optimisticGrowthRate);
        f.params.optimisticGrowthYears = p.optimisticGrowthYears ?? 5;
        const r = p.adjustmentRatios || {};
        Object.keys(f.params.ratios).forEach(key => { f.params.ratios[key] = toPct(r[key]); });
        this._crSeedEmptyRows();
    },

    _crBuildBody(draft, draftStep) {
        const f = this.companyReport.form;
        const note = v => (v && v.trim()) ? v.trim() : null;
        const grade = v => v || null;
        const pct = v => {
            const n = parseFloat(v);
            return isNaN(n) ? null : n / 100;
        };
        return {
            manual: this._crBuildManual(note),
            gradeAssetUndervalue: grade(f.grades.assetUndervalue),
            gradeEarningsUndervalue: grade(f.grades.earningsUndervalue),
            gradeFinancialHealth: grade(f.grades.financialHealth),
            gradeProfitability: grade(f.grades.profitability),
            gradeGrowth: grade(f.grades.growth),
            gradeBusinessCompetence: grade(f.grades.businessCompetence),
            gradeShareholderPolicy: grade(f.grades.shareholderPolicy),
            valuationParams: {
                discountRate: pct(f.params.discountRate),
                optimisticGrowthRate: pct(f.params.optimisticGrowthRate),
                optimisticGrowthYears: parseInt(f.params.optimisticGrowthYears, 10) || 5,
                adjustmentRatios: {
                    cash: pct(f.params.ratios.cash),
                    securities: pct(f.params.ratios.securities),
                    receivables: pct(f.params.ratios.receivables),
                    inventory: pct(f.params.ratios.inventory),
                    investments: pct(f.params.ratios.investments),
                    tangible: pct(f.params.ratios.tangible),
                    intangible: pct(f.params.ratios.intangible),
                    otherCurrent: pct(f.params.ratios.otherCurrent)
                }
            },
            draft: draft,
            draftStep: draftStep
        };
    },

    // 빈 행(모든 필드 공백)은 버리고 필드를 trim해서 전송
    _crBuildManual(note) {
        const m = this.companyReport.form.manual;
        const rows = list => (list || [])
            .map(row => Object.fromEntries(Object.entries(row).map(([k, v]) => [k, (v || '').trim()])))
            .filter(row => Object.values(row).some(v => v !== ''));
        return {
            schemaVersion: 2,
            history: rows(m.history),
            philosophyNote: note(m.philosophyNote),
            customers: rows(m.customers),
            suppliers: rows(m.suppliers),
            partnerAssessment: note(m.partnerAssessment),
            competitors: rows(m.competitors),
            performanceNote: note(m.performanceNote),
            financialChanges: rows(m.financialChanges),
            shareholderEvents: rows(m.shareholderEvents),
            shareholderNote: note(m.shareholderNote),
            judgmentComment: note(m.judgmentComment),
            revenueForecasts: rows(m.revenueForecasts),
            amountUnit: m.amountUnit || '억',
            metricInputs: {
                price: note(m.metricInputs.price), shares: note(m.metricInputs.shares),
                netIncome: note(m.metricInputs.netIncome), equity: note(m.metricInputs.equity),
                revenue: note(m.metricInputs.revenue), operatingCf: note(m.metricInputs.operatingCf),
                eps: note(m.metricInputs.eps), bps: note(m.metricInputs.bps)
            }
        };
    },

    // ==================== 구조화 입력 행 편집 ====================
    _crNewRow(list) {
        const templates = {
            history: { year: '', content: '' },
            customers: { name: '', item: '', share: '', note: '' },
            suppliers: { name: '', item: '', share: '', note: '' },
            competitors: { name: '', segment: '', note: '' },
            financialChanges: { year: '', item: '', reason: '' },
            shareholderEvents: { period: '', content: '' },
            revenueForecasts: { year: '', q1: '', q2: '', q3: '', q4: '', ni1: '', ni2: '', ni3: '', ni4: '' }
        };
        return templates[list] ? { ...templates[list] } : null;
    },

    // 각 편집기에 기본 빈 행 1개를 미리 보여준다 (빈 행은 저장 시 자동 제외)
    _crSeedEmptyRows() {
        const manual = this.companyReport.form.manual;
        ['history', 'customers', 'suppliers', 'competitors', 'financialChanges', 'shareholderEvents', 'revenueForecasts']
            .forEach(list => {
                if (manual[list].length === 0) manual[list].push(this._crNewRow(list));
            });
    },

    companyReportAddRow(list) {
        const row = this._crNewRow(list);
        if (row) this.companyReport.form.manual[list].push(row);
    },

    companyReportRemoveRow(list, idx) {
        this.companyReport.form.manual[list].splice(idx, 1);
    },

    // 연혁 날짜(YYYY 또는 YYYY.MM) 오름차순 정렬 (조회 표시용) — 월 자릿수 무관 숫자 정렬
    crHistoryAsc(manual) {
        const rows = manual?.history || [];
        const sortKey = h => {
            const [year, month] = String(h?.year || '').split('.');
            return (parseInt(year, 10) || 0) * 100 + (parseInt(month, 10) || 0);
        };
        return [...rows].sort((a, b) => sortKey(a) - sortKey(b));
    },

    // ==================== 차트 ====================
    // 실적 추이: 매출/영업이익/순이익(억원, bar) + 영업이익률/순이익률(%, line, 우축)
    _crRenderPerfChart(snapshot, canvasId, manual) {
        if (!snapshot || !Array.isArray(snapshot.performance) || typeof Chart === 'undefined') return;
        const canvas = document.getElementById(canvasId);
        if (!canvas) return;
        const columns = snapshot.columns || [];
        const forecasts = this._crForecastSeries(manual);
        const labels = columns.map(c => c.year + (c.partial ? '*' : '')).concat(forecasts.map(f => f.year + 'E'));
        const pad = arr => arr.concat(forecasts.map(() => null));
        const amount = key => pad(this._crSeries(snapshot.performance, key, columns, v => v / 1e8));
        const ratio = key => pad(this._crSeries(snapshot.performance, key, columns, v => v));
        const forecastData = columns.map(() => null).concat(forecasts.map(f => f.annual / 1e8));
        const chart = new Chart(canvas, {
            data: {
                labels,
                datasets: [
                    { type: 'bar', label: '매출액(억)', data: amount('revenue'), backgroundColor: 'rgba(59,130,246,0.65)', yAxisID: 'y' },
                    { type: 'bar', label: '예상 매출(억)', data: forecastData, backgroundColor: 'rgba(59,130,246,0.2)', borderColor: 'rgba(59,130,246,0.8)', borderWidth: 1, yAxisID: 'y' },
                    { type: 'bar', label: '영업이익(억)', data: amount('operatingProfit'), backgroundColor: 'rgba(16,185,129,0.65)', yAxisID: 'y' },
                    { type: 'bar', label: '당기순이익(억)', data: amount('netIncome'), backgroundColor: 'rgba(245,158,11,0.65)', yAxisID: 'y' },
                    { type: 'line', label: '영업이익률(%)', data: ratio('operatingMargin'), borderColor: '#dc2626', backgroundColor: '#dc2626', yAxisID: 'y1', tension: 0.2 },
                    { type: 'line', label: '순이익률(%)', data: ratio('netMargin'), borderColor: '#7c3aed', backgroundColor: '#7c3aed', yAxisID: 'y1', tension: 0.2 }
                ]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                interaction: { mode: 'index', intersect: false },
                scales: {
                    y: { position: 'left', title: { display: true, text: '억원' } },
                    y1: { position: 'right', grid: { drawOnChartArea: false }, title: { display: true, text: '%' } }
                }
            }
        });
        this.companyReport._charts.push(chart);
    },

    _crSeries(rows, key, columns, transform) {
        const row = (rows || []).find(r => r.key === key);
        return columns.map(c => {
            const raw = row && row.values ? row.values[c.year] : null;
            const num = raw == null ? NaN : parseFloat(raw);
            return isNaN(num) ? null : transform(num);
        });
    },

    _crDestroyCharts() {
        this.companyReport._charts.forEach(chart => {
            try { chart.destroy(); } catch (e) { /* ignore */ }
        });
        this.companyReport._charts = [];
        this.companyReport._previewChartRendered = false;
    },

    // 주가지표 카드 툴팁 (계산식 + 출처 + 결손 사유)
    crMetricHelp: {
        marketCap: '시가총액(근사) = 현재가 × 보통주 유통주식수\n유통주식수는 DART 최신 사업보고서의 주식총수 기준입니다.',
        eps: 'EPS(주당순이익) = 당기순이익 ÷ 유통주식수\n최신 사업보고서 기준. DART에서 당기순이익을 찾지 못하면 "—"로 표시됩니다.',
        bps: 'BPS(주당순자산) = 자본총계 ÷ 유통주식수\n최신 사업보고서 기준.',
        per: 'PER = 현재가 ÷ EPS\n이익 대비 주가 수준 — 낮을수록 이익 대비 저평가. EPS가 "—"면 함께 계산되지 않습니다(계산기에서 EPS를 직접 입력하면 내 계산으로 산출 가능).',
        pbr: 'PBR = 현재가 ÷ BPS\n1배 미만이면 시가총액이 장부상 순자산보다 낮다는 뜻입니다.',
        psr: 'PSR = 시가총액 ÷ 매출액(최신 사업연도)\n매출 대비 주가 수준.',
        pcfr: 'PCFR = 시가총액 ÷ 영업활동현금흐름(최신 사업연도)\n현금창출력 대비 주가 수준.',
        perTimesPbr: 'PER × PBR\n벤저민 그레이엄 기준 — 곱이 8 이하면 저평가 참고 신호. PER가 "—"면 함께 계산되지 않습니다.',
        evEbitda: 'EV/EBITDA(근사) = (시가총액 + 차입금 − 현금성·단기금융) ÷ (영업이익 + 상각비)\n현금흐름표에 상각비 행이 없는 종목은 "—"로 표시됩니다.',
        roic: 'ROIC(근사) = 영업이익 × (1 − 실효세율) ÷ (자본총계 + 차입금 − 현금성)\n실효세율은 법인세 ÷ 세전이익으로 추정(이상치면 25% 대체).',
        accrual: '발생액/총자산 = (당기순이익 − 영업활동현금흐름) ÷ 자산총계\n플러스로 크면 장부 이익 대비 현금이 못 따라온다는 뜻(이익의 질 주의). 마이너스면 현금흐름이 이익보다 좋다는 신호.'
    },

    // 주가지표 툴팁 팝오버 상태 (hover 미리보기 / 클릭 고정)
    crHint: { show: false, text: '', x: 0, y: 0, pinned: false },

    // 지표 key → 결과값 단위 (amount:조/억, price:주당 원, x:배, pct:%)
    crMetricUnit: {
        marketCap: 'amount', eps: 'price', bps: 'price', per: 'x', pbr: 'x', psr: 'x',
        pcfr: 'x', perTimesPbr: 'x', evEbitda: 'x', roic: 'pct', accrual: 'pct'
    },

    // 근거 항 값 포맷 (단위별)
    _crBdVal(value, unit) {
        if (value == null) return '—';
        const n = parseFloat(value);
        if (isNaN(n)) return '—';
        if (unit === 'x') return Format.number(n, 2) + '배';
        if (unit === 'pct') return Format.number(n, 2) + '%';
        if (unit === 'price') return Format.number(n, 0) + '원';
        if (unit === 'shares') return Format.compactNumber(n) + '주';
        return Format.compactNumber(n); // amount
    },

    // 현재 활성 스냅샷(작성=preview / 상세=detail)의 계산 근거를 식으로 조립. 없으면 '' (공식만 표시)
    crBreakdownText(key, showBreakdown) {
        if (showBreakdown === false) return '';
        const snap = this.companyReport.view === 'detail'
            ? this.companyReport.detail?.snapshot : this.companyReport.preview?.snapshot;
        const bd = snap?.priceMetrics?.breakdowns?.[key];
        if (!bd || !(bd.terms || []).length) return '';
        const eq = bd.terms.map(t => (t.op ? t.op + ' ' : '') + t.label + ' ' + this._crBdVal(t.value, t.unit)).join(' ');
        let out = '\n\n= ' + eq + '\n= ' + this._crBdVal(bd.result, this.crMetricUnit[key]);
        if ((bd.extras || []).length) {
            out += '\n(' + bd.extras.map(t => t.label + ' ' + this._crBdVal(t.value, t.unit)).join(' · ') + ')';
        }
        return out;
    },

    _crHintText(key, showBreakdown) {
        return (this.crMetricHelp[key] || '') + this.crBreakdownText(key, showBreakdown);
    },

    crHintShow(event, key, showBreakdown) {
        const r = event.currentTarget.getBoundingClientRect();
        this.crHint.text = this._crHintText(key, showBreakdown);
        this.crHint.x = Math.max(8, Math.min(r.left, window.innerWidth - 300));
        this.crHint.y = r.bottom + 6;
        this.crHint.show = true;
    },

    crHintHide() {
        if (!this.crHint.pinned) this.crHint.show = false;
    },

    crHintToggle(event, key, showBreakdown) {
        const text = this._crHintText(key, showBreakdown);
        if (this.crHint.pinned && this.crHint.show && this.crHint.text === text) {
            this.crHint.pinned = false;
            this.crHint.show = false;
        } else {
            this.crHintShow(event, key, showBreakdown);
            this.crHint.pinned = true;
        }
    },

    crHintClose() {
        this.crHint.pinned = false;
        this.crHint.show = false;
    },

    // ==================== 표시 헬퍼 ====================
    crAmt(value) {
        // 조 단위는 2자리 버림(반올림 X) — 기업 리포트 표시용
        return (value == null || value === '') ? '—' : Format.compactNumber(value, true);
    },

    crNum(value, decimals = 2) {
        return (value == null || value === '') ? '—' : Format.number(value, decimals);
    },

    crPct(value) {
        return (value == null || value === '') ? '—' : Format.number(value, 2) + '%';
    },

    crCellAmt(row, year) {
        return this.crAmt(row && row.values ? row.values[year] : null);
    },

    crCellPct(row, year) {
        return this.crPct(row && row.values ? row.values[year] : null);
    },

    // 재무제표 요약을 BS / IS / CF 구역으로 분리 (statements의 key 접두사 기준)
    crStatementGroups: [
        { label: '재무상태표 (BS)', prefix: 'bs.' },
        { label: '손익계산서 (IS)', prefix: 'is.' },
        { label: '현금흐름표 (CF)', prefix: 'cf.' }
    ],

    crStatementRows(statements, prefix) {
        return (statements || []).filter(r => (r.key || '').startsWith(prefix));
    },

    crDate(value) {
        return value ? Format.date(value) : '—';
    },

    // 'YYYYMMDD' → 'YYYY-MM-DD'
    crRawDate(value) {
        if (!value || value.length !== 8) return value || '—';
        return value.slice(0, 4) + '-' + value.slice(4, 6) + '-' + value.slice(6, 8);
    },

    crJudgeBadge(judgment) {
        if (judgment === 'good') return 'bg-green-50 text-green-700';
        if (judgment === 'warn') return 'bg-amber-50 text-amber-700';
        if (judgment === 'risk') return 'bg-red-50 text-red-700';
        return 'bg-gray-50 text-gray-400';
    },

    crJudgeLabel(judgment) {
        if (judgment === 'good') return '양호';
        if (judgment === 'warn') return '주의';
        if (judgment === 'risk') return '위험';
        return '—';
    },

    crGradeBadge(grade) {
        if (grade === 'A') return 'bg-red-600 text-white';
        if (grade === 'B') return 'bg-orange-100 text-orange-700';
        if (grade === 'C') return 'bg-gray-100 text-gray-700';
        if (grade === 'D') return 'bg-blue-50 text-blue-700';
        if (grade === 'E') return 'bg-slate-100 text-slate-500';
        return 'bg-gray-50 text-gray-400';
    },

    // ==================== 제안 등급 (정량 5항목, 조회 시 파생 계산 — 작성=preview / 상세=detail) ====================
    crSuggestion(key) {
        const cr = this.companyReport;
        const source = cr.view === 'detail' ? cr.detail : cr.preview;
        return source?.suggestedGrades?.[key] || null;
    },

    crSuggestionBasis(key) {
        return (this.crSuggestion(key)?.basis || []).join(' · ');
    },

    crHasSuggestions() {
        return this.companyReport.gradeItems.some(item => this.crSuggestion(item.key));
    },

    crApplySuggestion(key) {
        const s = this.crSuggestion(key);
        if (s?.grade) this.companyReport.form.grades[key] = s.grade;
    },

    crApplyAllSuggestions() {
        this.companyReport.gradeItems.forEach(item => this.crApplySuggestion(item.key));
    },

    // ==================== 제안 사유 패널 (화살표 → 우측 슬라이드, #93) ====================
    crReason: { open: false, key: null },

    crReasonOpen(key) {
        this.crReason.key = key;
        this.crReason.open = true;
    },

    crReasonClose() {
        this.crReason.open = false;
    },

    crReasonLabel() {
        const item = this.companyReport.gradeItems.find(i => i.key === this.crReason.key);
        return item ? item.label : '';
    },

    _crReasonSource() {
        const cr = this.companyReport;
        return (cr.view === 'detail' ? cr.detail : cr.preview) || null;
    },

    // 판정 기준연도: 가치평가 입력의 최신 사업연도, 없으면 마지막 온기 컬럼
    crReasonBaseYear() {
        const snap = this._crReasonSource()?.snapshot;
        if (snap?.valuationInputs?.baseYear) return snap.valuationInputs.baseYear;
        const full = (snap?.columns || []).filter(c => !c.partial);
        return full.length ? full[full.length - 1].year : null;
    },

    // basis 문자열의 ' (good|warn|risk)' 마커 분리 → {text, judgment} (판정 색상 표시용)
    crReasonBasis() {
        return (this.crSuggestion(this.crReason.key)?.basis || []).map(line => {
            const m = line.match(/^(.*) \((good|warn|risk)\)$/);
            return m ? { text: m[1], judgment: m[2] } : { text: line, judgment: null };
        });
    },

    // 표시용 등급 기준표 — GradeSuggestionCalculator(밴드·규칙)와 SnapshotFinancialExtractor(판정 임계값)의 사본.
    // 백엔드 기준 변경 시 반드시 함께 수정한다.
    crGradeCriteria: {
        assetUndervalue: {
            rules: [
                { grade: 'A', label: '시총/청산가치 ≤ 1.0배 (청산가치 근거일 때만)' },
                { grade: 'B', label: '시총/청산가치 ≤ 1.5배 또는 PBR ≤ 0.6' },
                { grade: 'C', label: '시총/청산가치 ≤ 2.5배 또는 PBR ≤ 1.0' },
                { grade: 'D', label: '시총/청산가치 ≤ 4.0배 또는 PBR ≤ 2.0' },
                { grade: 'E', label: '두 밴드 모두 초과' }
            ],
            note: '두 신호 중 유리한 쪽 밴드를 취합니다.'
        },
        earningsUndervalue: {
            rules: [
                { grade: 'A', label: '시총/DCF보수 ≤ 1.0배' },
                { grade: 'B', label: '시총/DCF보수 ≤ 1.5배 또는 PER ≤ 8' },
                { grade: 'C', label: '시총/DCF낙관 ≤ 1.0배 또는 PER ≤ 15' },
                { grade: 'D', label: '시총/DCF낙관 ≤ 1.5배 또는 PER ≤ 25' },
                { grade: 'E', label: '모든 밴드 초과, 또는 FCF ≤ 0이고 PER 없음(수익 근거 없음)' }
            ],
            note: '두 신호 중 유리한 쪽 밴드를 취합니다.'
        },
        financialHealth: {
            rules: [
                { grade: 'A', label: 'B 조건 + 순현금 > 0 + 위험 시그널 전부 없음(유상증자 제외)' },
                { grade: 'B', label: '자기자본비율·유동비율·부채비율 모두 양호' },
                { grade: 'C', label: '주의 있음 (판정 불가는 주의로 봄)' },
                { grade: 'D', label: '위험 1개' },
                { grade: 'E', label: '위험 2개 이상 또는 채무초과' }
            ],
            note: '판정 기준 — 자기자본비율: 양호 ≥40%·위험 ≤20%, 유동비율: 100%, 부채비율: 양호 ≤200%·위험 >300%.'
        },
        profitability: {
            rules: [
                { grade: 'A', label: 'B 조건 + ROE ≥ 15%·영업이익률 ≥ 10%가 최근 3개 연도 지속(값 존재 연도 2개 이상)' },
                { grade: 'B', label: '영업이익률·ROE·ROA 모두 양호' },
                { grade: 'C', label: '주의 있음 (판정 불가는 주의로 봄)' },
                { grade: 'D', label: '위험 있음' },
                { grade: 'E', label: '영업이익률 ≤0%(영업적자, 0 포함) 또는 기준연도 ROE 음수(순적자)' }
            ],
            note: '판정 기준 — 영업이익률: 양호 ≥5%·위험 ≤0%, ROE: 양호 ≥10%·위험 ≤5%, ROA: 양호 ≥5%·위험 ≤0%.'
        },
        growth: {
            rules: [
                { grade: 'A', label: 'B 조건 + 최근 4개 연도 중 3개 연도 이상 둘 다 ≥ 10%' },
                { grade: 'B', label: '매출·영업이익 성장률 둘 다 ≥ 10%' },
                { grade: 'C', label: '둘 다 0 이상' },
                { grade: 'D', label: '한쪽이라도 음수' },
                { grade: 'E', label: '둘 다 음수가 2년 연속' }
            ],
            note: '기준연도 성장률 기준 — 전년 값이 없으면 제안을 생략합니다.'
        }
    },

    crReasonRules() {
        return this.crGradeCriteria[this.crReason.key]?.rules || [];
    },

    crReasonNote() {
        return this.crGradeCriteria[this.crReason.key]?.note || '';
    },

    // 항목별 계산식 빌더 테이블 — crReasonFormulas가 call(this)로 실행
    _crReasonFormulaBuilders: {
        assetUndervalue() {
            return [this._crReasonMetricFormula('pbr'), this._crReasonLiquidationFormula()];
        },
        earningsUndervalue() {
            return [this._crReasonMetricFormula('per'), this._crReasonDcfFormula()];
        },
        financialHealth() {
            return [
                this._crReasonRatioFormula('자기자본비율 = 자본총계 ÷ 자산총계', 'bs.totalEquity', 'bs.totalAssets', 'equityRatio'),
                this._crReasonRatioFormula('유동비율 = 유동자산 ÷ 유동부채', 'bs.currentAssets', 'bs.currentLiabilities', 'currentRatio'),
                this._crReasonRatioFormula('부채비율 = 부채총계 ÷ 자본총계', 'bs.totalLiabilities', 'bs.totalEquity', 'debtRatio'),
                this._crReasonNetCashFormula()
            ];
        },
        profitability() {
            return [
                this._crReasonRatioFormula('영업이익률 = 영업이익 ÷ 매출액', 'is.operatingProfit', 'is.revenue', 'operatingMargin'),
                this._crReasonRatioFormula('ROE = 당기순이익 ÷ 자본총계', 'is.netIncome', 'bs.totalEquity', 'roe'),
                this._crReasonRatioFormula('ROA = 당기순이익 ÷ 자산총계', 'is.netIncome', 'bs.totalAssets', 'roa')
            ];
        },
        growth() {
            return [
                this._crReasonGrowthFormula('매출 성장률 = (당년 − 전년) ÷ 전년', 'is.revenue', 'revenueGrowth'),
                this._crReasonGrowthFormula('영업이익 성장률 = (당년 − 전년) ÷ 전년', 'is.operatingProfit', 'operatingProfitGrowth')
            ];
        }
    },

    // 계산식 블록 목록 — 숫자는 응답 값을 그대로 나열한다 (프런트 재계산 없음, 반올림 불일치 방지)
    crReasonFormulas() {
        const build = this._crReasonFormulaBuilders[this.crReason.key];
        return build ? build.call(this).filter(Boolean) : [];
    },

    // 주가지표(PBR/PER): 툴팁 공식 첫 줄 + 백엔드 breakdown 항 나열 재사용
    _crReasonMetricFormula(metricKey) {
        const formula = (this.crMetricHelp[metricKey] || '').split('\n')[0];
        return formula ? formula + this.crBreakdownText(metricKey, true) : null;
    },

    _crReasonLiquidationFormula() {
        const liq = this._crReasonSource()?.valuation?.liquidation;
        if (!liq) return null;
        let out = '청산가치 = 조정자산 합계 − 총부채\n= ' + this.crAmt(liq.adjustedAssetTotal)
            + ' − ' + this.crAmt(liq.totalLiabilities) + ' = ' + this.crAmt(liq.liquidationValue);
        if (liq.marketCapToLiquidation != null) {
            out += '\n시총/청산가치 = ' + this.crAmt(liq.marketCap) + ' ÷ ' + this.crAmt(liq.liquidationValue)
                + ' = ' + this.crNum(liq.marketCapToLiquidation) + '배';
        }
        return out;
    },

    _crReasonDcfFormula() {
        const valuation = this._crReasonSource()?.valuation;
        const dcf = valuation?.dcf;
        if (!dcf) return null;
        if (!dcf.calculable) return 'DCF 계산 불가 — ' + (dcf.reason || '사유 미상');
        const p = valuation.params || {};
        const r = p.discountRate != null ? this.crNum(p.discountRate * 100, 1) + '%' : '—';
        let out = 'DCF 보수 = 순현금 + FCF ÷ R(' + r + ')\n= ' + this.crAmt(dcf.netCash) + ' + ' + this.crAmt(dcf.fcf)
            + ' ÷ ' + r + ' = ' + this.crAmt(dcf.conservativeValue);
        if (dcf.optimisticValue != null) {
            const years = p.optimisticGrowthYears != null ? this.crNum(p.optimisticGrowthYears, 0) : '—';
            const g = p.optimisticGrowthRate != null ? this.crNum(p.optimisticGrowthRate * 100, 0) + '%' : '—';
            out += '\nDCF 낙관 = 순현금 + ' + years + '년 ' + g + ' 성장 후 무성장 영구가치 할인 = ' + this.crAmt(dcf.optimisticValue);
        }
        if (dcf.marketCapToConservative != null) out += '\n시총/보수 ' + this.crNum(dcf.marketCapToConservative) + '배';
        if (dcf.marketCapToOptimistic != null) out += ' · 시총/낙관 ' + this.crNum(dcf.marketCapToOptimistic) + '배';
        return out;
    },

    // kind: 'statements' | 'ratios'
    _crReasonRowValue(kind, key, year) {
        const snap = this._crReasonSource()?.snapshot;
        const row = (snap?.[kind] || []).find(r => r.key === key);
        return row && row.values ? row.values[year] : null;
    },

    // 비율 공식: 분자·분모는 재무제표 값, 결과는 백엔드 비율 값 그대로. 결과 없으면 블록 생략, 항 결손 시 공식+결과만
    _crReasonRatioFormula(formula, numerKey, denomKey, ratioKey) {
        const year = this.crReasonBaseYear();
        const result = year ? this._crReasonRowValue('ratios', ratioKey, year) : null;
        if (result == null) return null;
        const numer = this._crReasonRowValue('statements', numerKey, year);
        const denom = this._crReasonRowValue('statements', denomKey, year);
        if (numer == null || denom == null) return formula + '\n= ' + this.crPct(result);
        return formula + '\n= ' + this.crAmt(numer) + ' ÷ ' + this.crAmt(denom) + ' = ' + this.crPct(result);
    },

    _crReasonNetCashFormula() {
        const vi = this._crReasonSource()?.snapshot?.valuationInputs;
        if (!vi || vi.netCash == null) return null;
        if (vi.cash == null && vi.securities == null && vi.borrowings == null) {
            return '순현금 = 현금성 + 단기금융 − 차입금 = ' + this.crAmt(vi.netCash);
        }
        return '순현금 = 현금성 + 단기금융 − 차입금\n= ' + this.crAmt(vi.cash) + ' + ' + this.crAmt(vi.securities)
            + ' − ' + this.crAmt(vi.borrowings) + ' = ' + this.crAmt(vi.netCash);
    },

    _crReasonGrowthFormula(formula, stmtKey, ratioKey) {
        const year = this.crReasonBaseYear();
        const result = year ? this._crReasonRowValue('ratios', ratioKey, year) : null;
        if (result == null) return null;
        const prevYear = String(parseInt(year, 10) - 1);
        const cur = this._crReasonRowValue('statements', stmtKey, year);
        const prev = this._crReasonRowValue('statements', stmtKey, prevYear);
        if (cur == null || prev == null) return formula + '\n= ' + this.crPct(result);
        return formula + '\n= (' + this.crAmt(cur) + ' − ' + this.crAmt(prev) + ') ÷ ' + this.crAmt(prev)
            + ' = ' + this.crPct(result);
    },

    // 원천 데이터 표 구성 — 항목별 관련 계정만 (자산 저평가는 청산가치 라인 표 전용 렌더 추가)
    crReasonSourceConfig: {
        assetUndervalue: { statements: ['bs.totalAssets', 'bs.totalLiabilities', 'bs.totalEquity'], ratios: [] },
        earningsUndervalue: { statements: ['is.netIncome', 'cf.operating', 'cf.fcf'], ratios: [] },
        financialHealth: {
            statements: ['bs.totalAssets', 'bs.totalEquity', 'bs.totalLiabilities', 'bs.currentAssets', 'bs.currentLiabilities'],
            ratios: ['equityRatio', 'currentRatio', 'debtRatio']
        },
        profitability: {
            statements: ['is.revenue', 'is.operatingProfit', 'is.netIncome'],
            ratios: ['operatingMargin', 'roe', 'roa']
        },
        growth: {
            statements: ['is.revenue', 'is.operatingProfit'],
            ratios: ['revenueGrowth', 'operatingProfitGrowth']
        }
    },

    crReasonColumns() {
        return this._crReasonSource()?.snapshot?.columns || [];
    },

    // kind: 'statements' | 'ratios'
    crReasonRows(kind) {
        const snap = this._crReasonSource()?.snapshot;
        const keys = this.crReasonSourceConfig[this.crReason.key]?.[kind] || [];
        return keys.map(k => (snap?.[kind] || []).find(r => r.key === k)).filter(Boolean);
    },

    crReasonLiquidationLines() {
        return this._crReasonSource()?.valuation?.liquidation?.lines || [];
    },

    crReasonRiskSignals() {
        const signals = this._crReasonSource()?.snapshot?.riskSignals;
        if (!signals) return [];
        return this.companyReport.riskItems.map(item => ({ label: item.label, value: signals[item.key] }));
    },

    crRiskBadge(value) {
        if (value === true) return 'bg-red-50 text-red-700 border border-red-200';
        if (value === false) return 'bg-green-50 text-green-700 border border-green-200';
        return 'bg-gray-50 text-gray-400 border border-gray-200';
    },

    crRiskLabel(value) {
        if (value === true) return '해당';
        if (value === false) return '해당 없음';
        return '판정 불가';
    },

    crDartViewerUrl(receiptNo) {
        return 'https://dart.fss.or.kr/dsaf001/main.do?rcpNo=' + receiptNo;
    },

    // ==================== 주가지표 계산기 / 예상 매출 ====================
    crAmountInput(obj, key) {
        obj[key] = String(obj[key] ?? '').replace(/[^0-9.]/g, '');
    },

    // 손익/자본: 음수(적자·자본잠식) 허용 — 선행 마이너스 1개만 유지
    crSignedAmountInput(obj, key) {
        obj[key] = String(obj[key] ?? '').replace(/[^0-9.-]/g, '').replace(/(?!^)-/g, '');
    },

    _crNum(value) {
        const n = parseFloat(String(value ?? '').replace(/,/g, ''));
        return isNaN(n) ? null : n;
    },

    // 선택 단위(억/조) → 원 환산 배수
    crUnitMult(manual) {
        return (manual?.amountUnit === '조') ? 1e12 : 1e8;
    },

    _crQuarterSum(row, keys) {
        return keys.map(key => this._crNum(row[key]))
            .filter(v => v != null)
            .reduce((a, b) => a + b, 0);
    },

    // 예상 실적 연합계 [{year, annual(원), netIncome(원|null)}] (연도 유효 + 매출 합계>0만, 연도순)
    _crForecastSeries(manual) {
        const unit = this.crUnitMult(manual);
        return (manual?.revenueForecasts || [])
            .map(row => {
                const annual = this._crQuarterSum(row, ['q1', 'q2', 'q3', 'q4']);
                const net = this._crQuarterSum(row, ['ni1', 'ni2', 'ni3', 'ni4']);
                return { year: row.year, annual: annual * unit, netIncome: net > 0 ? net * unit : null };
            })
            .filter(f => /^\d{4}$/.test(f.year || '') && f.annual > 0)
            .sort((a, b) => a.year.localeCompare(b.year));
    },

    // 편집기 표시용: 해당 행의 매출 연합계 (선택 단위)
    crForecastAnnualDisplay(row) {
        const sum = this._crQuarterSum(row, ['q1', 'q2', 'q3', 'q4']);
        return sum > 0 ? Format.number(sum, 2) : '—';
    },

    // 편집기 표시용: 해당 행의 순이익 연합계 (선택 단위)
    crForecastNetDisplay(row) {
        const sum = this._crQuarterSum(row, ['ni1', 'ni2', 'ni3', 'ni4']);
        return sum > 0 ? Format.number(sum, 2) : '—';
    },

    // 스냅샷 자동 자본총계(원): BPS 근거값(자본총계 term) 우선, 없으면 BPS × 스냅샷 유통주식수 역산. 유통주식수 편집과 무관하게 고정.
    _crAutoEquity(pm, snapshotShares) {
        const terms = pm?.breakdowns?.bps?.terms || [];
        const term = terms.find(t => (t.label || '').includes('자본총계'));
        const fromTerm = term ? this._crNum(term.value) : null;
        if (fromTerm != null) return fromTerm;
        const bps = this._crNum(pm?.bps);
        return (bps != null && snapshotShares != null) ? bps * snapshotShares : null;
    },

    // 분자 ÷ 유통주식수 파생값 + 채택 근거(source). 우선순위: 명시 입력 → 구버전 직접입력 → 자동 분자 → 스냅샷 지표
    _crPerShare(inputAmt, legacyPerShare, autoAmount, snapMetric, unit, shares) {
        const inNum = this._crNum(inputAmt);
        const hasShares = shares != null && shares > 0;
        if (inNum != null && hasShares) {
            return { value: inNum * unit / shares, numerator: inNum * unit, source: 'input' };
        }
        const legacy = this._crNum(legacyPerShare);
        if (legacy != null) {
            return { value: legacy, numerator: null, source: 'legacy' };
        }
        if (autoAmount != null && hasShares) {
            return { value: autoAmount / shares, numerator: autoAmount, source: 'auto' };
        }
        return { value: this._crNum(snapMetric), numerator: null, source: 'snapshot' };
    },

    // 내 계산 표: EPS·BPS 공식에 실제 대입값을 넣은 문자열 (채택 근거 기반 — 우선순위 재복제 안 함)
    crFormulaText(kind, manual, snapshot) {
        const b = this.crMetricBase(manual, snapshot);
        const source = kind === 'eps' ? b.epsSource : b.bpsSource;
        if (source === 'legacy') {
            return '직접 입력값';
        }
        const label = kind === 'eps' ? '당기순이익' : '자본총계';
        const numerator = kind === 'eps' ? b.netIncome : b.equity;
        if (numerator == null || b.shares == null) {
            return label + ' ÷ 유통주식수';
        }
        return label + ' ÷ 유통주식수 = ' + this.crAmt(numerator) + ' ÷ ' + Format.compactNumber(b.shares) + '주';
    },

    // 스냅샷 실적 최신 사업연도(baseYear) 값(원). 없으면 null
    _crPerf(snapshot, key) {
        const row = (snapshot?.performance || []).find(r => r.key === key);
        const baseYear = snapshot?.valuationInputs?.baseYear;
        return (row && baseYear != null) ? this._crNum(row.values[baseYear]) : null;
    },

    // 계산기 재료: 사용자 입력 우선, 비면 스냅샷 자동값 폴백. 금액은 원 환산
    crMetricBase(manual, snapshot) {
        const pm = snapshot?.priceMetrics || {};
        const mi = manual?.metricInputs || {};
        const unit = this.crUnitMult(manual);
        const perf = key => this._crPerf(snapshot, key);
        const autoPrice = this._crNum(pm.referencePrice);
        const autoCap = this._crNum(pm.marketCap);
        const price = this._crNum(mi.price) ?? autoPrice;
        const snapshotShares = (autoCap != null && autoPrice) ? autoCap / autoPrice : null;
        const shares = this._crNum(mi.shares) ?? snapshotShares;
        const marketCap = (price != null && shares != null) ? price * shares : autoCap;
        // EPS·BPS: 분자(당기순이익·자본총계) ÷ 유통주식수. 우선순위/채택 근거는 _crPerShare가 결정(유통주식수만 바꿔도 재계산)
        const autoNet = perf('netIncome');
        const autoEquity = this._crAutoEquity(pm, snapshotShares);
        const epsCalc = this._crPerShare(mi.netIncome, mi.eps, autoNet, pm.eps, unit, shares);
        const bpsCalc = this._crPerShare(mi.equity, mi.bps, autoEquity, pm.bps, unit, shares);
        return {
            price, shares, marketCap,
            netIncome: epsCalc.numerator, equity: bpsCalc.numerator,
            eps: epsCalc.value, bps: bpsCalc.value,
            epsSource: epsCalc.source, bpsSource: bpsCalc.source,
            revenue: this._crNum(mi.revenue) != null ? this._crNum(mi.revenue) * unit : perf('revenue'),
            operatingCf: this._crNum(mi.operatingCf) != null ? this._crNum(mi.operatingCf) * unit : perf('operatingCf')
        };
    },

    // 통합 표: 입력과 무관한 순수 자동값(재료). 편집칸 placeholder(회색 안내값)용
    crAutoMaterials(snapshot) {
        const pm = snapshot?.priceMetrics || {};
        const autoPrice = this._crNum(pm.referencePrice);
        const autoCap = this._crNum(pm.marketCap);
        const snapshotShares = (autoCap != null && autoPrice) ? autoCap / autoPrice : null;
        return {
            price: autoPrice,
            shares: snapshotShares,
            netIncome: this._crPerf(snapshot, 'netIncome'),
            equity: this._crAutoEquity(pm, snapshotShares),
            revenue: this._crPerf(snapshot, 'revenue'),
            operatingCf: this._crPerf(snapshot, 'operatingCf')
        };
    },

    // 통합 표 재료 편집칸 placeholder: 자동값(값 열과 동일 포맷 — 주가=원, 유통주식수=주, 금액=조/억)
    crMaterialAutoHint(key, snapshot) {
        const v = this.crAutoMaterials(snapshot)[key];
        if (v == null) return '자동값 없음';
        if (key === 'price') return '자동 ' + Format.number(v, 0) + '원';
        if (key === 'shares') return '자동 ' + Format.compactNumber(v) + '주';
        return '자동 ' + this.crAmt(v); // 금액: 값 열과 동일하게 조/억 형식
    },

    // 통합 표 재료 유효값(입력 override 우선, 없으면 자동). 표시 문자열
    crMaterialValue(key, manual, snapshot) {
        const b = this.crMetricBase(manual, snapshot);
        if (key === 'price') return b.price != null ? Format.number(b.price, 0) + '원' : '—';
        if (key === 'shares') return b.shares != null ? Format.compactNumber(b.shares) + '주' : '—';
        const v = { netIncome: b.netIncome, equity: b.equity, revenue: b.revenue, operatingCf: b.operatingCf }[key];
        return v != null ? this.crAmt(v) : '—';
    },

    // 계산기 결과 (재료 기반 즉시 계산)
    crCalcMetrics(manual, snapshot) {
        const b = this.crMetricBase(manual, snapshot);
        const div = (x, y) => (x != null && y != null && y > 0) ? x / y : null;
        const per = div(b.price, b.eps);
        const pbr = div(b.price, b.bps);
        return {
            marketCap: b.marketCap,
            per, pbr,
            psr: div(b.marketCap, b.revenue),
            pcfr: div(b.marketCap, b.operatingCf),
            perTimesPbr: (per != null && pbr != null) ? per * pbr : null
        };
    },

    // 예상 매출 기반 연도별 예상 지표 [{year, annual, psr, per}]
    crForecastMetrics(manual, snapshot) {
        const b = this.crMetricBase(manual, snapshot);
        return this._crForecastSeries(manual).map(f => ({
            year: f.year,
            annual: f.annual,
            psr: (b.marketCap != null && f.annual > 0) ? b.marketCap / f.annual : null,
            per: (b.marketCap != null && f.netIncome != null && f.netIncome > 0) ? b.marketCap / f.netIncome : null
        }));
    },

    crTimes(value) {
        return value == null ? '—' : Format.number(value, 2) + '배';
    },

    // 최대주주 지분 추이: 최신 연도 우선 정렬 (백엔드는 연도 오름차순)
    crShareholdersDesc(snapshot) {
        const rows = snapshot?.shareholders?.majorShareholders || [];
        return [...rows].sort((a, b) => (b.bsnsYear || '').localeCompare(a.bsnsYear || ''));
    }
};
