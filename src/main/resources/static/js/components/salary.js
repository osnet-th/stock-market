/**
 * SalaryComponent — 월급 사용 비율 (목업 기반 재설계)
 *
 * 소유 프로퍼티: salary
 * 외부 프로퍼티: auth (로그인 userId 조회)
 *
 * 설계 (docs/plans/2026-08-21-001-feat-salary-usage-screen-redesign-plan.md):
 *  - 서버 응답(monthlyView)을 편집 버퍼(income·cats)로 복사해 로컬 편집 → dirty 추적 →
 *    `변경 저장`이 PUT /api/salary/monthly/{yearMonth} 일괄 저장.
 *  - 카테고리 금액: 하위 항목이 있으면 합계(파생), 없으면 직접 입력(flat).
 *  - 추이 차트는 Chart.js 대신 목업과 동일한 SVG 문자열 렌더(x-html) — 비율/금액/구성 3모드.
 *  - 빠른 월 전환 race 방지: AbortController + generation counter (기존 유지).
 */

// 편집 버퍼 항목 로컬 키 (x-for :key 안정성용 — 서버 왕복 시 재발급)
let _salaryUid = 0;

const SalaryComponent = {
    salary: {
        currentMonth: null,            // "2026-08" 형식
        availableMonths: [],
        monthlyView: null,             // MonthlySalaryResponse (마지막 로드/저장 결과)
        trend: null,                   // SalaryTrendResponse
        loading: false,
        saving: false,

        // 편집 버퍼
        income: 0,                     // 월 실수령액 (number)
        cats: [],                      // [{ key, label, color, flat, budget, items:[{uid,name,amount,fixed}], open }]
        dirty: false,

        savingTarget: 50,              // 저축률 목표(%) — 서버 설정값 로드, 편집 가능

        showBudget: true,              // 예산 대비 열 표시
        trendMode: '비율',             // '비율' | '금액' | '구성'

        showNewMonthDialog: false,
        newMonthInput: ''
    },

    /** 신규 커스텀 카테고리 색 팔레트 (서버 팔레트와 동일 — 저장 후 서버 색이 확정) */
    SALARY_CUSTOM_COLORS: ['#5b8a72', '#a86bc9', '#c97b4a', '#4a90a4', '#8a6a34', '#6b737a'],

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

        // 다른 페이지에 다녀온 재진입 시 미저장 편집을 조용히 버리지 않는다
        if (this.salary.dirty && this.salary.monthlyView) return;

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
            this.buildSalaryBuffer(data);
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
            this.salary.trend = await API.getSalaryTrend(userId, 12);
        } catch (e) {
            console.error('추이 조회 실패:', e);
        }
    },

    async refreshSalaryMonths() {
        const months = await API.getSalaryAvailableMonths(this.getSalaryUserId());
        this.salary.availableMonths = Array.isArray(months) ? months : [];
    },

    // =========================================================================
    // 편집 버퍼
    // =========================================================================

    /** 서버 응답을 편집 버퍼로 복사. 접기/펼치기 상태는 유지한다. 카테고리는 응답 메타 기반 동적. */
    buildSalaryBuffer(view) {
        const openMap = {};
        this.salary.cats.forEach(c => { if (c.key) openMap[c.key] = c.open; });

        this.salary.income = view && view.income != null ? Number(view.income) : 0;
        this.salary.savingTarget = view && view.savingTarget != null ? Number(view.savingTarget) : 50;
        this.salary.cats = this.salaryLinesToCats(view, openMap);
        this.salary.dirty = false;
    },

    /** 응답 spendings 라인 → 편집 버퍼 카테고리 배열 */
    salaryLinesToCats(view, openMap) {
        return (((view && view.spendings) || [])).map(line => {
            const items = (line.items || []).map(it => ({
                uid: ++_salaryUid,
                name: it.name || '',
                amount: Number(it.amount) || 0,
                fixed: !!it.fixed
            }));
            return {
                uid: ++_salaryUid,
                key: line.category,
                label: line.categoryLabel || line.category,
                color: line.color || '#7a828a',
                savings: !!line.savings,
                system: !!line.system,
                isNew: false,
                flat: items.length ? 0 : (Number(line.amount) || 0),
                budget: line.budget != null ? Number(line.budget) : 0,
                items,
                open: !!(openMap && openMap[line.category])
            };
        });
    },

    markSalaryDirty() {
        this.salary.dirty = true;
    },

    /** 새 커스텀 카테고리 추가 (저장 시 서버가 code·확정 색 발급) */
    addSalaryCategory() {
        this.salary.cats.push({
            uid: ++_salaryUid,
            key: null,
            label: '새 카테고리',
            color: this.SALARY_CUSTOM_COLORS[this.salary.cats.length % this.SALARY_CUSTOM_COLORS.length],
            savings: false,
            system: false,
            isNew: true,
            flat: 0,
            budget: 0,
            items: [],
            open: true
        });
        this.markSalaryDirty();
    },

    /** 카테고리 제거 (저축 카테고리 불가 — 버튼 자체를 숨김). 저장 전까지는 되돌리기 가능 */
    removeSalaryCategory(cat) {
        if (cat.savings) return;
        this.salary.cats = this.salary.cats.filter(c => c.uid !== cat.uid);
        this.markSalaryDirty();
    },

    /** 커스텀 카테고리 이름 변경 (system 카테고리는 입력 자체가 없음) */
    setSalaryCategoryName(cat, raw) {
        if (cat.system) return;
        cat.label = String(raw || '').trim().slice(0, 40) || '새 카테고리';
        this.markSalaryDirty();
    },

    /** 저축률 목표(%) 변경 — 0~100 클램프 */
    setSalaryTarget(raw) {
        const n = this.parseSalaryNum(raw);
        this.salary.savingTarget = Math.max(0, Math.min(100, n));
        this.markSalaryDirty();
    },

    salaryTarget() {
        const n = Number(this.salary.savingTarget);
        return Number.isNaN(n) ? 50 : n;
    },

    setSalaryIncome(raw) {
        this.salary.income = this.parseSalaryNum(raw);
        this.markSalaryDirty();
    },

    setSalaryFlat(cat, raw) {
        cat.flat = this.parseSalaryNum(raw);
        this.markSalaryDirty();
    },

    setSalaryBudget(cat, raw) {
        cat.budget = this.parseSalaryNum(raw);
        this.markSalaryDirty();
    },

    setSalaryItemName(item, raw) {
        item.name = String(raw || '').slice(0, 100);
        this.markSalaryDirty();
    },

    setSalaryItemAmount(item, raw) {
        item.amount = this.parseSalaryNum(raw);
        this.markSalaryDirty();
    },

    toggleSalaryItemFixed(item) {
        item.fixed = !item.fixed;
        this.markSalaryDirty();
    },

    addSalaryItem(cat) {
        cat.items.push({ uid: ++_salaryUid, name: '', amount: 0, fixed: false });
        cat.open = true;
        this.markSalaryDirty();
    },

    removeSalaryItem(cat, item) {
        cat.items = cat.items.filter(i => i.uid !== item.uid);
        this.markSalaryDirty();
    },

    toggleSalaryCat(cat) {
        cat.open = !cat.open;
    },

    expandAllSalaryCats() {
        this.salary.cats.forEach(c => { c.open = true; });
    },

    collapseAllSalaryCats() {
        this.salary.cats.forEach(c => { c.open = false; });
    },

    toggleSalaryBudgetColumn() {
        this.salary.showBudget = !this.salary.showBudget;
    },

    // =========================================================================
    // 저장 / 되돌리기 / 지난달 복사
    // =========================================================================

    async saveSalaryMonthly() {
        if (this.salary.saving) return;
        const payload = {
            income: Number(this.salary.income) || 0,
            savingTarget: this.salaryTarget(),
            categories: this.salary.cats.map(c => ({
                category: c.isNew ? null : c.key,
                name: c.label,
                amount: c.items.length ? null : (Number(c.flat) || 0),
                budget: Number(c.budget) || 0,
                items: c.items.map(i => ({
                    name: i.name || '',
                    amount: Number(i.amount) || 0,
                    fixed: !!i.fixed
                }))
            }))
        };

        this.salary.saving = true;
        try {
            const userId = this.getSalaryUserId();
            const view = await API.saveSalaryMonthly(userId, this.salary.currentMonth, payload);
            this.salary.monthlyView = view;
            this.buildSalaryBuffer(view);
            await Promise.all([this.refreshSalaryMonths(), this.loadSalaryTrend()]);
        } catch (e) {
            alert('저장 실패: ' + this.salaryErrorMessage(e));
        } finally {
            this.salary.saving = false;
        }
    },

    /** 마지막 로드/저장 상태로 버퍼 복원 */
    resetSalaryBuffer() {
        if (!this.salary.monthlyView) return;
        this.buildSalaryBuffer(this.salary.monthlyView);
    },

    /** 이전 달 유효값을 편집 버퍼로 복사 (월급·목표 제외, 저장 전까지 미반영) */
    async copyPrevSalaryMonth() {
        try {
            const prev = this.addMonthsToString(this.salary.currentMonth, -1);
            const data = await API.getSalaryMonthly(this.getSalaryUserId(), prev);
            const openMap = {};
            this.salary.cats.forEach(c => { if (c.key) openMap[c.key] = c.open; });
            this.salary.cats = this.salaryLinesToCats(data, openMap);
            this.markSalaryDirty();
        } catch (e) {
            alert('지난달 불러오기 실패: ' + this.salaryErrorMessage(e));
        }
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

    confirmSalaryLeave() {
        return !this.salary.dirty
            || confirm('저장 안 된 변경이 있습니다. 이동하면 사라집니다. 계속할까요?');
    },

    goPrevSalaryMonth() {
        if (!this.salary.currentMonth || !this.confirmSalaryLeave()) return;
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
        if (!this.confirmSalaryLeave()) return;
        this.salary.currentMonth = next;
        this.loadSalaryMonthly(next);
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
        if (!this.confirmSalaryLeave()) return;
        this.salary.currentMonth = ym;
        this.salary.showNewMonthDialog = false;
        await this.loadSalaryMonthly(ym);
    },

    // =========================================================================
    // 파생 값 — 요약 카드
    // =========================================================================

    salaryCatTotal(cat) {
        if (cat.items.length) {
            return cat.items.reduce((a, i) => a + (Number(i.amount) || 0), 0);
        }
        return Number(cat.flat) || 0;
    },

    salaryAllocated() {
        return this.salary.cats.reduce((a, c) => a + this.salaryCatTotal(c), 0);
    },

    salaryLeft() {
        return (Number(this.salary.income) || 0) - this.salaryAllocated();
    },

    salarySaved() {
        return this.salary.cats
            .filter(c => c.savings)
            .reduce((a, c) => a + this.salaryCatTotal(c), 0);
    },

    salaryRate() {
        const income = Number(this.salary.income) || 0;
        return income ? (this.salarySaved() / income) * 100 : 0;
    },

    /** 항목의 고정/변동 합계 (항목 없는 flat 금액은 미포함 — 목업 로직 동일) */
    salaryFixedVar() {
        let fixed = 0, variable = 0;
        this.salary.cats.forEach(c => c.items.forEach(i => {
            if (i.fixed) fixed += Number(i.amount) || 0;
            else variable += Number(i.amount) || 0;
        }));
        return { fixed, variable, total: (fixed + variable) || 1 };
    },

    salaryMaxCat() {
        return Math.max(...this.salary.cats.map(c => this.salaryCatTotal(c)), 1);
    },

    /** 배분 리본 세그먼트 (+미배분 꼬리) */
    salaryRibbon() {
        const income = Number(this.salary.income) || 0;
        const segs = this.salary.cats.map(c => {
            const total = this.salaryCatTotal(c);
            const p = income ? (total / income) * 100 : 0;
            return {
                key: 'c' + c.uid, name: c.label, color: c.color,
                pct: p.toFixed(1) + '%',
                w: Math.max(0, p).toFixed(2) + '%',
                showLabel: p > 6,
                title: c.label + ' ' + this.salaryWon(total) + '원'
            };
        });
        const left = this.salaryLeft();
        if (income && left > 0) {
            const p = (left / income) * 100;
            segs.push({
                key: '_left', name: '미배분', color: '#c9ced6',
                pct: p.toFixed(1) + '%', w: p.toFixed(2) + '%',
                showLabel: false, title: '미배분 ' + this.salaryWon(left) + '원'
            });
        }
        return segs;
    },

    salaryLeftLabel() {
        return this.salaryLeft() >= 0 ? '미배분' : '초과';
    },

    salaryLeftColor() {
        const left = this.salaryLeft();
        if (left < 0) return '#c02a22';
        return left === 0 ? '#2e8b62' : '#8b9299';
    },

    salaryLeftPctLabel() {
        const income = Number(this.salary.income) || 0;
        return income ? (Math.abs(this.salaryLeft()) / income * 100).toFixed(1) + '%' : '—';
    },

    salaryLeftBarW() {
        const income = Number(this.salary.income) || 0;
        if (!income) return '0%';
        return Math.min(100, Math.abs(this.salaryLeft()) / this.salaryMaxCat() * 100).toFixed(1) + '%';
    },

    /** 전월 저축률(%) — previous 요약 기반. 없으면 null */
    salaryPrevRate() {
        const prev = this.salary.monthlyView && this.salary.monthlyView.previous;
        if (!prev || prev.savingsRatio == null) return null;
        return Number(prev.savingsRatio) * 100;
    },

    salarySavingDeltaLabel() {
        const prevRate = this.salaryPrevRate();
        if (prevRate == null) return '—';
        const d = this.salaryRate() - prevRate;
        return (d >= 0 ? '+' : '−') + Math.abs(d).toFixed(1) + '%p';
    },

    salarySavingDeltaColor() {
        const prevRate = this.salaryPrevRate();
        if (prevRate == null) return '#9aa3ab';
        return this.salaryRate() >= prevRate ? '#5fd0a0' : '#f0857a';
    },

    salarySavingNote() {
        return '저축·투자 ' + this.salaryMan(this.salarySaved())
            + ' / 월급 ' + this.salaryMan(Number(this.salary.income) || 0);
    },

    salaryInheritNote() {
        const v = this.salary.monthlyView;
        if (!v) return '';
        const months = [v.incomeInheritedFromMonth, v.itemsInheritedFromMonth]
            .concat((v.spendings || []).map(s => s.inheritedFromMonth))
            .filter(Boolean);
        const src = months.length ? months.slice().sort().pop() : null;
        const dirtyTxt = '저장 안 된 변경 ' + (this.salary.dirty ? '있음' : '없음');
        return (src ? '상속 출처 ' + src + ' · ' : '') + dirtyTxt;
    },

    // =========================================================================
    // 파생 값 — 사용처 테이블
    // =========================================================================

    salaryCatKind(cat) {
        if (cat.savings) {
            return { label: '저축', bg: '#eff3fa', color: '#1f4f9e' };
        }
        if (cat.items.length && cat.items.every(i => i.fixed)) {
            return { label: '고정', bg: '#eef2f0', color: '#2e6b52' };
        }
        return { label: '변동 포함', bg: '#faf4ea', color: '#8a6a34' };
    },

    salaryCatCountLabel(cat) {
        return cat.items.length ? cat.items.length + '개 항목 · 합계' : '항목 없음 · 직접 입력';
    },

    salaryCatPctLabel(cat) {
        const income = Number(this.salary.income) || 0;
        return income ? (this.salaryCatTotal(cat) / income * 100).toFixed(1) + '%' : '—';
    },

    salaryCatBarW(cat) {
        return (this.salaryCatTotal(cat) / this.salaryMaxCat() * 100).toFixed(1) + '%';
    },

    salaryBudgetDiffLabel(cat) {
        const budget = Number(cat.budget) || 0;
        if (!budget) return '—';
        const diff = this.salaryCatTotal(cat) - budget;
        if (diff === 0) return '±0';
        return (diff > 0 ? '+' : '−') + this.salaryMan(Math.abs(diff));
    },

    salaryBudgetDiffColor(cat) {
        const budget = Number(cat.budget) || 0;
        if (!budget) return '#98a0a7';
        return this.salaryCatTotal(cat) - budget > 0 ? '#c02a22' : '#2e8b62';
    },

    salaryItemPctOfCat(cat, item) {
        const total = this.salaryCatTotal(cat);
        return total ? ((Number(item.amount) || 0) / total * 100).toFixed(0) + '%' : '—';
    },

    salaryItemBarW(cat, item) {
        const maxItem = Math.max(...cat.items.map(i => Number(i.amount) || 0), 1);
        return ((Number(item.amount) || 0) / maxItem * 100).toFixed(1) + '%';
    },

    salaryItemShareLabel(item) {
        const income = Number(this.salary.income) || 0;
        return income ? '월급 ' + ((Number(item.amount) || 0) / income * 100).toFixed(1) + '%' : '—';
    },

    salarySaveHint() {
        return this.salary.dirty
            ? '저장하지 않은 변경이 있습니다 — 항목·금액·예산이 함께 저장됩니다'
            : '모든 변경이 저장되었습니다';
    },

    salarySaveLabel() {
        if (this.salary.saving) return '저장 중…';
        return this.salary.dirty ? '변경 저장' : '저장됨';
    },

    // =========================================================================
    // 추이 차트 (SVG 문자열 렌더)
    // =========================================================================

    /** trend 포인트를 차트용으로 변환. 현재 월 포인트는 편집 버퍼 값으로 라이브 반영 */
    /** 추이 응답의 categories 메타 (없으면 빈 배열) */
    salaryTrendCatMeta() {
        return (this.salary.trend && this.salary.trend.categories) || [];
    },

    _salaryTrendData() {
        const t = this.salary.trend;
        const pts = (t && t.points) ? t.points : [];
        const savingsCodes = new Set(this.salaryTrendCatMeta().filter(m => m.savings).map(m => m.code));
        const data = pts.map(p => {
            const cats = {};
            let saved = 0;
            (p.categoryTotals || []).forEach(ct => {
                const v = Number(ct.amount) || 0;
                cats[ct.category] = v;
                if (savingsCodes.has(ct.category)) saved += v;
            });
            return {
                m: p.yearMonth,
                salary: p.income != null ? Number(p.income) : null,
                spend: Math.max(0, (Number(p.totalSpending) || 0) - saved),
                rate: p.savingsRatio != null ? Number(p.savingsRatio) * 100 : null,
                cats
            };
        });

        const idx = data.findIndex(d => d.m === this.salary.currentMonth);
        if (idx >= 0) {
            const income = Number(this.salary.income) || 0;
            const cats = {};
            let saved = 0;
            this.salary.cats.forEach(c => {
                if (!c.key) return;                       // 저장 전 신규 카테고리는 제외
                const v = this.salaryCatTotal(c);
                cats[c.key] = v;
                if (c.savings) saved += v;
            });
            data[idx] = {
                m: this.salary.currentMonth,
                salary: income || null,
                spend: Math.max(0, this.salaryAllocated() - saved),
                rate: income ? (saved / income) * 100 : null,
                cats
            };
        }
        return data;
    },

    salaryTrendTitle() {
        return this.salary.trendMode === '구성' ? '월별 구성 비율 추이' : '최근 12개월 추이';
    },

    salaryTrendNote() {
        if (this.salary.trendMode === '비율') return '저축률 하나만 보여 목표선과의 거리를 바로 읽습니다';
        if (this.salary.trendMode === '금액') return '월급은 점선, 총 지출은 실선 — 간격이 곧 저축액입니다';
        return '카테고리가 월급에서 차지하는 비중 변화';
    },

    salaryTrendLegend() {
        if (this.salary.trendMode === '비율') {
            return [
                { name: '저축률', color: '#1f4f9e' },
                { name: '목표 ' + this.salaryTarget() + '%', color: '#2e8b62' }
            ];
        }
        if (this.salary.trendMode === '금액') {
            return [{ name: '월급', color: '#b6bbc2' }, { name: '총 지출', color: '#c02a22' }];
        }
        return this.salaryCompositionStack().map(m => ({ name: m.name, color: m.color }));
    },

    /** 구성 모드 스택 대상 — 추이에 값이 있는 카테고리 (메타 순서) */
    salaryCompositionStack() {
        const hist = this._salaryTrendData();
        return this.salaryTrendCatMeta()
            .filter(m => hist.some(h => (h.cats[m.code] || 0) > 0))
            .map(m => ({ code: m.code, name: m.name, color: m.color }));
    },

    /** X축 라벨 — 최대 5개 균등 추출 ('YYYY.MM') */
    salaryTrendAxis() {
        const hist = this._salaryTrendData();
        if (!hist.length) return [];
        const n = Math.min(5, hist.length);
        const idxs = Array.from({ length: n }, (_, i) => Math.round(i * (hist.length - 1) / Math.max(1, n - 1)));
        return Array.from(new Set(idxs)).map(i => String(hist[i].m).replace('-', '.'));
    },

    salaryTrendSvg() {
        const hist = this._salaryTrendData();
        if (hist.length < 2) {
            return '<div style="display:flex;align-items:center;justify-content:center;height:100%;'
                + 'font-size:12px;color:#98a0a7">추이를 그리려면 두 달 이상의 기록이 필요합니다</div>';
        }
        const W = 900, H = 230, PADT = 12, PADB = 24, PADL = 4;
        const px = i => PADL + (i / (hist.length - 1)) * (W - PADL * 2);

        let inner = '';
        [0.25, 0.5, 0.75].forEach(f => {
            const y = (PADT + (H - PADT - PADB) * f).toFixed(1);
            inner += `<line x1="0" x2="${W}" y1="${y}" y2="${y}" stroke="rgba(0,0,0,.05)" stroke-width="1"/>`;
        });

        if (this.salary.trendMode === '비율') {
            inner += this._salaryRatioSvg(hist, px, W, H, PADT, PADB);
        } else if (this.salary.trendMode === '금액') {
            inner += this._salaryAmountSvg(hist, px, W, H, PADT, PADB);
        } else {
            inner += this._salaryCompositionSvg(hist, px, W, H, PADT, PADB);
        }
        return `<svg viewBox="0 0 ${W} ${H}" preserveAspectRatio="none" `
            + `style="width:100%;height:100%;display:block">${inner}</svg>`;
    },

    /** 비율 모드 — 저축률 라인 + 목표 점선. 스케일은 데이터·목표를 감싸는 5% 단위 */
    _salaryRatioSvg(hist, px, W, H, PADT, PADB) {
        const TARGET = this.salaryTarget();
        const rates = hist.filter(h => h.rate != null).map(h => h.rate);
        const lo = Math.max(0, Math.floor((Math.min(...rates, TARGET) - 5) / 5) * 5);
        const hi = Math.min(120, Math.ceil((Math.max(...rates, TARGET) + 5) / 5) * 5);
        const py = v => PADT + (1 - (v - lo) / ((hi - lo) || 1)) * (H - PADT - PADB);

        let d = '', firstX = null, lastX = null, out = '';
        hist.forEach((h, i) => {
            if (h.rate == null) return;
            const x = px(i);
            if (firstX == null) firstX = x;
            lastX = x;
            d += (d ? ' L' : 'M') + x.toFixed(1) + ' ' + py(h.rate).toFixed(1);
        });
        if (d) {
            out += `<path d="${d} L${lastX.toFixed(1)} ${H - PADB} L${firstX.toFixed(1)} ${H - PADB} Z" fill="#1f4f9e" opacity=".08"/>`;
            out += `<path d="${d}" fill="none" stroke="#1f4f9e" stroke-width="2.2" vector-effect="non-scaling-stroke" stroke-linejoin="round"/>`;
        }
        const ty = py(TARGET).toFixed(1);
        out += `<line x1="0" x2="${W}" y1="${ty}" y2="${ty}" stroke="#2e8b62" stroke-width="1" stroke-dasharray="5 4"/>`;
        out += `<text x="${W - 6}" y="${(py(TARGET) - 5).toFixed(1)}" text-anchor="end" `
            + `style="font-size:9.5px;fill:#2e8b62;font-family:'IBM Plex Mono',monospace">목표 ${TARGET}%</text>`;
        hist.forEach((h, i) => {
            if (h.rate == null) return;
            out += `<circle cx="${px(i).toFixed(1)}" cy="${py(h.rate).toFixed(1)}" r="${i === hist.length - 1 ? 4 : 2.4}" fill="#1f4f9e"/>`;
        });
        for (let v = lo + 5; v < hi; v += 5) {
            out += `<text x="4" y="${(py(v) - 4).toFixed(1)}" `
                + `style="font-size:9px;fill:#b2b8bd;font-family:'IBM Plex Mono',monospace">${v}%</text>`;
        }
        return out;
    },

    /** 금액 모드 — 월급 점선 + 총 지출 실선/영역 */
    _salaryAmountSvg(hist, px, W, H, PADT, PADB) {
        const hi = Math.max(...hist.map(h => Math.max(h.salary || 0, h.spend)), 1) * 1.08;
        const py = v => PADT + (1 - v / hi) * (H - PADT - PADB);

        let sd = '', pd = '', out = '';
        hist.forEach((h, i) => {
            if (h.salary != null) sd += (sd ? ' L' : 'M') + px(i).toFixed(1) + ' ' + py(h.salary).toFixed(1);
            pd += (pd ? ' L' : 'M') + px(i).toFixed(1) + ' ' + py(h.spend).toFixed(1);
        });
        if (sd) {
            out += `<path d="${sd}" fill="none" stroke="#b6bbc2" stroke-width="1.8" stroke-dasharray="5 4" vector-effect="non-scaling-stroke"/>`;
        }
        out += `<path d="${pd} L${px(hist.length - 1).toFixed(1)} ${H - PADB} L${px(0).toFixed(1)} ${H - PADB} Z" fill="#c02a22" opacity=".08"/>`;
        out += `<path d="${pd}" fill="none" stroke="#c02a22" stroke-width="2.2" vector-effect="non-scaling-stroke" stroke-linejoin="round"/>`;
        hist.forEach((h, i) => {
            out += `<circle cx="${px(i).toFixed(1)}" cy="${py(h.spend).toFixed(1)}" r="${i === hist.length - 1 ? 4 : 2.4}" fill="#c02a22"/>`;
        });
        [0.25, 0.5, 0.75].forEach(f => {
            const v = hi * f;
            out += `<text x="4" y="${(py(v) - 4).toFixed(1)}" `
                + `style="font-size:9px;fill:#b2b8bd;font-family:'IBM Plex Mono',monospace">${this.salaryMan(v)}</text>`;
        });
        return out;
    },

    /** 구성 모드 — 카테고리별 월급 대비 비중 스택 영역 (동적 카테고리) */
    _salaryCompositionSvg(hist, px, W, H, PADT, PADB) {
        const py = v => PADT + (1 - v / 100) * (H - PADT - PADB);
        let base = hist.map(() => 0);
        let out = '';

        this.salaryCompositionStack().forEach(meta => {
            const key = meta.code;
            const vals = hist.map(h => (h.salary ? ((h.cats[key] || 0) / h.salary) * 100 : 0));
            const top = vals.map((v, i) => base[i] + v);
            let up = '';
            top.forEach((v, i) => { up += (up ? ' L' : 'M') + px(i).toFixed(1) + ' ' + py(v).toFixed(1); });
            let down = '';
            for (let i = base.length - 1; i >= 0; i--) {
                down += ' L' + px(i).toFixed(1) + ' ' + py(base[i]).toFixed(1);
            }
            out += `<path d="${up}${down} Z" fill="${meta.color}" opacity=".82"/>`;
            base = top;
        });
        [25, 50, 75].forEach(v => {
            out += `<text x="4" y="${(py(v) - 4).toFixed(1)}" `
                + `style="font-size:9px;fill:#fff;font-family:'IBM Plex Mono',monospace">${v}%</text>`;
        });
        return out;
    },

    // =========================================================================
    // 지난달 대비 · 인사이트
    // =========================================================================

    salaryPrevMonthLabel() {
        const prev = this.salary.monthlyView && this.salary.monthlyView.previous;
        return prev ? prev.yearMonth + ' 기준' : '전월 기록 없음';
    },

    /** 카테고리별 전월 대비 증감 (절대값 상위 6) */
    salaryDeltas() {
        const prev = this.salary.monthlyView && this.salary.monthlyView.previous;
        if (!prev) return [];
        const prevMap = {};
        (prev.categoryTotals || []).forEach(ct => { prevMap[ct.category] = Number(ct.amount) || 0; });

        return this.salary.cats.map(c => {
            const cur = this.salaryCatTotal(c);
            const p = prevMap[c.key] || 0;
            const d = cur - p;
            const pct = p ? (d / p) * 100 : 0;
            const clamp = Math.max(-40, Math.min(40, pct));
            const half = (Math.abs(clamp) / 40) * 50;
            return {
                key: 'd' + c.uid, name: c.label, color: c.color, abs: Math.abs(d),
                label: d === 0 ? '±0' : (d > 0 ? '+' : '−') + this.salaryMan(Math.abs(d)),
                barColor: d > 0 ? '#c02a22' : (d < 0 ? '#2e8b62' : '#98a0a7'),
                barL: (clamp >= 0 ? 50 : 50 - half).toFixed(1) + '%',
                barW: Math.max(1, half).toFixed(1) + '%'
            };
        }).sort((a, b) => b.abs - a.abs).slice(0, 6);
    },

    /** 눈에 띄는 것 — 예산 초과 / 저축률 목표 / 고정비 비중 (최대 3장) */
    salaryInsights() {
        const out = [];
        const overs = this.salary.cats
            .map(c => ({ c, budget: Number(c.budget) || 0, over: this.salaryCatTotal(c) - (Number(c.budget) || 0) }))
            .filter(x => x.budget > 0 && x.over > 0)
            .sort((a, b) => b.over - a.over);
        if (overs.length) {
            out.push({
                key: 'over',
                title: overs[0].c.label + ' 예산 초과 ' + this.salaryMan(overs[0].over),
                body: '예산 ' + this.salaryMan(overs[0].budget) + ' 대비 초과입니다. 하위 항목을 열어 어디서 늘었는지 확인해 보세요.',
                color: '#c02a22', bg: '#fdf5f4'
            });
        }

        const rate = this.salaryRate();
        const target = this.salaryTarget();
        if (rate >= target) {
            out.push({
                key: 'rate',
                title: '저축률 ' + rate.toFixed(1) + '% — 목표 달성',
                body: '저축·투자 ' + this.salaryMan(this.salarySaved()) + '으로 목표선 ' + target + '%를 넘겼습니다.',
                color: '#2e8b62', bg: '#f2f8f5'
            });
        } else {
            out.push({
                key: 'rate',
                title: '저축률 목표까지 ' + (target - rate).toFixed(1) + '%p',
                body: '약 ' + this.salaryMan((target - rate) / 100 * (Number(this.salary.income) || 0))
                    + '을 저축·투자로 옮기면 목표선에 닿습니다.',
                color: '#1f4f9e', bg: '#f4f7fc'
            });
        }

        const fv = this.salaryFixedVar();
        const topFixed = this.salary.cats
            .map(c => ({ label: c.label, f: c.items.filter(i => i.fixed).reduce((a, i) => a + (Number(i.amount) || 0), 0) }))
            .filter(x => x.f > 0)
            .sort((a, b) => b.f - a.f)
            .slice(0, 2)
            .map(x => x.label);
        out.push({
            key: 'fixed',
            title: '고정비 ' + ((fv.fixed / fv.total) * 100).toFixed(0) + '%',
            body: '고정비 ' + this.salaryMan(fv.fixed)
                + (topFixed.length ? ' 중 ' + topFixed.join('·') + '가 큰 비중입니다.' : '입니다.')
                + ' 항목별 고정/변동 태그로 조정할 수 있습니다.',
            color: '#b5854a', bg: '#fbf8f3'
        });
        return out.slice(0, 3);
    },

    // =========================================================================
    // Helpers
    // =========================================================================

    /** 정수 KRW 포맷 (ko-KR 그룹핑, 통화 기호 없음 — 목업 표기) */
    salaryWon(n) {
        return Math.round(Number(n) || 0).toLocaleString('ko-KR');
    },

    /** 만 단위 축약 표기 ('123만'). 1만 미만은 '0만' 대신 원 단위로 표기 */
    salaryMan(n) {
        const v = Math.round(Math.abs(Number(n) || 0));
        if (v < 10000) {
            return v.toLocaleString('ko-KR') + '원';
        }
        return Math.round(v / 10000).toLocaleString('ko-KR') + '만';
    },

    /** 콤마 포함 입력 파싱 — 숫자 이외 문자는 제거, 음수 미허용 */
    parseSalaryNum(raw) {
        const n = Number(String(raw == null ? '' : raw).replace(/[^0-9]/g, ''));
        return Number.isNaN(n) ? 0 : n;
    },

    formatKrw(value) {
        if (value == null) return '-';
        return new Intl.NumberFormat('ko-KR', {
            style: 'currency', currency: 'KRW', maximumFractionDigits: 0
        }).format(Number(value));
    },

    salaryCategoryLabel(category) {
        const c = this.salary.cats.find(x => x.key === category);
        return c ? c.label : category;
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
