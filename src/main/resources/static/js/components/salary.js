/**
 * SalaryComponent — 월급 사용 비율 상태 관리
 *
 * 소유 프로퍼티: salary
 * 외부 프로퍼티: auth (로그인 userId 조회), $refs (차트 canvas)
 *
 * 설계 원칙 (docs/plans/2026-04-16-001-feat-salary-usage-ratio-tab-plan.md):
 *  - Chart 인스턴스는 필드로 보관하지 않고 Chart.getChart($refs.xxx)로 조회.
 *    → Alpine reactive proxy와 Chart.js 충돌 원천 회피.
 *  - 빠른 월 전환 race 방지: AbortController + generation counter 이중 방어.
 *  - 차트 렌더 구현은 Task #10에서 완성. 본 파일은 상태·API·월 전환·파생 getter.
 */
const SalaryComponent = {
    salary: {
        currentMonth: null,            // "2026-04" 형식
        monthlyView: null,             // MonthlySalaryResponse
        availableMonths: [],           // ["2026-04", "2026-01", ...]
        trend: null,                   // SalaryTrendResponse
        loading: false,

        // 입력 폼 상태
        editingIncome: '',             // string (사용자 입력)
        editingSpending: {},           // { FOOD: { amount, memo }, ... }

        // 새 월 기록 다이얼로그
        showNewMonthDialog: false,
        newMonthInput: ''
    },

    /** 8개 카테고리 메타 (표시 순서·라벨·색) */
    SALARY_CATEGORIES: [
        { key: 'FOOD',               label: '식비',       color: '#ef4444' },
        { key: 'HOUSING',            label: '주거',       color: '#f97316' },
        { key: 'TRANSPORT',          label: '교통',       color: '#eab308' },
        { key: 'EVENTS',             label: '경조사',     color: '#22c55e' },
        { key: 'COMMUNICATION',      label: '통신',       color: '#14b8a6' },
        { key: 'LEISURE',            label: '여가',       color: '#3b82f6' },
        { key: 'SAVINGS_INVESTMENT', label: '저축·투자', color: '#8b5cf6' },
        { key: 'ETC',                label: '기타',       color: '#6b7280' }
    ],
    SALARY_REMAINING_COLOR: 'rgba(156, 163, 175, 0.35)',  // 잔여 세그먼트 (회색 반투명)
    SALARY_OVER_COLOR: '#dc2626',                         // 초과 지출 강조색

    initSalary() {
        // Alpine reactive tracking 제외 필드 (네트워크 취소용 컨트롤러, race counter)
        Object.defineProperty(this.salary, '_abortCtrl', {
            value: null, writable: true, enumerable: false
        });
        Object.defineProperty(this.salary, '_loadGen', {
            value: 0, writable: true, enumerable: false
        });
    },

    // =========================================================================
    // 데이터 로드
    // =========================================================================

    async loadSalaryInitial() {
        if (!this.checkLoggedIn || !this.checkLoggedIn()) return;
        if (this.salary._abortCtrl === undefined) this.initSalary();

        try {
            const userId = this.getSalaryUserId();
            if (!userId) return;

            const months = await API.getSalaryAvailableMonths(userId);
            this.salary.availableMonths = Array.isArray(months) ? months : [];

            // 기본 월: 마지막 변경 레코드 월. 없으면 현재 월(온보딩).
            const initial = this.salary.availableMonths.length > 0
                ? this.salary.availableMonths[0]
                : this.currentYearMonthString();
            this.salary.currentMonth = initial;

            await Promise.all([this.loadSalaryMonthly(initial), this.loadSalaryTrend()]);
        } catch (e) {
            console.error('salary 초기 로드 실패:', e);
        }
    },

    async loadSalaryMonthly(yearMonth) {
        if (!yearMonth) return;
        if (this.salary._abortCtrl === undefined) this.initSalary();

        // 이전 in-flight 요청 중단
        if (this.salary._abortCtrl) this.salary._abortCtrl.abort();
        this.salary._abortCtrl = new AbortController();
        const gen = ++this.salary._loadGen;

        this.salary.loading = true;
        try {
            const userId = this.getSalaryUserId();
            const data = await API.getSalaryMonthly(userId, yearMonth, {
                signal: this.salary._abortCtrl.signal
            });
            if (gen !== this.salary._loadGen) return;   // stale 응답 폐기
            this.salary.monthlyView = data;
            this.syncEditingFromMonthly(data);
            // x-if 블록이 DOM에 mount되고 $refs가 등록될 때까지 대기
            this.scheduleSalaryChartRender('monthly');
        } catch (e) {
            if (e.name !== 'AbortError') {
                console.error('월별 조회 실패:', e);
            }
        } finally {
            if (gen === this.salary._loadGen) this.salary.loading = false;
        }
    },

    async loadSalaryTrend() {
        try {
            const userId = this.getSalaryUserId();
            const data = await API.getSalaryTrend(userId, 12);
            this.salary.trend = data;
            this.scheduleSalaryChartRender('trend');
        } catch (e) {
            console.error('추이 조회 실패:', e);
        }
    },

    /** 조회 결과를 편집 폼 상태에 반영 */
    syncEditingFromMonthly(monthly) {
        this.salary.editingIncome = (monthly && monthly.income != null)
            ? String(monthly.income)
            : '';

        const editing = {};
        const spendings = (monthly && monthly.spendings) || [];
        spendings.forEach(s => {
            editing[s.category] = {
                amount: s.amount != null ? String(s.amount) : '0',
                memo: s.memo || ''
            };
        });
        // 누락된 카테고리는 0으로 채움 (방어적)
        this.SALARY_CATEGORIES.forEach(c => {
            if (!editing[c.key]) editing[c.key] = { amount: '0', memo: '' };
        });
        this.salary.editingSpending = editing;
    },

    // =========================================================================
    // 저장 / 삭제
    // =========================================================================

    async saveSalaryIncome() {
        const amount = this.parseSalaryAmount(this.salary.editingIncome);
        if (amount == null) return;
        try {
            const userId = this.getSalaryUserId();
            const result = await API.upsertSalaryIncome(userId, this.salary.currentMonth, amount);
            this.showSalaryResultToast(result);
            await this.afterSalaryMutation();
        } catch (e) {
            alert('월급 저장 실패: ' + this.salaryErrorMessage(e));
        }
    },

    async saveSalarySpending(category) {
        const entry = this.salary.editingSpending[category] || { amount: '0', memo: '' };
        const amount = this.parseSalaryAmount(entry.amount);
        if (amount == null) return;
        const memo = (entry.memo || '').trim() || null;
        try {
            const userId = this.getSalaryUserId();
            const result = await API.upsertSalarySpending(
                userId, this.salary.currentMonth, category, amount, memo
            );
            this.showSalaryResultToast(result);
            await this.afterSalaryMutation();
        } catch (e) {
            alert('지출 저장 실패: ' + this.salaryErrorMessage(e));
        }
    },

    async deleteSalaryIncomeRecord() {
        if (!confirm('해당 월의 월급 변경 레코드를 삭제할까요?\n이전 설정이 상속되어 적용됩니다.')) return;
        try {
            const userId = this.getSalaryUserId();
            await API.deleteSalaryIncome(userId, this.salary.currentMonth);
            await this.afterSalaryMutation();
        } catch (e) {
            alert('월급 삭제 실패: ' + this.salaryErrorMessage(e));
        }
    },

    async deleteSalarySpendingRecord(category) {
        const label = this.salaryCategoryLabel(category);
        if (!confirm(`해당 월의 "${label}" 변경 레코드를 삭제할까요?\n이전 설정이 상속되어 적용됩니다.`)) return;
        try {
            const userId = this.getSalaryUserId();
            await API.deleteSalarySpending(userId, this.salary.currentMonth, category);
            await this.afterSalaryMutation();
        } catch (e) {
            alert('지출 삭제 실패: ' + this.salaryErrorMessage(e));
        }
    },

    async afterSalaryMutation() {
        const userId = this.getSalaryUserId();
        const [months] = await Promise.all([
            API.getSalaryAvailableMonths(userId),
            this.loadSalaryMonthly(this.salary.currentMonth),
            this.loadSalaryTrend()
        ]);
        this.salary.availableMonths = Array.isArray(months) ? months : [];
    },

    showSalaryResultToast(result) {
        if (!result || result.status !== 'NOOP') return;
        const from = result.inheritedFromMonth
            ? ` (상속 출처: ${result.inheritedFromMonth})` : '';
        alert(`${result.message}${from}`);
    },

    salaryErrorMessage(e) {
        if (!e) return '알 수 없는 오류';
        const msg = e.message || String(e);
        if (msg.includes('409')) {
            return '동시 편집 충돌이 발생했습니다. 잠시 후 다시 시도해주세요.';
        }
        return msg;
    },

    // =========================================================================
    // 월 전환
    // =========================================================================

    goPrevSalaryMonth() {
        if (!this.salary.currentMonth) return;
        const prev = this.addMonthsToString(this.salary.currentMonth, -1);
        this.salary.currentMonth = prev;
        this.loadSalaryMonthly(prev);
    },

    goNextSalaryMonth() {
        if (!this.salary.currentMonth) return;
        const next = this.addMonthsToString(this.salary.currentMonth, 1);
        if (next > this.currentYearMonthString()) {
            alert('미래 월은 조회할 수 없습니다.');
            return;
        }
        this.salary.currentMonth = next;
        this.loadSalaryMonthly(next);
    },

    onSalaryMonthSelected(value) {
        if (!value || value === this.salary.currentMonth) return;
        this.salary.currentMonth = value;
        this.loadSalaryMonthly(value);
    },

    openSalaryNewMonthDialog() {
        this.salary.newMonthInput = this.currentYearMonthString();
        this.salary.showNewMonthDialog = true;
    },

    closeSalaryNewMonthDialog() {
        this.salary.showNewMonthDialog = false;
    },

    async confirmSalaryNewMonth() {
        const ym = (this.salary.newMonthInput || '').trim();
        if (!/^\d{4}-\d{2}$/.test(ym)) {
            alert('올바른 월 형식이 아닙니다 (예: 2026-04)');
            return;
        }
        if (ym > this.currentYearMonthString()) {
            alert('미래 월은 선택할 수 없습니다.');
            return;
        }
        this.salary.currentMonth = ym;
        this.salary.showNewMonthDialog = false;
        await this.loadSalaryMonthly(ym);
    },

    // =========================================================================
    // 파生 값 (메서드로 구현 — spread 연산자가 getter를 복사하지 않기 때문에
    //         getter로 두면 spread 시점 값으로 고정되어 reactive 추적이 안 됨)
    // =========================================================================

    salaryTotalSpendingDisplay() {
        const m = this.salary.monthlyView;
        return m ? this.formatKrw(m.totalSpending) : '-';
    },

    salaryRemainingDisplay() {
        const m = this.salary.monthlyView;
        if (!m || m.remaining == null) return '-';
        return this.formatKrw(m.remaining);
    },

    salaryRatioDisplay() {
        const m = this.salary.monthlyView;
        if (!m || m.savingsRatio == null) return '-';
        const pct = Number(m.savingsRatio) * 100;
        return pct.toFixed(1) + '%';
    },

    salaryIsOverspending() {
        const m = this.salary.monthlyView;
        return !!(m && m.remaining != null && Number(m.remaining) < 0);
    },

    salaryOverspendingAmount() {
        if (!this.salaryIsOverspending()) return 0;
        return -Number(this.salary.monthlyView.remaining);
    },

    salaryIsNegativeRatio() {
        const m = this.salary.monthlyView;
        return !!(m && m.savingsRatio != null && Number(m.savingsRatio) < 0);
    },

    salaryHasAnyData() {
        return !!(this.salary.monthlyView && this.salary.monthlyView.hasAnyData);
    },

    // =========================================================================
    // 차트 렌더링 (Task #10 구현 예정 — 본 파일은 stub + destroy만)
    // =========================================================================

    renderSalaryCharts() {
        this._renderSalaryDonut();
        this._renderSalaryBar();
    },

    /**
     * x-if가 canvas를 DOM에 mount하고 Alpine $refs가 업데이트될 때까지 기다린 후 render.
     * $nextTick + requestAnimationFrame 2단계로 충분 (3단 fallback에서 축소).
     */
    scheduleSalaryChartRender(target) {
        const getCanvas = () => this.$refs && this.$refs[
            target === 'monthly' ? 'salaryDonutCanvas' : 'salaryLineCanvas'
        ];
        const run = () => {
            if (target === 'monthly') this.renderSalaryCharts();
            else this.renderTrendChart();
        };

        this.$nextTick(() => {
            if (getCanvas()) { run(); return; }
            requestAnimationFrame(() => {
                if (getCanvas()) run();
            });
        });
    },

    /**
     * 월별 카테고리 비율 도넛.
     *  - 잔여가 양수: 카테고리 8개 + "잔여" 세그먼트 (회색 반투명)
     *  - 잔여가 음수(초과 지출): 잔여 세그먼트 제외 + 카테고리 색을 모두 경고색으로 치환
     *    (요약 배너에 "초과 ₩X" 텍스트가 별도로 표시됨)
     */
    _renderSalaryDonut() {
        const canvas = this.$refs && this.$refs.salaryDonutCanvas;
        if (!canvas || !window.Chart) return;

        const existing = Chart.getChart(canvas);
        if (existing) existing.destroy();

        const m = this.salary.monthlyView;
        if (!m) return;

        const isOver = this.salaryIsOverspending();
        const overColor = this.SALARY_OVER_COLOR;

        const labels = [];
        const data = [];
        const colors = [];
        this.SALARY_CATEGORIES.forEach(cat => {
            const s = (m.spendings || []).find(x => x.category === cat.key);
            const amount = s ? Number(s.amount) : 0;
            labels.push(cat.label);
            data.push(amount);
            colors.push(isOver ? overColor : cat.color);
        });

        const remaining = m.remaining != null ? Number(m.remaining) : null;
        if (remaining != null && remaining > 0) {
            labels.push('잔여');
            data.push(remaining);
            colors.push(this.SALARY_REMAINING_COLOR);
        }

        const income = m.income != null ? Number(m.income) : 0;
        const spendSum = data.reduce((a, b) => a + b, 0);
        const denominator = income > 0 ? income : (spendSum > 0 ? spendSum : 1);

        const fmt = (v) => this.formatKrw(v);
        new Chart(canvas, {
            type: 'doughnut',
            data: {
                labels,
                datasets: [{
                    data,
                    backgroundColor: colors,
                    borderWidth: 1,
                    borderColor: '#ffffff'
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                animation: false,
                cutout: '55%',
                plugins: {
                    legend: { position: 'bottom', labels: { font: { size: 11 }, boxWidth: 12 } },
                    tooltip: {
                        callbacks: {
                            label: (ctx) => {
                                const v = Number(ctx.parsed) || 0;
                                const pct = (v / denominator * 100).toFixed(1);
                                return `${ctx.label}: ${fmt(v)} (${pct}%)`;
                            }
                        }
                    }
                }
            }
        });
    },

    /** 카테고리별 금액 바 차트. 축 tick은 compact notation ("1만/1억"), 툴팁은 원 단위. */
    _renderSalaryBar() {
        const canvas = this.$refs && this.$refs.salaryBarCanvas;
        if (!canvas || !window.Chart) return;

        const existing = Chart.getChart(canvas);
        if (existing) existing.destroy();

        const m = this.salary.monthlyView;
        if (!m) return;

        const labels = [];
        const data = [];
        const colors = [];
        this.SALARY_CATEGORIES.forEach(cat => {
            const s = (m.spendings || []).find(x => x.category === cat.key);
            labels.push(cat.label);
            data.push(s ? Number(s.amount) : 0);
            colors.push(cat.color);
        });

        const compact = new Intl.NumberFormat('ko-KR', {
            notation: 'compact', maximumFractionDigits: 1
        });
        const fmt = (v) => this.formatKrw(v);

        new Chart(canvas, {
            type: 'bar',
            data: {
                labels,
                datasets: [{
                    data,
                    backgroundColor: colors,
                    borderWidth: 0
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                animation: false,
                plugins: {
                    legend: { display: false },
                    tooltip: {
                        callbacks: {
                            label: (ctx) => `${ctx.label}: ${fmt(ctx.parsed.y)}`
                        }
                    }
                },
                scales: {
                    x: { ticks: { font: { size: 10 } }, grid: { display: false } },
                    y: {
                        beginAtZero: true,
                        ticks: {
                            font: { size: 10 },
                            callback: (v) => compact.format(v)
                        }
                    }
                }
            }
        });
    },

    /**
     * 12개월 추이 라인 차트 — 듀얼 Y축(금액 좌축 / 저축율 % 우축).
     * 저축율 음수 구간은 segment.borderColor로 빨강 강조.
     */
    renderTrendChart() {
        const canvas = this.$refs && this.$refs.salaryLineCanvas;
        if (!canvas || !window.Chart) return;

        const existing = Chart.getChart(canvas);
        if (existing) existing.destroy();

        const trend = this.salary.trend;
        if (!trend || !trend.points || !trend.points.length) return;

        const labels = trend.points.map(p => p.yearMonth);
        const incomes = trend.points.map(p => p.income != null ? Number(p.income) : null);
        const totals = trend.points.map(p => p.totalSpending != null ? Number(p.totalSpending) : 0);
        const ratios = trend.points.map(p => p.savingsRatio != null ? Number(p.savingsRatio) * 100 : null);

        const compact = new Intl.NumberFormat('ko-KR', {
            notation: 'compact', maximumFractionDigits: 1
        });
        const fmt = (v) => this.formatKrw(v);

        new Chart(canvas, {
            type: 'line',
            data: {
                labels,
                datasets: [
                    {
                        label: '월급',
                        data: incomes,
                        yAxisID: 'y',
                        borderColor: '#3b82f6',
                        backgroundColor: 'rgba(59, 130, 246, 0.08)',
                        borderWidth: 2,
                        pointRadius: 3,
                        tension: 0.25,
                        spanGaps: true
                    },
                    {
                        label: '총 지출',
                        data: totals,
                        yAxisID: 'y',
                        borderColor: '#ef4444',
                        backgroundColor: 'rgba(239, 68, 68, 0.08)',
                        borderWidth: 2,
                        pointRadius: 3,
                        tension: 0.25
                    },
                    {
                        label: '저축율(%)',
                        data: ratios,
                        yAxisID: 'y1',
                        borderColor: '#10b981',
                        backgroundColor: 'rgba(16, 185, 129, 0.08)',
                        borderWidth: 2,
                        pointRadius: 3,
                        tension: 0.25,
                        spanGaps: true,
                        segment: {
                            borderColor: (c) => (c.p1.parsed.y < 0 ? '#dc2626' : undefined)
                        }
                    }
                ]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                animation: false,
                interaction: { mode: 'index', intersect: false },
                plugins: {
                    legend: { position: 'bottom', labels: { font: { size: 11 }, boxWidth: 14 } },
                    tooltip: {
                        callbacks: {
                            label: (ctx) => {
                                const v = ctx.parsed.y;
                                if (v == null) return `${ctx.dataset.label}: -`;
                                if (ctx.dataset.yAxisID === 'y1') {
                                    return `${ctx.dataset.label}: ${Number(v).toFixed(1)}%`;
                                }
                                return `${ctx.dataset.label}: ${fmt(v)}`;
                            }
                        }
                    }
                },
                scales: {
                    x: { ticks: { font: { size: 10 }, maxRotation: 45, minRotation: 0 } },
                    y: {
                        position: 'left',
                        beginAtZero: true,
                        ticks: {
                            font: { size: 10 },
                            callback: (v) => compact.format(v)
                        }
                    },
                    y1: {
                        position: 'right',
                        grid: { drawOnChartArea: false },
                        ticks: {
                            font: { size: 10 },
                            callback: (v) => v + '%'
                        }
                    }
                }
            }
        });
    },

    /**
     * 탭 이탈 시 호출. Chart.getChart()로 인스턴스를 조회해 destroy.
     * 인스턴스를 컴포넌트에 보관하지 않으므로 Alpine proxy와 충돌 없음.
     */
    destroySalaryCharts() {
        if (!window.Chart || !Chart.getChart) return;
        ['salaryDonutCanvas', 'salaryBarCanvas', 'salaryLineCanvas'].forEach(ref => {
            const canvas = this.$refs && this.$refs[ref];
            if (!canvas) return;
            const inst = Chart.getChart(canvas);
            if (inst) inst.destroy();
        });
    },

    // =========================================================================
    // Helpers
    // =========================================================================

    parseSalaryAmount(raw) {
        if (raw == null) {
            alert('금액을 입력해주세요.');
            return null;
        }
        const s = String(raw).trim().replace(/,/g, '');
        if (s === '') return 0;
        const n = Number(s);
        if (Number.isNaN(n) || n < 0) {
            alert('금액은 0 이상의 숫자여야 합니다.');
            return null;
        }
        return n;
    },

    formatKrw(value) {
        if (value == null) return '-';
        return new Intl.NumberFormat('ko-KR', {
            style: 'currency', currency: 'KRW', maximumFractionDigits: 0
        }).format(Number(value));
    },

    salaryCategoryLabel(category) {
        const c = this.SALARY_CATEGORIES.find(x => x.key === category);
        return c ? c.label : category;
    },

    /** 해당 월에 월급 직접 변경 레코드(상속이 아닌)가 있는지 */
    hasSalaryDirectIncomeRecord() {
        const m = this.salary.monthlyView;
        return !!(m && m.income != null && m.incomeInheritedFromMonth == null);
    },

    /** 해당 월에 특정 카테고리의 지출 직접 변경 레코드가 있는지 */
    hasSalaryDirectSpendingRecord(category) {
        const m = this.salary.monthlyView;
        if (!m) return false;
        const s = (m.spendings || []).find(x => x.category === category);
        return !!(s && s.inheritedFromMonth == null && s.amount != null && Number(s.amount) > 0);
    },

    addMonthsToString(yearMonth, delta) {
        const [y, m] = yearMonth.split('-').map(Number);
        const d = new Date(y, m - 1 + delta, 1);
        return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`;
    },

    currentYearMonthString() {
        const d = new Date();
        return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`;
    },

    getSalaryUserId() {
        if (this.auth && this.auth.userId) return this.auth.userId;
        const stored = localStorage.getItem('userId');
        return stored ? Number(stored) : null;
    }
};