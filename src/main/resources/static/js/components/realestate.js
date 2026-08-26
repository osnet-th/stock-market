/**
 * RealEstateComponent — 부동산 시장 데이터 대시보드.
 * 소유 프로퍼티: realestate
 *
 * 응답 어디에도 판단/추천/등급 라벨을 노출하지 않는다 (코드에서 signal/grade/recommend 키 미사용).
 */
const RealEstateComponent = {
    realestate: {
        loading: false,
        // 지역 필터
        sidos: [],
        selectedSidoCode: '',
        sigungus: [],
        selectedRegionCode: '',
        emds: [],
        selectedEmdCode: '',
        // 카테고리 탭 (T1~T11)
        categories: [
            { key: 'TRADE', label: '매매 거래' },
            { key: 'PRICE_INDEX', label: '매매 가격' },
            { key: 'RENT', label: '전월세' },
            { key: 'OFFICIAL_STATS', label: '공식 통계' },
            { key: 'SUPPLY', label: '공급' },
            { key: 'UNSOLD', label: '미분양' },
            { key: 'SUBSCRIPTION', label: '청약' },
            { key: 'GUARANTEE_RISK', label: '분양 리스크' },
            { key: 'SEOUL_LOCAL', label: '서울 특화' },
            { key: 'GYEONGGI_LOCAL', label: '경기 특화' },
            { key: 'COMPARISON', label: '지역 비교' },
            { key: 'SOURCES', label: '데이터 출처' }
        ],
        selectedCategory: 'TRADE',
        period: 'ONE_MONTH',
        // 응답
        summary: null,
        tab: null,
        comparison: null,
        sources: null,
        // 광역 가용성 (/api/realestate/sources/availability)
        // null = 미조회, {available, reason} = 조회 완료
        sidoAvailability: null,
        // 관심지역
        favorites: [],
        // 비교 지역
        compareTargets: [],
        // 관리자 수동 배치 트리거 상태
        adminBatch: {
            triggering: false,
            message: null,
            messageType: null  // 'success' | 'warning' | 'error'
        }
    },

    /**
     * 관리자 전용 — 부동산 일배치 수동 트리거.
     * 응답: 202 triggered (성공) / 409 rejected (다른 배치 in-flight) / 401·403 (권한 없음).
     */
    async triggerRealEstateAdminBatch() {
        if (this.realestate.adminBatch.triggering) return;
        this.realestate.adminBatch.triggering = true;
        this.realestate.adminBatch.message = null;
        try {
            const res = await API.triggerRealEstateBatch();
            const at = (res && res.triggeredAt) ? ` (${res.triggeredAt})` : '';
            this.realestate.adminBatch.message = `동기화가 시작되었습니다${at}. 완료까지 잠시 걸릴 수 있어요.`;
            this.realestate.adminBatch.messageType = 'success';
        } catch (e) {
            const status = e && e.status;
            if (status === 409) {
                this.realestate.adminBatch.message = '다른 배치가 이미 실행 중입니다. 잠시 후 다시 시도해 주세요.';
                this.realestate.adminBatch.messageType = 'warning';
            } else if (status === 401 || status === 403) {
                this.realestate.adminBatch.message = '관리자 권한이 필요합니다.';
                this.realestate.adminBatch.messageType = 'error';
            } else {
                this.realestate.adminBatch.message = (e && e.message) || '동기화 요청에 실패했습니다.';
                this.realestate.adminBatch.messageType = 'error';
            }
        } finally {
            this.realestate.adminBatch.triggering = false;
        }
    },

    initRealEstateCharts() {
        Object.defineProperty(this.realestate, '_chartInstances', {
            value: new Map(), writable: true, enumerable: false
        });
    },

    async loadRealEstateRegions() {
        try {
            const result = await API.getRealEstateRegions();
            this.realestate.sidos = (result && result.sidos) || [];
            if (this.realestate.sidos.length && !this.realestate.selectedSidoCode) {
                this.realestate.selectedSidoCode = this.realestate.sidos[0].sidoCode;
                this.onRealEstateSidoChange();
            }
        } catch (e) {
            console.error('부동산 지역 로드 실패:', e);
            this.realestate.sidos = [];
        }
    },

    onRealEstateSidoChange() {
        const sido = this.realestate.sidos.find(s => s.sidoCode === this.realestate.selectedSidoCode);
        this.realestate.sigungus = sido ? sido.sigungus : [];
        if (this.realestate.sigungus.length) {
            this.realestate.selectedRegionCode = this.realestate.sigungus[0].regionCode;
            this.onRealEstateRegionChange();
        } else {
            this.realestate.selectedRegionCode = '';
        }
    },

    async onRealEstateRegionChange() {
        const code = this.realestate.selectedRegionCode;
        this.realestate.selectedEmdCode = '';
        try {
            this.realestate.emds = await API.getRealEstateEmds(code) || [];
        } catch (e) {
            this.realestate.emds = [];
        }
        await this.refreshRealEstate();
    },

    async refreshRealEstate() {
        if (!this.realestate.selectedRegionCode) return;
        this.realestate.loading = true;
        try {
            await Promise.all([this.loadRealEstateSummary(), this.loadRealEstateActiveTab()]);
        } finally {
            this.realestate.loading = false;
        }
    },

    async loadRealEstateSummary() {
        try {
            this.realestate.summary = await API.getRealEstateSummary(this.realestate.selectedRegionCode);
        } catch (e) {
            console.error('부동산 요약 로드 실패:', e);
            this.realestate.summary = null;
        }
    },

    async loadRealEstateActiveTab() {
        const cat = this.realestate.selectedCategory;
        this.destroyRealEstateCharts();
        if (cat === 'COMPARISON') {
            await this.loadRealEstateComparison();
            return;
        }
        if (cat === 'SOURCES') {
            await this.loadRealEstateSources();
            return;
        }
        try {
            this.realestate.tab = await API.getRealEstateTab(
                this.realestate.selectedRegionCode, cat, this.realestate.period);
            this.$nextTick(() => this.renderRealEstateCharts());
        } catch (e) {
            console.error('부동산 탭 로드 실패:', e);
            this.realestate.tab = null;
        }
    },

    async loadRealEstateComparison() {
        const targets = [this.realestate.selectedRegionCode, ...this.realestate.compareTargets].filter(Boolean);
        if (targets.length < 2) {
            this.realestate.comparison = { rows: [] };
            return;
        }
        try {
            this.realestate.comparison = await API.getRealEstateComparison(
                targets, 'TRADE', this.realestate.period);
        } catch (e) {
            console.error('부동산 비교 로드 실패:', e);
            this.realestate.comparison = null;
        }
    },

    async loadRealEstateSources() {
        try {
            this.realestate.sources = await API.getRealEstateSources();
        } catch (e) {
            console.error('부동산 출처 메타 로드 실패:', e);
            this.realestate.sources = null;
        }
    },

    async selectRealEstateCategory(category) {
        this.realestate.selectedCategory = category;
        await this.loadRealEstateActiveTab();
    },

    async selectRealEstatePeriod(period) {
        this.realestate.period = period;
        await this.loadRealEstateActiveTab();
    },

    renderRealEstateCharts() {
        const tab = this.realestate.tab;
        if (!tab || !tab.cards) return;
        if (!this.realestate._chartInstances) this.initRealEstateCharts();

        tab.cards.forEach(card => {
            if (!card.history || card.history.length < 2) return;
            const canvasId = `re-chart-${card.indicatorCode}`;
            const canvas = document.getElementById(canvasId);
            if (!canvas) return;
            const labels = card.history.map(h => h.referenceText);
            const data = card.history.map(h => h.value);
            const chart = new Chart(canvas.getContext('2d'), {
                type: 'line',
                data: {
                    labels,
                    datasets: [{
                        label: card.displayName,
                        data,
                        borderColor: '#2563eb',
                        backgroundColor: 'rgba(37, 99, 235, 0.1)',
                        tension: 0.2,
                        fill: true
                    }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    plugins: { legend: { display: false } },
                    scales: { y: { beginAtZero: false } }
                }
            });
            this.realestate._chartInstances.set(canvasId, chart);
        });
    },

    destroyRealEstateCharts() {
        const map = this.realestate._chartInstances;
        if (map && typeof map.forEach === 'function') {
            map.forEach(c => { try { c && c.destroy(); } catch (e) { /* ignore */ } });
            map.clear();
        }
    },

    async loadRealEstateFavorites() {
        try {
            const result = await API.getRealEstateFavoriteRegions();
            this.realestate.favorites = (result && result.items) || [];
        } catch (e) {
            console.error('관심지역 로드 실패:', e);
            this.realestate.favorites = [];
        }
    },

    async toggleRealEstateFavorite() {
        if (!this.realestate.selectedRegionCode) return;
        const code = this.realestate.selectedRegionCode;
        const emd = this.realestate.selectedEmdCode || null;
        const exists = this.realestate.favorites.some(f =>
            f.regionCode === code && (f.emdCode || null) === emd);
        try {
            if (exists) {
                await API.removeRealEstateFavoriteRegion(code, emd);
            } else {
                await API.addRealEstateFavoriteRegion(code, emd);
            }
            await this.loadRealEstateFavorites();
        } catch (e) {
            console.error('관심지역 갱신 실패:', e);
        }
    },

    async openRealEstateFavorite(item) {
        this.realestate.selectedSidoCode = item.regionCode.substring(0, 2);
        const sido = this.realestate.sidos.find(s => s.sidoCode === this.realestate.selectedSidoCode);
        this.realestate.sigungus = sido ? sido.sigungus : [];
        this.realestate.selectedRegionCode = item.regionCode;
        this.realestate.selectedEmdCode = item.emdCode || '';
        try {
            this.realestate.emds = await API.getRealEstateEmds(item.regionCode) || [];
        } catch (e) {
            this.realestate.emds = [];
        }
        await this.refreshRealEstate();
    },

    isRealEstateFavoriteActive() {
        const code = this.realestate.selectedRegionCode;
        const emd = this.realestate.selectedEmdCode || null;
        return this.realestate.favorites.some(f =>
            f.regionCode === code && (f.emdCode || null) === emd);
    },

    async initRealEstate() {
        if (this.realestate._initialized) return;
        this.realestate._initialized = true;
        this.initRealEstateCharts();
        await this.loadRealEstateRegions();
        await this.loadRealEstateFavorites();
        await this.loadRealEstateAvailability();
    },

    async loadRealEstateAvailability() {
        try {
            const result = await API.getRealEstateSourcesAvailability();
            this.realestate.sidoAvailability = (result && result.reb) || null;
        } catch (e) {
            console.error('부동산 출처 가용성 로드 실패:', e);
            // 가용성 조회 실패 = 응답이 없으므로 "준비 중" 상태로 처리
            this.realestate.sidoAvailability = { available: false, reason: 'availability-check-failed' };
        }
    },

    // 광역(SIDO) 섹션 카드 — regionLevel === 'SIDO' 카드만 필터
    sidoCardsFor(cards) {
        return (cards || []).filter(c => c && c.regionLevel === 'SIDO');
    },

    // 시군구(SIGUNGU) 섹션 카드 — regionLevel === 'SIGUNGU' 또는 미지정(default) 카드
    sigunguCardsFor(cards) {
        return (cards || []).filter(c => !c || c.regionLevel !== 'SIDO');
    },

    isSidoAvailable() {
        return !!(this.realestate.sidoAvailability && this.realestate.sidoAvailability.available);
    },

    sidoUnavailableReason() {
        const a = this.realestate.sidoAvailability;
        if (!a || a.available) return '';
        switch (a.reason) {
            case 'availability-check-failed':
                return '가용성 점검 일시 실패 — 잠시 후 다시 시도해주세요.';
            case 'unavailable':
            default:
                return '광역 통계는 데이터 소스 준비가 완료되면 노출됩니다.';
        }
    },

    formatRealEstateValue(value, unit) {
        if (value === null || value === undefined) return '—';
        const num = typeof value === 'number' ? value : parseFloat(value);
        if (Number.isNaN(num)) return '—';
        const formatted = num.toLocaleString('ko-KR', { maximumFractionDigits: 2 });
        return unit ? `${formatted} ${unit}` : formatted;
    },

    formatRealEstateChangeRate(rate) {
        if (rate === null || rate === undefined) return '';
        const num = typeof rate === 'number' ? rate : parseFloat(rate);
        if (Number.isNaN(num)) return '';
        const sign = num > 0 ? '+' : '';
        return `${sign}${num.toFixed(2)}%`;
    }
};