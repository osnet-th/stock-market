/**
 * HomeComponent - 홈 대시보드 요약
 * 소유 프로퍼티: homeSummary
 */
const HomeComponent = {
    homeSummary: {
        keywordCount: 0,
        activeKeywordCount: 0,
        domesticKeywordCount: 0,
        internationalKeywordCount: 0,
        ecosCategories: [],
        globalCategories: [],
        enrichedFavorites: null,
        recentUpdates: null,
        allocationStatus: null,
        dashboardLoading: false,
        // 우측 패널용 (#114) — 만기 알림 계산에 항목 원본이 필요하다
        portfolioItems: [],
        newsFeed: null,
    },

    /**
     * 홈 대시보드 화면 상태 (#114)
     * - range: 비교 차트·카드 기간 변화율의 조회 구간
     * - mode: 비교 차트 정규화 방식 (pct = 시작점 0% 기준, raw = 지표별 자체 스케일)
     * - selected: 비교 대상 indicatorCode. 최대 3개, 순서가 곧 색 순서
     */
    homeDashboard: {
        ranges: [
            { key: '1M', label: '1M', points: 22 },
            { key: '3M', label: '3M', points: 63 },
            { key: '6M', label: '6M', points: 126 },
            { key: '1Y', label: '1Y', points: 252 },
        ],
        range: '6M',
        mode: 'pct',
        filter: '전체',
        selected: [],
        // 선택 순서 = 색 순서. 목업과 동일하게 BLUE → RED → AMBER
        colors: ['#1f4f9e', '#c02a22', '#b5854a'],
        maxCompare: 3,
        // Chart.js 인스턴스 보관 (dashboardSummary._chartInstance 와 같은 컨벤션)
        _compareChart: null,
    },

    setHomeRange(rangeKey) {
        this.homeDashboard.range = rangeKey;
        this.renderHomeCompareChart();
    },

    toggleHomeMode() {
        this.homeDashboard.mode = this.homeDashboard.mode === 'pct' ? 'raw' : 'pct';
        this.renderHomeCompareChart();
    },

    getHomeModeLabel() {
        return this.homeDashboard.mode === 'pct' ? '변화율(%) 기준' : '자체 스케일 기준';
    },

    getHomeModeNote() {
        return this.homeDashboard.mode === 'pct'
            ? '기간 시작점을 0%로 맞춰 단위가 다른 지표도 같은 축에서 비교합니다'
            : '각 지표를 자기 범위에 맞춰 정규화해 모양만 비교합니다';
    },

    /**
     * 현재 range 의 조회 포인트 수. history 가 짧으면 있는 만큼만 쓴다.
     */
    getHomeRangePoints() {
        const found = this.homeDashboard.ranges.find(r => r.key === this.homeDashboard.range);
        return found ? found.points : 126;
    },

    getHomeAsOfLabel() {
        const now = new Date();
        const days = ['일', '월', '화', '수', '목', '금', '토'];
        const pad = (n) => String(n).padStart(2, '0');
        return `${now.getFullYear()}.${pad(now.getMonth() + 1)}.${pad(now.getDate())}`
            + ` (${days[now.getDay()]}) ${pad(now.getHours())}:${pad(now.getMinutes())} 기준`;
    },

    /**
     * 관심 지표(국내 우선, 부족하면 글로벌)에서 직전 대비 변화가 계산되는 항목 최대 3종.
     * 관심 지표가 없으면 빈 배열 → 브리핑 줄 자체가 숨는다.
     */
    getHomeBriefing() {
        const fav = this.homeSummary.enrichedFavorites;
        if (!fav) return [];

        const pool = [
            ...(fav.ecos || []).map(c => ({
                key: 'ECOS-' + c.indicatorCode,
                name: c.keystatName,
                current: c.dataValue,
                previous: c.previousDataValue,
                failed: false,
            })),
            ...(fav.global || []).map(c => ({
                key: 'GLOBAL-' + c.indicatorCode,
                name: c.countryName + ' ' + c.indicatorTypeDisplayName,
                current: c.dataValue,
                previous: c.previousDataValue,
                failed: !!c.failed,
            })),
        ];

        return pool
            .filter(x => !x.failed && this._isHomeNumeric(x.current) && this._isHomeNumeric(x.previous))
            .slice(0, 3)
            .map(x => {
                const diff = Number(x.current) - Number(x.previous);
                const decimals = this._homeDeltaDecimals(x.current);
                return {
                    key: x.key,
                    name: x.name,
                    delta: (diff >= 0 ? '+' : '−') + Math.abs(diff).toFixed(decimals),
                    direction: diff > 0 ? 'up' : diff < 0 ? 'down' : 'flat',
                };
            });
    },

    _isHomeNumeric(value) {
        if (value === null || value === undefined || value === '') return false;
        return !Number.isNaN(Number(value));
    },

    /**
     * 원본 값의 소수 자릿수를 따라간다. 지표마다 정밀도가 달라 일괄 고정하면
     * 기준금리(2.75) 와 KORIBOR(2.984) 가 같은 자릿수로 뭉개진다.
     */
    _homeDeltaDecimals(sample) {
        const text = String(sample);
        const dot = text.indexOf('.');
        if (dot < 0) return 0;
        return Math.min(text.length - dot - 1, 3);
    },

    // ==================== 지표 비교 보기 (#114 Phase 2) ====================

    /**
     * 관심 지표(국내 + 글로벌)를 비교 대상 공통 형태로 펼친다.
     * key 는 sourceType 을 접두어로 붙여 국내/글로벌 코드 충돌을 막는다.
     */
    getHomeCompareCandidates() {
        const fav = this.homeSummary.enrichedFavorites;
        if (!fav) return [];
        return [
            ...(fav.ecos || []).map(c => ({
                key: 'ECOS-' + c.indicatorCode,
                sourceType: 'ECOS',
                indicatorCode: c.indicatorCode,
                name: c.keystatName,
                category: c.className,
                unit: '',
                history: c.history || [],
                failed: false,
            })),
            ...(fav.global || []).map(c => ({
                key: 'GLOBAL-' + c.indicatorCode,
                sourceType: 'GLOBAL',
                indicatorCode: c.indicatorCode,
                name: c.countryName + ' ' + c.indicatorTypeDisplayName,
                category: c.countryName + ' · ' + (c.indicatorTypeDisplayName || ''),
                unit: c.unit || '',
                history: c.history || [],
                failed: !!c.failed,
            })),
        ];
    },

    getHomeCompareKey(sourceType, indicatorCode) {
        return sourceType + '-' + indicatorCode;
    },

    isHomeCompareSelected(sourceType, indicatorCode) {
        return this.homeDashboard.selected.includes(this.getHomeCompareKey(sourceType, indicatorCode));
    },

    /**
     * 선택 색. 미선택이면 null — 카드가 기본 테두리를 쓰도록 한다.
     */
    getHomeCompareColor(sourceType, indicatorCode) {
        const idx = this.homeDashboard.selected.indexOf(this.getHomeCompareKey(sourceType, indicatorCode));
        return idx < 0 ? null : this.homeDashboard.colors[idx];
    },

    /**
     * 비교 대상 토글. 이미 3개면 가장 오래된 선택을 밀어내고 추가한다(목업 동작).
     */
    toggleHomeCompare(sourceType, indicatorCode) {
        const key = this.getHomeCompareKey(sourceType, indicatorCode);
        const st = this.homeDashboard;
        if (st.selected.includes(key)) {
            st.selected = st.selected.filter(k => k !== key);
        } else {
            st.selected = st.selected.length >= st.maxCompare
                ? [...st.selected.slice(1), key]
                : [...st.selected, key];
        }
        this.renderHomeCompareChart();
    },

    removeHomeCompare(key) {
        this.homeDashboard.selected = this.homeDashboard.selected.filter(k => k !== key);
        this.renderHomeCompareChart();
    },

    /**
     * 선택된 지표의 계산 결과. 칩 렌더와 차트가 같은 배열을 쓴다.
     * history 가 비었거나 숫자가 하나도 없으면 제외한다 — 차트에 빈 선이 생기지 않도록.
     */
    getHomeCompareSeries() {
        const candidates = this.getHomeCompareCandidates();
        const points = this.getHomeRangePoints();

        return this.homeDashboard.selected.map((key, idx) => {
            const found = candidates.find(c => c.key === key);
            if (!found) return null;

            const sliced = (found.history || []).slice(-points);
            const values = sliced.map(p => {
                const num = parseFloat(String(p.dataValue ?? '').replace(/,/g, ''));
                return Number.isFinite(num) ? num : null;
            });
            if (!values.some(v => v !== null)) return null;

            const last = [...values].reverse().find(v => v !== null);
            return {
                key,
                name: found.name,
                unit: found.unit,
                color: this.homeDashboard.colors[idx],
                labels: sliced.map(p => p.snapshotDate),
                values,
                lastValue: last,
            };
        }).filter(Boolean);
    },

    /**
     * 정규화.
     * - pct: 기간 시작점을 0% 로 맞춰 단위가 다른 지표를 같은 축에 올린다
     * - raw: 지표별 min~max 를 -50~+50 으로 눌러 모양만 비교한다
     * 기준값이 0 이면 변화율이 발산하므로 pct 라도 raw 로 떨어뜨린다.
     */
    normalizeHomeSeries(values, mode) {
        const nums = values.filter(v => v !== null);
        if (nums.length === 0) return values;

        if (mode === 'pct') {
            const base = nums[0];
            if (base !== 0) {
                return values.map(v => (v === null ? null : ((v - base) / Math.abs(base)) * 100));
            }
        }
        const lo = Math.min(...nums);
        const hi = Math.max(...nums);
        const span = (hi - lo) || 1;
        return values.map(v => (v === null ? null : ((v - lo) / span) * 100 - 50));
    },

    destroyHomeCompareChart() {
        const chart = this.homeDashboard._compareChart;
        if (chart) {
            try { chart.destroy(); } catch (_) { /* noop */ }
            this.homeDashboard._compareChart = null;
        }
    },

    /**
     * 비교 차트 렌더. 선택·기간·모드가 바뀔 때마다 destroy 후 재생성한다.
     * canvas 가 없으면(다른 화면·미마운트) 조용히 빠진다.
     */
    renderHomeCompareChart() {
        // Alpine 이 x-show 를 반영한 다음에 그려야 canvas 크기가 잡힌다
        const draw = () => {
            const canvas = document.getElementById('homeCompareChart');
            if (!canvas || typeof Chart === 'undefined') return;

            this.destroyHomeCompareChart();

            const series = this.getHomeCompareSeries();
            if (series.length === 0) return;

            const mode = this.homeDashboard.mode;
            // 주기가 다른 지표(일별 vs 월별)가 섞이면 길이가 다르다. 인덱스로 그냥 붙이면
            // 짧은 쪽이 왼쪽에만 그려져 시점이 어긋난다 → 각 지표를 전 구간에 펴서 모양을 비교한다(목업 방식).
            // 대신 툴팁은 공유 라벨이 아니라 각 지표의 실제 날짜를 쓴다.
            const width = series.reduce((a, s) => Math.max(a, s.values.length), 0);
            const longest = series.find(s => s.values.length === width) || series[0];
            const stretch = (arr) => {
                if (arr.length === width) return arr.slice();
                if (arr.length <= 1) return Array.from({ length: width }, () => arr[0] ?? null);
                return Array.from({ length: width }, (_, i) =>
                    arr[Math.round((i * (arr.length - 1)) / (width - 1))]);
            };
            const stretched = series.map(s => ({
                ...s,
                values: stretch(s.values),
                labels: stretch(s.labels),
            }));

            const chart = new Chart(canvas, {
                type: 'line',
                data: {
                    labels: longest.labels,
                    datasets: stretched.map(s => ({
                        label: s.name,
                        data: this.normalizeHomeSeries(s.values, mode),
                        borderColor: s.color,
                        borderWidth: 2,
                        pointRadius: 0,
                        pointHoverRadius: 4,
                        tension: 0.25,
                        fill: false,
                        spanGaps: true,
                    })),
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    animation: false,
                    interaction: { mode: 'index', intersect: false },
                    plugins: {
                        legend: { display: false },
                        tooltip: {
                            callbacks: {
                                // 지표마다 주기가 달라 공유 제목이 거짓이 된다 → 각 줄에 자기 날짜를 붙인다
                                title: () => '',
                                label: (ctx) => {
                                    const s = stretched[ctx.datasetIndex];
                                    const raw = s ? s.values[ctx.dataIndex] : null;
                                    const at = s ? s.labels[ctx.dataIndex] : '';
                                    if (raw === null || raw === undefined) return `${ctx.dataset.label}: 데이터 없음`;
                                    return `${ctx.dataset.label} ${at ? '(' + at + ')' : ''}: ${raw}${s.unit ? ' ' + s.unit : ''}`;
                                },
                            },
                        },
                    },
                    scales: {
                        x: {
                            ticks: { maxRotation: 0, autoSkip: true, maxTicksLimit: 6, font: { size: 10 } },
                            grid: { display: false },
                        },
                        y: {
                            ticks: {
                                maxTicksLimit: 5,
                                font: { size: 10 },
                                callback: (v) => (mode === 'pct' ? (v >= 0 ? '+' : '') + Number(v).toFixed(1) + '%' : ''),
                            },
                            grid: { color: 'rgba(0,0,0,.05)' },
                        },
                    },
                },
            });
            this.homeDashboard._compareChart = chart;
        };

        if (this.$nextTick) this.$nextTick(draw);
        else draw();
    },

    // ==================== 우측 패널 (#114 Phase 4) ====================

    /**
     * "확인이 필요한 것" 알림. 전부 이미 로드된 데이터로 계산하며 신규 조회는 없다.
     * 배분 이탈(RED) → 지표 추세(BLUE) → 만기 임박(AMBER) 순으로 보여준다.
     */
    getHomeAlerts() {
        return [
            ...this._homeAllocationAlerts(),
            ...this._homeTrendAlerts(),
            ...this._homeMaturityAlerts(),
        ];
    },

    _homeAllocationAlerts() {
        const status = this.homeSummary.allocationStatus;
        if (!status || !status.configured) return [];

        const exceeded = [
            ...(status.buckets || []),
            ...(status.investAssets || []),
        ].filter(b => b.bandExceeded);
        if (exceeded.length === 0) return [];

        // 편차가 가장 큰 항목 하나만 알린다 — 패널이 배분 알림으로 도배되지 않도록
        const worst = exceeded.reduce((a, b) =>
            Math.abs(Number(b.deviationPctPoint)) > Math.abs(Number(a.deviationPctPoint)) ? b : a);
        const name = worst.bucketName || worst.assetTypeName || '자산군';
        const over = Number(worst.deviationPctPoint) > 0;

        return [{
            key: 'alloc',
            tone: 'red',
            title: '포트폴리오 배분 밴드 초과',
            body: `${name} ${Number(worst.currentRatio).toFixed(1)}% (목표 ${Number(worst.targetRatio).toFixed(0)}%,`
                + ` 밴드 ±${Number(status.bandPctPoint).toFixed(1)}%p).`
                + ` ${this.formatKrwCompact(Math.abs(Number(worst.deviationAmount)))} ${over ? '초과' : '부족'}합니다.`,
            cta: '포트폴리오 열기 →',
            go: () => this.navigateTo('portfolio'),
        }];
    },

    /**
     * 관심 지표 시계열이 같은 방향으로 연속 이동했는지 본다.
     * 임계는 4회 — 일별 지표에서 3회는 흔해 알림이 시끄러워진다.
     */
    _homeTrendAlerts() {
        const STREAK = 4;
        const candidates = this.getHomeCompareCandidates().filter(c => !c.failed);

        for (const c of candidates) {
            const values = (c.history || [])
                .map(p => parseFloat(String(p.dataValue ?? '').replace(/,/g, '')))
                .filter(v => Number.isFinite(v));
            if (values.length < STREAK + 1) continue;

            const tail = values.slice(-(STREAK + 1));
            const diffs = tail.slice(1).map((v, i) => v - tail[i]);
            const allUp = diffs.every(d => d > 0);
            const allDown = diffs.every(d => d < 0);
            if (!allUp && !allDown) continue;

            return [{
                key: 'trend-' + c.key,
                tone: 'blue',
                title: `${c.name} ${STREAK}회 연속 ${allUp ? '상승' : '하락'}`,
                body: '보유 자산 평가액과 재예치 조건에 영향이 있을 수 있습니다.',
                cta: '지표 비교에 추가 →',
                go: () => this.toggleHomeCompare(c.sourceType, c.indicatorCode),
            }];
        }
        return [];
    },

    /**
     * 예금·적금·채권 만기 임박.
     * 임계 120일 — 목업이 D-102 를 임박으로 보여주므로 3개월(90일)로는 그 사례가 걸러진다.
     * 재예치 조건 비교는 넉넉히 앞서 시작하는 게 실용적이기도 하다.
     */
    _homeMaturityAlerts() {
        const THRESHOLD_DAYS = 120;
        const today = new Date();
        today.setHours(0, 0, 0, 0);

        const upcoming = (this.homeSummary.portfolioItems || [])
            .map(item => {
                const maturity = item.cashDetail?.maturityDate || item.bondDetail?.maturityDate;
                if (!maturity) return null;
                const parts = String(maturity).split('-').map(Number);
                if (parts.length !== 3 || parts.some(Number.isNaN)) return null;
                const dday = Math.round((new Date(parts[0], parts[1] - 1, parts[2]) - today) / 86400000);
                if (dday < 0 || dday > THRESHOLD_DAYS) return null;
                return { item, maturity, dday };
            })
            .filter(Boolean)
            .sort((a, b) => a.dday - b.dday);

        if (upcoming.length === 0) return [];

        const nearest = upcoming[0];
        const rate = nearest.item.cashDetail?.interestRate ?? nearest.item.bondDetail?.couponRate;
        const more = upcoming.length > 1 ? ` · 외 ${upcoming.length - 1}건` : '';

        return [{
            key: 'maturity',
            tone: 'amber',
            title: `${nearest.item.itemName} 만기 D-${nearest.dday}`,
            body: `${nearest.maturity}${rate ? ' · 연 ' + Number(rate).toFixed(2) + '%' : ''}${more}.`
                + ' 만기 후 재예치 조건을 비교해 두세요.',
            cta: '포트폴리오 열기 →',
            go: () => this.navigateTo('portfolio'),
        }];
    },

    /**
     * 알림 색 토큰. 목업의 RED/BLUE/AMBER 를 Tailwind 클래스로 맞춘다.
     */
    getHomeAlertTone(tone) {
        // 목업 실측 색 (RED #c02a22 / BLUE #1f4f9e / AMBER #b5854a 와 각 tint)
        if (tone === 'red') return { bg: 'var(--dc-red-tint)', accent: 'var(--dc-red)' };
        if (tone === 'blue') return { bg: 'var(--dc-blue-tint)', accent: 'var(--dc-blue)' };
        return { bg: 'var(--dc-amber-tint)', accent: 'var(--dc-amber)' };
    },

    /**
     * 오늘이면 시:분, 어제면 '어제', 그 밖은 월.일.
     */
    formatHomeNewsTime(iso) {
        if (!iso) return '';
        const at = new Date(iso);
        if (Number.isNaN(at.getTime())) return '';
        const pad = (n) => String(n).padStart(2, '0');
        const startOfToday = new Date();
        startOfToday.setHours(0, 0, 0, 0);
        // 날짜 경계로 비교해야 한다. 타임스탬프끼리 빼면 어제 늦은 기사가 24시간 안이라 '오늘'로 잡힌다
        const startOfArticleDay = new Date(at);
        startOfArticleDay.setHours(0, 0, 0, 0);
        const diffDays = Math.round((startOfToday - startOfArticleDay) / 86400000);
        if (diffDays <= 0) return `${pad(at.getHours())}:${pad(at.getMinutes())}`;
        if (diffDays === 1) return '어제';
        return `${at.getMonth() + 1}.${at.getDate()}`;
    },

    /**
     * 월급 사용 비율 — 도넛 대신 스택 바. 금액 0인 항목은 제외한다.
     */
    getHomeSalaryBreakdown() {
        const salary = this.dashboardSummary?.salary;
        const total = Number(salary?.totalSpending || 0);
        if (!salary || !Array.isArray(salary.spendings) || total === 0) return [];

        const palette = ['#1f4f9e', '#c02a22', '#4d7f9e', '#b5854a', '#b6bbc2'];
        return salary.spendings
            .filter(s => s && Number(s.amount) > 0)
            .sort((a, b) => Number(b.amount) - Number(a.amount))
            .map((s, i) => {
                const amount = Number(s.amount);
                return {
                    key: s.category,
                    name: s.categoryLabel || this.salaryCategoryLabel?.(s.category) || s.category,
                    amount,
                    amountLabel: this.formatKrwCompact(amount),
                    pct: (amount / total) * 100,
                    pctLabel: Math.round((amount / total) * 100) + '%',
                    color: palette[i % palette.length],
                };
            });
    },

    // ==================== 지표 카드 그리드 (#114 Phase 3) ====================

    /**
     * 관심 지표에 실제로 존재하는 분류만 필터 칩으로 만든다.
     */
    getHomeCategories() {
        const fav = this.homeSummary.enrichedFavorites;
        const names = (fav?.ecos || []).map(c => c.className).filter(Boolean);
        return ['전체', ...Array.from(new Set(names))];
    },

    setHomeFilter(category) {
        this.homeDashboard.filter = category;
    },

    /**
     * 카드 뷰모델. 국내/글로벌이 같은 카드 형태를 쓰므로 한 함수에서 만든다.
     * 편집 중에는 필터를 무시한다 — 숨겨진 카드가 순서 페이로드에서 빠지면 안 되기 때문.
     */
    getHomeIndicatorCards(sourceType) {
        const fav = this.homeSummary.enrichedFavorites;
        if (!fav) return [];
        const bucket = (sourceType === 'ECOS' ? fav.ecos : fav.global) || [];
        const editing = this.isEditing(sourceType);
        const filter = this.homeDashboard.filter;

        return bucket
            .filter(c => {
                if (sourceType !== 'ECOS' || editing || filter === '전체') return true;
                return c.className === filter;
            })
            .map(c => this._toHomeCard(c, sourceType));
    },

    _toHomeCard(c, sourceType) {
        const isEcos = sourceType === 'ECOS';
        const selected = this.isHomeCompareSelected(sourceType, c.indicatorCode);
        const color = this.getHomeCompareColor(sourceType, c.indicatorCode);
        const current = this._isHomeNumeric(c.dataValue) ? Number(c.dataValue) : null;
        const previous = this._isHomeNumeric(c.previousDataValue) ? Number(c.previousDataValue) : null;

        let delta = null;
        let direction = 'flat';
        if (current !== null && previous !== null) {
            const diff = current - previous;
            direction = diff > 0 ? 'up' : diff < 0 ? 'down' : 'flat';
            delta = (diff >= 0 ? '▲ +' : '▼ −') + Math.abs(diff).toFixed(this._homeDeltaDecimals(c.dataValue));
        }

        const sliced = (c.history || []).slice(-this.getHomeRangePoints());
        const values = sliced
            .map(p => parseFloat(String(p.dataValue ?? '').replace(/,/g, '')))
            .filter(v => Number.isFinite(v));

        // 기간 라벨은 '실제로 덮은 구간'을 말해야 한다.
        // 수집 시작이 얼마 안 된 지표는 6M 을 골라도 history 가 몇 주치뿐인데,
        // 그대로 '6M −13.7%' 라고 쓰면 6개월 변화로 읽힌다 (실측에서 코스피가 그랬다).
        let periodLabel = null;
        let periodTitle = null;
        if (values.length >= 2 && values[0] !== 0) {
            const rate = ((values[values.length - 1] - values[0]) / Math.abs(values[0])) * 100;
            const pct = (rate >= 0 ? '+' : '−') + Math.abs(rate).toFixed(1) + '%';
            const coversFullRange = sliced.length >= this.getHomeRangePoints();
            const from = this._homeFormatRefDate(sliced[0]?.snapshotDate);
            periodLabel = coversFullRange || !from
                ? this.homeDashboard.range + ' ' + pct
                : from + '~ ' + pct;
            periodTitle = from
                ? `${from} ~ ${this._homeFormatRefDate(sliced[sliced.length - 1]?.snapshotDate)} 기준`
                + (coversFullRange ? '' : ` · 선택한 ${this.homeDashboard.range} 보다 짧은 구간입니다`)
                : null;
        }

        // 미선택 카드는 일간 방향 색으로 스파크라인을 그린다 (목업 동작)
        const sparkColor = color || (direction === 'down' ? '#1f4f9e' : '#c02a22');

        return {
            key: sourceType + '-' + c.indicatorCode,
            raw: c,
            sourceType,
            indicatorCode: c.indicatorCode,
            name: isEcos ? c.keystatName : c.countryName,
            category: isEcos ? c.className : c.indicatorTypeDisplayName,
            value: c.hasData ? c.dataValue : null,
            unit: isEcos ? '' : (c.unit || ''),
            date: this._homeRefDate(sliced) || this._homeFormatRefDate(c.cycle) || '',
            delta,
            direction,
            periodLabel,
            periodTitle,
            hasData: !!c.hasData,
            failed: !!c.failed,
            refreshable: !!c.refreshable,
            selected,
            color: sparkColor,
            borderColor: selected ? sparkColor : 'rgba(0,0,0,.08)',
            bgColor: selected ? this._homeTintFor(sparkColor) : '#ffffff',
            starColor: selected ? sparkColor : '#d7dbe0',
            hasSeries: values.length >= 2,
            spark: this.buildHomeSparkline(values, sparkColor),
        };
    },

    /**
     * 카드 우하단 기준일. 목업은 날짜(08.07)를 쓰는데 응답의 `cycle` 은 주기 라벨(일/월)이라
     * history 의 마지막 스냅샷 날짜를 쓴다. 지표 주기마다 형식이 달라 자릿수로 분기한다.
     */
    _homeRefDate(history) {
        if (!history || history.length === 0) return null;
        return this._homeFormatRefDate(history[history.length - 1].snapshotDate);
    },

    /**
     * 기준일 포맷. ECOS 는 `cycle` 에도 20260815 같은 날짜 문자열이 들어와서
     * history 와 같은 규칙으로 다듬어야 raw 8자리가 그대로 노출되지 않는다.
     */
    _homeFormatRefDate(value) {
        const raw = String(value ?? '').trim();
        if (!raw) return null;
        const digits = raw.replace(/[^0-9]/g, '');
        if (digits.length === 8) return digits.slice(4, 6) + '.' + digits.slice(6, 8);   // YYYYMMDD → MM.DD
        if (digits.length === 6) return digits.slice(0, 4) + '.' + digits.slice(4, 6);   // YYYYMM → YYYY.MM
        return raw;
    },

    _homeTintFor(color) {
        if (color === '#1f4f9e') return '#f4f7fc';
        if (color === '#c02a22') return '#fdf5f4';
        if (color === '#b5854a') return '#fbf8f3';
        return '#ffffff';
    },

    /**
     * 카드마다 하나씩 그려지므로 Chart.js 대신 인라인 SVG path 문자열로 만든다.
     * viewBox 200x44 고정, 값이 2개 미만이면 null 을 돌려 카드가 자리만 비운다.
     */
    buildHomeSparkline(values, color) {
        if (!values || values.length < 2) return null;
        const W = 200, H = 44, PAD = 5;
        const lo = Math.min(...values);
        const hi = Math.max(...values);
        const span = (hi - lo) || 1;
        const pts = values.map((v, i) => [
            (i / (values.length - 1)) * W,
            H - PAD - ((v - lo) / span) * (H - PAD * 2),
        ]);
        const line = pts.map((p, i) => (i ? 'L' : 'M') + p[0].toFixed(1) + ' ' + p[1].toFixed(1)).join(' ');
        const last = pts[pts.length - 1];
        return {
            line,
            area: line + ` L${W} ${H} L0 ${H} Z`,
            dotX: last[0].toFixed(1),
            dotY: last[1].toFixed(1),
            color,
        };
    },

    /**
     * 포트폴리오 요약 카드 노출 조건 — 목표 배분이 설정되고 평가액이 있을 때만.
     * 목업에 미설정 상태가 없어 카드를 숨기고 사이드바 메뉴로 유도한다.
     */
    hasHomePortfolioSummary() {
        const status = this.homeSummary.allocationStatus;
        return !!(status && status.configured && Number(status.totalEvaluated) > 0);
    },

    getHomeAllocationBucket(bucketKey) {
        const status = this.homeSummary.allocationStatus;
        if (!status || !status.configured || !status.buckets) return null;
        return status.buckets.find((b) => b.bucket === bucketKey) || null;
    },

    hasHomeAllocationWarning() {
        const status = this.homeSummary.allocationStatus;
        if (!status || !status.configured) return false;
        const bucketExceeded = (status.buckets || []).some((b) => b.bandExceeded);
        const assetExceeded = (status.investAssets || []).some((a) => a.bandExceeded);
        return bucketExceeded || assetExceeded;
    },

    async loadHomeSummary() {
        this.homeSummary.dashboardLoading = true;
        try {
            const results = await Promise.allSettled([
                this.checkLoggedIn() ? API.getKeywords(this.auth.userId) : Promise.resolve(null),
                API.getEcosCategories(),
                API.getGlobalCategories(),
                this.checkLoggedIn() ? API.getPortfolioItems(this.auth.userId) : Promise.resolve(null),
                this.checkLoggedIn() ? API.getEnrichedFavorites() : Promise.resolve(null),
                API.getRecentUpdates(),
                this.checkLoggedIn() ? API.getAllocationStatus(this.auth.userId) : Promise.resolve(null),
                this.checkLoggedIn() ? API.getKeywordNewsFeed(this.auth.userId, 5) : Promise.resolve(null)
            ]);

            // 키워드
            if (results[0].status === 'fulfilled' && results[0].value) {
                const allKeywords = results[0].value;
                this.homeSummary.keywordCount = allKeywords.length;
                this.homeSummary.activeKeywordCount = allKeywords.filter(k => k.active).length;
                this.homeSummary.domesticKeywordCount = allKeywords.filter(k => k.region === 'DOMESTIC').length;
                this.homeSummary.internationalKeywordCount = allKeywords.filter(k => k.region === 'INTERNATIONAL').length;
            }

            // ECOS
            if (results[1].status === 'fulfilled') {
                this.homeSummary.ecosCategories = results[1].value || [];
            }

            // 글로벌
            if (results[2].status === 'fulfilled') {
                this.homeSummary.globalCategories = results[2].value || [];
            }

            // 포트폴리오
            if (results[3].status === 'fulfilled' && results[3].value) {
                const portfolioItems = results[3].value;
                this.homeSummary.portfolioItems = portfolioItems;
                this.homeSummary.portfolioItemCount = portfolioItems.length;
                this.homeSummary.portfolioNewsEnabledCount = portfolioItems.filter(item => item.newsEnabled).length;
                const typeCounts = {};
                const assetTypeConfig = this.assetTypeConfig;
                portfolioItems.forEach(item => {
                    const label = assetTypeConfig[item.assetType]?.label || item.assetType;
                    typeCounts[label] = (typeCounts[label] || 0) + 1;
                });
                this.homeSummary.portfolioTypeSummary = Object.keys(typeCounts)
                    .map(label => label + ' ' + typeCounts[label] + '건')
                    .join(' · ');
            }

            // 관심 지표 (enriched)
            if (results[4].status === 'fulfilled' && results[4].value) {
                this.homeSummary.enrichedFavorites = results[4].value;
            }

            // 최근 업데이트
            if (results[5].status === 'fulfilled' && results[5].value) {
                this.homeSummary.recentUpdates = results[5].value;
            }

            // 목표 자산 배분 현황 (실패 시 카드가 기존 개수 표시로 동작)
            if (results[6].status === 'fulfilled' && results[6].value) {
                this.homeSummary.allocationStatus = results[6].value;
            }

            // 키워드 뉴스 피드 (#114) — 실패해도 패널만 빈 상태로 떨어진다
            if (results[7].status === 'fulfilled' && results[7].value) {
                this.homeSummary.newsFeed = results[7].value;
            }
        } catch (e) {
            console.error('홈 요약 로드 실패:', e);
        } finally {
            this.homeSummary.dashboardLoading = false;
            // 관심 지표가 새로 들어왔으므로 비교 차트를 다시 그린다.
            // 선택이 비어 있으면 renderHomeCompareChart 가 조용히 빠진다.
            this.renderHomeCompareChart();
        }
    },

    /**
     * 관심 지표 해제 후 대시보드 카드 즉시 제거
     */
    async removeDashboardFavorite(sourceType, indicatorCode) {
        await this.toggleFavorite(sourceType, indicatorCode);
        // 비교 선택에 남아 있으면 3칸 중 하나를 stale 키가 차지하게 된다 (#114)
        this.removeHomeCompare(this.getHomeCompareKey(sourceType, indicatorCode));
        // enrichedFavorites에서도 즉시 제거
        if (this.homeSummary.enrichedFavorites) {
            if (sourceType === 'ECOS') {
                this.homeSummary.enrichedFavorites.ecos =
                    this.homeSummary.enrichedFavorites.ecos.filter(f => f.indicatorCode !== indicatorCode);
            } else {
                this.homeSummary.enrichedFavorites.global =
                    this.homeSummary.enrichedFavorites.global.filter(f => f.indicatorCode !== indicatorCode);
            }
        }
        // 방금 해제한 지표가 현재 필터의 마지막 항목이었으면 칩이 사라져 빈 목록에 갇힌다 (#114 review L3)
        if (!this.getHomeCategories().includes(this.homeDashboard.filter)) {
            this.homeDashboard.filter = '전체';
        }
    }
};