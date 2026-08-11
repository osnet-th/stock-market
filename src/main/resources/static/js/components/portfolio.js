/**
 * PortfolioComponent - 포트폴리오 CRUD, 주가검색, 자산배분, 뉴스, 매수이력
 * 소유 프로퍼티: portfolio (financial 관련 상태 포함)
 */
const PortfolioComponent = {
    portfolio: {
        items: [],
        allocation: [],
        allocationStatus: null,
        allocationTargetModal: {
            show: false,
            saving: false,
            form: { safeRatio: '', bandPctPoint: '5', assets: [] }
        },
        loading: false,
        _initialized: false,
        showAddModal: false,
        showEditModal: false,
        editingItem: null,
        selectedAssetType: null,
        addForm: {},
        editForm: {},
        stockSearch: {
            query: '',
            results: [],
            loading: false,
            selected: null,
            debounceTimer: null
        },
        stockPrices: {},
        expandedSections: {},
        showPurchaseModal: false,
        purchaseItem: null,
        deleteConfirm: { show: false },
        purchaseForm: { quantity: '', purchasePrice: '' },
        purchaseHistories: [],
        editingHistory: null,
        editHistoryForm: { quantity: '', purchasePrice: '', purchasedAt: '', memo: '', fxRate: '' },
        showDepositModal: false,
        depositItem: null,
        depositForm: { depositDate: '', amount: '', units: '', memo: '' },
        depositHistories: [],
        editingDeposit: null,
        editDepositForm: { depositDate: '', amount: '', units: '', memo: '' },
        depositReminder: { show: false, items: [], snoozeChecked: false },
        // 매도 상태
        activeTab: 'holdings',
        showSaleModal: false,
        saleItem: null,
        saleForm: { quantity: '', salePrice: '', soldAt: '', reason: 'OTHER', memo: '', fxRate: '', deductionAmountKrw: '', netProceedsKrw: '', depositCashItemId: '', confirmUnrecorded: false },
        saleContext: { currentPriceKrw: null, currentPriceOriginal: null, currency: 'KRW', fxRate: null, totalAsset: null },
        salePreview: { profit: 0, profitRate: 0, contributionRate: 0, salePriceKrw: 0, profitKrw: 0, deductionAmountKrw: 0, netProceedsKrw: 0, netProfitKrw: 0, netProfitRate: 0, netContributionRate: 0 },
        userCashItems: [],
        userSales: [],
        userSalesLoading: false,
        userSalesItemIdsWithSales: [],
        showSaleDetailModal: false,
        saleDetail: null,
        editingSaleHistory: false,
        editSaleForm: { quantity: '', salePrice: '', deductionAmountKrw: '', netProceedsKrw: '', reason: 'OTHER', memo: '' },
        // 키워드 등록 모달 (#110) — 기사 열람은 키워드 메뉴가 담당한다
        keywordModal: { show: false, item: null, saving: false },
        // 상단 요약 · 배당/이자 · 자산 추이 (#110)
        summary: null,
        income: null,
        snapshots: [],
        snapshotSaving: false,
        _holdingGroupsCache: null,
        trendChartInstance: null,
        _trendChartRetry: 0,
        chartInstance: null,
        // 재무정보 상태
        financialChartInstance: null,
        financialOptions: null,
        financialResult: null,
        financialLoading: false,
        financialError: null,
        _tooltipText: '',
        selectedStockItem: null,
        financialYear: String(new Date().getFullYear()),
        financialReportCode: 'ANNUAL',
        selectedFinancialMenu: null,
        financialIndexClass: 'PROFITABILITY',
        financialFsDiv: 'CFS',
        financialAccountFsFilter: '',
        financialStatementFilter: '',
        _financialRequestGeneration: 0,
        // 연도별 추세(타임라인) 상태
        timelineData: null,
        timelineLoading: false,
        timelineError: null,
        timelineYears: '5',
        timelineFsDiv: 'CFS',
        _timelineCharts: [],
        timelineExpandedStatements: { IS: true },
        timelineExpandedIndexClasses: {},
        timelineExpandedDetailCategories: {},
        // 재무상세 패널 사용자 조절 너비(px). null이면 기본(lg 65%). localStorage에서 복원
        financialPanelWidth: parseInt(localStorage.getItem('financialPanelWidth'), 10) || null,
        // 공시 목록 상태
        disclosureData: null,
        disclosureLoading: false,
        disclosureError: null,
        disclosureSelectedTypes: [],
        disclosurePeriod: '1',
        financialMenus: [
            { key: 'timeline', label: '연도별 추세' },
            { key: 'disclosures', label: '공시' }
        ],
        _krFinancialMenus: [
            { key: 'timeline', label: '연도별 추세' },
            { key: 'disclosures', label: '공시' }
        ],
        _secFinancialMenus: [
            { key: 'sec-income', label: '손익계산서' },
            { key: 'sec-balance', label: '재무상태표' },
            { key: 'sec-cashflow', label: '현금흐름표' },
            { key: 'sec-metrics', label: '투자지표' }
        ],
        secFinancialData: null,
        secQuarterlyData: null,
        secQuarterlyPeriod: 'annual',
        secMetricsData: null,
        _secChartInstance: null,
        secFinancialError: null,
        secEdgarUrl: null
    },

    getAssetTypeLabel(type) {
        return this.assetTypeConfig[type]?.label || type;
    },

    getAssetTypeBadgeClass(type) {
        const color = this.assetTypeConfig[type]?.color || 'gray';
        return 'bg-' + color + '-100 text-' + color + '-700';
    },

    getAssetTypeBarClass(type) {
        return this.assetTypeConfig[type]?.barColor || 'bg-gray-500';
    },

    async loadPortfolio() {
        this.portfolio.loading = true;
        try {
            const results = await Promise.all([
                API.getPortfolioItems(this.auth.userId),
                API.getPortfolioAllocation(this.auth.userId),
                API.getAllocationStatus(this.auth.userId).catch((e) => {
                    console.error('배분 현황 조회 실패:', e);
                    return null;
                })
            ]);
            this.portfolio.items = results[0] || [];
            this.portfolio.allocation = results[1] || [];
            this.portfolio.allocationStatus = results[2] || null;
            this.invalidateHoldingGroups();

            this.portfolio.items
                .filter((item) => item.assetType === 'STOCK' && !item.stockDetail)
                .forEach((item) => {
                    console.warn('STOCK item without stockDetail', {
                        id: item.id,
                        itemName: item.itemName,
                        assetType: item.assetType
                    });
                });

            await this.loadStockPrices();

            // 섹션 키는 assetType 이 아니라 getHoldingGroups() 의 group.key 다.
            // 주식은 STOCK_KR / STOCK_OVERSEAS 로 쪼개지므로 assetType 으로 채우면 주식 그룹만 접힌 채 시작한다.
            const sections = {};
            this.getHoldingGroups().forEach((group) => {
                sections[group.key] = true;
            });
            this.portfolio.expandedSections = sections;

            // 보유 카드의 삭제 disabled 판정용 경량 itemId 인덱스 갱신
            // (전체 매도 이력 페이로드는 매도 이력 탭 진입 시에만 fetch)
            this.loadSaleItemIds();

            // KPI/자산 추이는 실패해도 목록 표시를 막지 않는다 (#110)
            this.loadSummary();
            this.loadSnapshots();
        } catch (e) {
            console.error('포트폴리오 로드 실패:', e);
            this.portfolio.items = [];
            this.portfolio.allocation = [];
            this.portfolio.allocationStatus = null;
            this.invalidateHoldingGroups();
        } finally {
            this.portfolio.loading = false;
            this.portfolio._initialized = true;
            this.$nextTick(() => {
                this.renderDonutChart();
            });
        }
    },

    // ──────────────────────────────────────────────────────────────────
    // 상단 요약 · 배당/이자 · 자산 추이 스냅샷 (#110)

    // 배당·이자는 /summary 응답의 income 에 함께 실려 온다 (평가 중복 실행 방지 — review M1)
    async loadSummary() {
        try {
            const summary = await API.getPortfolioSummary(this.auth.userId);
            this.portfolio.summary = summary;
            this.portfolio.income = summary ? summary.income : null;
        } catch (e) {
            console.error('포트폴리오 요약 조회 실패:', e);
            this.portfolio.summary = null;
            this.portfolio.income = null;
        }
    },

    async loadSnapshots() {
        try {
            this.portfolio.snapshots = await API.getPortfolioSnapshots(this.auth.userId, 12) || [];
        } catch (e) {
            console.error('자산 추이 조회 실패:', e);
            this.portfolio.snapshots = [];
        }
        this.$nextTick(() => this.renderPortfolioTrendChart());
    },

    async saveSnapshot() {
        if (this.portfolio.snapshotSaving) return;
        this.portfolio.snapshotSaving = true;
        try {
            await API.savePortfolioSnapshot(this.auth.userId);
            await this.loadSnapshots();
        } catch (e) {
            console.error('스냅샷 저장 실패:', e);
            alert('스냅샷 저장에 실패했습니다.');
        } finally {
            this.portfolio.snapshotSaving = false;
        }
    },

    // 환차손익 줄은 해외 자산이 실제로 있을 때만 노출한다 (#110 review L1).
    // 원화만 보유하면 fxProfit 이 항상 0이라 표시할 정보가 없다.
    hasFxProfitInfo() {
        const summary = this.portfolio.summary;
        if (!summary) return false;
        return Number(summary.fxProfit) !== 0 || summary.fxUnknownCount > 0;
    },

    getLatestSnapshotLabel() {
        const list = this.portfolio.snapshots;
        if (!list || list.length === 0) return '';
        return '마지막 저장 ' + list[list.length - 1].snapshotDate;
    },

    // salary.js 에도 renderTrendChart 가 있어 이름이 겹치면 나중에 로드되는 쪽이 덮어쓴다.
    // 컴포넌트가 같은 dashboard 객체에 병합되는 구조라 포트폴리오 전용 이름을 쓴다.
    renderPortfolioTrendChart() {
        const canvas = document.getElementById('portfolioTrendChart');
        if (!canvas) return;

        if (this.portfolio.trendChartInstance) {
            this.portfolio.trendChartInstance.destroy();
            this.portfolio.trendChartInstance = null;
        }

        const snapshots = this.portfolio.snapshots || [];
        if (snapshots.length < 2) return;   // 1건뿐이면 선이 그려지지 않아 안내 문구로 대체

        // x-show 가 아직 display:none 인 시점에 그리면 캔버스가 0 크기로 잡힌다.
        // requestAnimationFrame 은 탭이 백그라운드면 호출되지 않으므로 setTimeout 으로 재시도한다.
        if (canvas.clientHeight === 0) {
            if (this.portfolio._trendChartRetry >= 10) return;
            this.portfolio._trendChartRetry = (this.portfolio._trendChartRetry || 0) + 1;
            setTimeout(() => this.renderPortfolioTrendChart(), 50);
            return;
        }
        this.portfolio._trendChartRetry = 0;

        this.portfolio.trendChartInstance = new Chart(canvas, {
            type: 'line',
            data: {
                labels: snapshots.map((s) => s.snapshotDate),
                datasets: [
                    {
                        label: '총 자산',
                        data: snapshots.map((s) => Number(s.totalEvaluated)),
                        borderColor: '#E11D48',
                        backgroundColor: 'rgba(225, 29, 72, 0.08)',
                        fill: true,
                        tension: 0.25,
                        pointRadius: 2
                    },
                    {
                        label: '투자원금',
                        data: snapshots.map((s) => Number(s.totalInvested)),
                        borderColor: '#94A3B8',
                        borderDash: [4, 3],
                        fill: false,
                        tension: 0.25,
                        pointRadius: 0
                    }
                ]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                interaction: { mode: 'index', intersect: false },
                plugins: {
                    legend: { display: false },
                    tooltip: {
                        callbacks: {
                            label: (ctx) => ctx.dataset.label + ': ' + Format.number(ctx.parsed.y, 0) + '원'
                        }
                    }
                },
                scales: {
                    x: { grid: { display: false }, ticks: { font: { size: 10 } } },
                    y: {
                        ticks: {
                            font: { size: 10 },
                            callback: (value) => Math.round(value / 10000).toLocaleString('ko-KR') + '만'
                        }
                    }
                }
            }
        });
    },

    async loadStockPrices() {
        const stockItems = this.portfolio.items.filter((item) => item.assetType === 'STOCK' && item.stockDetail);
        if (stockItems.length === 0) {
            this.portfolio.stockPrices = {};
            this.invalidateHoldingGroups();
            return;
        }

        const stocks = stockItems.map((item) => ({
            stockCode: item.stockDetail.stockCode,
            marketType: item.stockDetail.market,
            exchangeCode: item.stockDetail.exchangeCode
        }));

        try {
            const result = await API.getStockPrices(stocks);
            this.portfolio.stockPrices = result.prices || {};
        } catch (e) {
            console.error('현재가 조회 실패:', e);
            this.portfolio.stockPrices = {};
        }
        this.invalidateHoldingGroups();
    },

    getEvalAmount(item) {
        if (item.assetType === 'STOCK' && item.stockDetail) {
            const priceData = this.portfolio.stockPrices[item.stockDetail.stockCode];
            if (priceData && priceData.currentPriceKrw) {
                return parseFloat(priceData.currentPriceKrw) * item.stockDetail.quantity;
            }
        }
        // 연금은 시세 연동이 없어 사용자가 갱신한 평가액을 쓴다 (미입력이면 원금)
        if (item.assetType === 'PENSION' && item.pensionDetail?.evaluatedAmount) {
            return parseFloat(item.pensionDetail.evaluatedAmount);
        }
        return item.investedAmount;
    },

    truncateToTen(value) {
        return Math.floor(Math.floor(value) / 10) * 10;
    },

    getMonthsBetween(startDateStr, endDateStr) {
        const start = new Date(startDateStr);
        const end = new Date(endDateStr);
        let months = (end.getFullYear() - start.getFullYear()) * 12
                     + (end.getMonth() - start.getMonth());
        if (end.getDate() < start.getDate()) months -= 1;
        return Math.max(0, months);
    },

    getExpectedReturn(item) {
        if (item.assetType !== 'CASH' || !item.cashDetail) return null;
        if (item.cashDetail.subType === 'CMA') return null;
        if (!item.cashDetail.interestRate || !item.cashDetail.startDate || !item.cashDetail.maturityDate) return null;

        const principal = item.investedAmount;
        const rate = item.cashDetail.interestRate / 100;
        const months = this.getMonthsBetween(item.cashDetail.startDate, item.cashDetail.maturityDate);
        const grossInterest = Math.floor(principal * rate * (months / 12));

        let totalTax = 0;
        if (item.cashDetail.taxType === 'GENERAL') {
            const incomeTax = this.truncateToTen(grossInterest * 0.14);
            const residentTax = this.truncateToTen(incomeTax * 0.1);
            totalTax = incomeTax + residentTax;
        } else if (item.cashDetail.taxType === 'TAX_FAVORED') {
            totalTax = this.truncateToTen(grossInterest * 0.095);
        }

        const netInterest = grossInterest - totalTax;
        return { grossInterest: grossInterest, totalTax: totalTax, netInterest: netInterest, expectedTotal: principal + netInterest };
    },

    getExchangeRate(item) {
        if (item.assetType !== 'STOCK' || !item.stockDetail) return 1;
        const priceCurrency = item.stockDetail.priceCurrency;
        if (!priceCurrency || priceCurrency === 'KRW') return 1;
        const priceData = this.portfolio.stockPrices[item.stockDetail.stockCode];
        if (!priceData || !priceData.exchangeRateValue) return 1;
        return parseFloat(priceData.exchangeRateValue);
    },

    getInvestedAmountKrw(item) {
        if (item.assetType === 'STOCK' && item.stockDetail && item.stockDetail.investedAmountKrw) {
            return parseFloat(item.stockDetail.investedAmountKrw);
        }
        return item.investedAmount * this.getExchangeRate(item);
    },

    getProfitAmount(item) {
        return this.getEvalAmount(item) - this.getInvestedAmountKrw(item);
    },

    getCacheRemainingText(priceData) {
        if (!priceData || priceData.remainingSeconds === undefined) return '';
        const remaining = priceData.remainingSeconds;
        const minutes = Math.floor(remaining / 60);
        const seconds = remaining % 60;
        if (remaining <= 0) return '0초 후 갱신';
        if (remaining < 60) return `${seconds}초 후 갱신`;
        return `${minutes}분 ${seconds}초 후 갱신`;
    },

    getCacheAgoText(priceData) {
        if (!priceData || !priceData.cachedAt) return '';
        const cachedAt = new Date(priceData.cachedAt);
        const now = new Date();
        const diffMs = now - cachedAt;
        const diffSeconds = Math.floor(diffMs / 1000);
        const diffMinutes = Math.floor(diffSeconds / 60);
        const remainSeconds = diffSeconds % 60;
        if (diffSeconds < 60) return `${diffSeconds}초 전 데이터`;
        return `${diffMinutes}분 ${remainSeconds}초 전 데이터`;
    },

    getTotalEvalAmount() {
        return this.portfolio.items.reduce((sum, item) => sum + this.getEvalAmount(item), 0);
    },

    getEvalAllocation() {
        const totalEval = this.getTotalEvalAmount();
        if (totalEval === 0) return this.portfolio.allocation;

        const typeMap = {};
        this.portfolio.items.forEach((item) => {
            if (!typeMap[item.assetType]) {
                typeMap[item.assetType] = { assetType: item.assetType, assetTypeName: this.getAssetTypeLabel(item.assetType), totalAmount: 0 };
            }
            typeMap[item.assetType].totalAmount += this.getEvalAmount(item);
        });

        const assetTypeOrder = ['STOCK', 'BOND', 'REAL_ESTATE', 'FUND', 'OTHER', 'CRYPTO', 'GOLD', 'COMMODITY', 'PENSION', 'CASH'];
        return Object.values(typeMap).map((alloc) => {
            alloc.percentage = Math.round(alloc.totalAmount / totalEval * 1000) / 10;
            return alloc;
        }).sort((a, b) => assetTypeOrder.indexOf(a.assetType) - assetTypeOrder.indexOf(b.assetType));
    },

    // ──────────────────────────────────────────────────────────────────
    // 목표 자산 배분

    allocationInvestTypes() {
        return ['STOCK', 'REAL_ESTATE', 'FUND', 'GOLD', 'COMMODITY', 'OTHER'];
    },

    // 편차 표시 공통 (#107): 막대는 중앙 0 기준 다이버징 — 왼쪽 부족, 오른쪽 초과.
    // 스케일(중앙에서 한쪽 끝까지의 %p)은 밴드 음영이 항상 보이도록 최소 밴드×2를 보장한다.

    hasDeviation(entry) {
        return entry.deviationPctPoint !== null && entry.deviationPctPoint !== undefined;
    },

    isDeviationZero(entry) {
        return this.hasDeviation(entry) && Math.abs(Number(entry.deviationPctPoint)) < 0.05;
    },

    getDeviationScale() {
        const status = this.portfolio.allocationStatus;
        if (!status) return 10;
        const entries = [...(status.buckets || []), ...(status.investAssets || [])];
        const maxDev = Math.max(...entries.filter((e) => this.hasDeviation(e))
            .map((e) => Math.abs(Number(e.deviationPctPoint))), 0);
        const band = Number(status.bandPctPoint) || 5;
        return Math.max(band * 2, Math.ceil(maxDev * 1.1));
    },

    getDeviationBarStyle(entry) {
        if (!this.hasDeviation(entry) || this.isDeviationZero(entry)) return 'display: none';
        const dev = Number(entry.deviationPctPoint);
        const half = Math.min(Math.abs(dev) / this.getDeviationScale(), 1) * 50;
        const side = dev > 0 ? 'left: 50%' : 'right: 50%';
        return `${side}; width: ${half}%`;
    },

    getDeviationBandStyle() {
        const status = this.portfolio.allocationStatus;
        const band = Number(status && status.bandPctPoint) || 5;
        const half = Math.min(band / this.getDeviationScale(), 1) * 50;
        return `left: ${50 - half}%; width: ${half * 2}%`;
    },

    getDeviationBarClass(entry) {
        if (!entry.bandExceeded) return 'bg-gray-300';
        return Number(entry.deviationPctPoint) > 0 ? 'bg-red-400' : 'bg-blue-400';
    },

    getDeviationTextClass(entry) {
        if (!entry.bandExceeded) return 'text-gray-400';
        return Number(entry.deviationPctPoint) > 0 ? 'text-red-600 font-medium' : 'text-blue-600 font-medium';
    },

    formatKrwCompact(amount) {
        const abs = Math.abs(Math.round(Number(amount)));
        if (abs < 10000) return `${Format.number(abs, 0)}원`;
        // 만원 단위로 먼저 반올림해야 9,999.6만원 → "1억"으로 캐리가 넘어간다
        const totalMan = Math.round(abs / 10000);
        const eok = Math.floor(totalMan / 10000);
        const man = totalMan % 10000;
        if (eok === 0) return `${Format.number(man, 0)}만원`;
        return man > 0 ? `${eok}억 ${Format.number(man, 0)}만원` : `${eok}억원`;
    },

    formatDeviationLabel(entry) {
        if (!this.hasDeviation(entry)) {
            return '현재 ' + Number(entry.currentRatio).toFixed(1) + '% · 목표 미설정';
        }
        if (this.isDeviationZero(entry)) return '목표 일치';
        const dev = Number(entry.deviationPctPoint);
        const sign = dev > 0 ? '+' : '−';
        const word = dev > 0 ? '많음' : '부족';
        return `${sign}${Math.abs(dev).toFixed(1)}%p · ${this.formatKrwCompact(entry.deviationAmount)} ${word}`;
    },

    formatAllocRatioPair(entry) {
        const current = Number(entry.currentRatio).toFixed(1) + '%';
        if (entry.targetRatio === null || entry.targetRatio === undefined) return current;
        return current + ' / ' + Number(entry.targetRatio).toFixed(1) + '%';
    },

    getRebalanceSummary() {
        const status = this.portfolio.allocationStatus;
        if (!status || !status.buckets || status.buckets.length === 0) return '';
        const exceeded = status.buckets.some((b) => b.bandExceeded);
        if (!exceeded) return '안전·투자 비율이 허용밴드 안에 있습니다';
        const short = status.buckets.find((b) => Number(b.deviationPctPoint) < 0);
        const over = status.buckets.find((b) => Number(b.deviationPctPoint) > 0);
        if (!short || !over) return '';
        const amount = this.formatKrwCompact(short.deviationAmount);
        return `${over.bucketName}에서 ${short.bucketName}으로 약 ${amount} 이동 시 목표 도달`;
    },

    // ──────────────────────────────────────────────────────────────────
    // 리밸런싱 제안 (#110) — 밴드를 벗어난 항목만 금액 제안. 주문 연동은 하지 않는다.

    // 상위 버킷(전체 대비)과 투자자산 내부(투자자산 총액 대비)는 기준이 다르므로
    // 한 목록에 섞더라도 level 로 구분하고, 이동 금액도 레벨별로 따로 합산한다.
    _rebalanceEntries(level) {
        const status = this.portfolio.allocationStatus;
        if (!status || !status.configured) return [];
        const source = level === 'bucket' ? (status.buckets || []) : (status.investAssets || []);
        return source
            .filter((entry) => entry.bandExceeded && this.hasDeviation(entry))
            .map((entry) => {
                const dev = Number(entry.deviationPctPoint);
                return {
                    key: level + ':' + (entry.bucket || entry.assetType),
                    level: level,
                    levelLabel: level === 'bucket' ? '상위 배분' : '투자자산 내부',
                    name: entry.bucketName || entry.assetTypeName,
                    over: dev > 0,
                    verb: dev > 0 ? '줄이기' : '채우기',
                    amount: Math.abs(Number(entry.deviationAmount || 0)),
                    detail: `현재 ${Number(entry.currentRatio).toFixed(1)}% → 목표 ${Number(entry.targetRatio).toFixed(1)}%`
                        + ` · ${dev > 0 ? '+' : '−'}${Math.abs(dev).toFixed(1)}%p`
                };
            })
            .sort((a, b) => b.amount - a.amount);
    },

    getRebalanceActions() {
        return [...this._rebalanceEntries('bucket'), ...this._rebalanceEntries('asset')];
    },

    getBandExceededCount() {
        return this.getRebalanceActions().length;
    },

    // 초과분과 부족분이 짝을 이루므로 레벨별 합계의 절반이 실제 이동 금액이다.
    // 두 레벨을 합산하면 같은 돈을 두 번 세므로, 상위 배분이 밴드를 벗어난 경우 그 금액을 우선한다.
    getRebalanceMoveTotal() {
        const half = (entries) => entries.reduce((sum, action) => sum + action.amount, 0) / 2;
        const buckets = this._rebalanceEntries('bucket');
        if (buckets.length > 0) return half(buckets);
        return half(this._rebalanceEntries('asset'));
    },

    getRebalanceMoveNote() {
        const buckets = this._rebalanceEntries('bucket');
        if (buckets.length > 0) {
            return '안전·투자 상위 배분 기준 이동액입니다. 투자자산 내부 조정은 아래 목록을 참고해 주세요.';
        }
        return '투자자산 내부 재배치 기준입니다. 매도 대금으로 매수분을 채우면 추가 입금 없이 조정됩니다.';
    },

    async adjustBand(delta) {
        const status = this.portfolio.allocationStatus;
        if (!status || !status.configured) return;
        const next = Math.round((Number(status.bandPctPoint) + delta) * 10) / 10;
        if (next < 0.5 || next > 20) return;

        try {
            const target = await API.getAllocationTarget(this.auth.userId);
            if (!target) return;
            await API.saveAllocationTarget(this.auth.userId, {
                safeRatio: target.safeRatio,
                investRatio: target.investRatio,
                bandPctPoint: next,
                investAssets: (target.investAssets || []).map((asset) => ({
                    assetType: asset.assetType,
                    targetRatio: asset.targetRatio
                }))
            });
            this.portfolio.allocationStatus = await API.getAllocationStatus(this.auth.userId).catch(() => null);
        } catch (e) {
            console.error('허용밴드 변경 실패:', e);
            alert('허용밴드 변경에 실패했습니다.');
        }
    },

    async copyRebalanceChecklist() {
        const actions = this.getRebalanceActions();
        if (actions.length === 0) return;
        const lines = actions.map((action) =>
            `- [${action.levelLabel}] ${action.name}: ${action.verb} ${this.formatKrwCompact(action.amount)} (${action.detail})`);
        const text = ['[리밸런싱 체크리스트]', ...lines].join('\n');
        try {
            await navigator.clipboard.writeText(text);
            alert('체크리스트를 복사했습니다.');
        } catch (e) {
            console.error('클립보드 복사 실패:', e);
            alert('복사에 실패했습니다. 브라우저 클립보드 권한을 확인해 주세요.');
        }
    },

    async openAllocationTargetModal() {
        const modal = this.portfolio.allocationTargetModal;
        const assets = this.allocationInvestTypes().map((type) => ({
            assetType: type,
            label: this.getAssetTypeLabel(type),
            ratio: ''
        }));
        modal.form = { safeRatio: '', bandPctPoint: '5', assets };

        try {
            const target = await API.getAllocationTarget(this.auth.userId);
            if (target) {
                modal.form.safeRatio = String(target.safeRatio);
                modal.form.bandPctPoint = String(target.bandPctPoint);
                (target.investAssets || []).forEach((asset) => {
                    const row = assets.find((a) => a.assetType === asset.assetType);
                    if (row) row.ratio = String(asset.targetRatio);
                });
            }
        } catch (e) {
            console.error('배분 목표 조회 실패:', e);
        }
        modal.show = true;
    },

    closeAllocationTargetModal() {
        this.portfolio.allocationTargetModal.show = false;
    },

    getAllocationInvestRatio() {
        const safe = Number(this.portfolio.allocationTargetModal.form.safeRatio);
        if (Number.isNaN(safe) || this.portfolio.allocationTargetModal.form.safeRatio === '') return null;
        return Math.round((100 - safe) * 100) / 100;
    },

    getAllocationAssetSum() {
        const sum = this.portfolio.allocationTargetModal.form.assets
            .reduce((acc, row) => acc + (row.ratio === '' ? 0 : Number(row.ratio) || 0), 0);
        return Math.round(sum * 100) / 100;
    },

    async submitAllocationTarget() {
        const modal = this.portfolio.allocationTargetModal;
        const form = modal.form;

        // 소수 2자리로 반올림해 안전+투자 합이 정확히 100이 되도록 맞춘다
        const safeRatio = Math.round(Number(form.safeRatio) * 100) / 100;
        if (form.safeRatio === '' || Number.isNaN(safeRatio) || safeRatio < 0 || safeRatio > 100) {
            alert('안전자산 목표 비율은 0~100 사이로 입력해 주세요.');
            return;
        }
        const bandPctPoint = Number(form.bandPctPoint);
        if (form.bandPctPoint === '' || Number.isNaN(bandPctPoint) || bandPctPoint < 0 || bandPctPoint > 20) {
            alert('허용밴드는 0~20%p 사이로 입력해 주세요.');
            return;
        }
        const filledAssets = form.assets.filter((row) => row.ratio !== '' && Number(row.ratio) > 0);
        if (filledAssets.length > 0) {
            const sum = this.getAllocationAssetSum();
            if (Math.abs(sum - 100) > 0.001) {
                alert(`투자자산 세부 비율의 합은 100이어야 합니다. (현재: ${sum})`);
                return;
            }
        }

        modal.saving = true;
        try {
            await API.saveAllocationTarget(this.auth.userId, {
                safeRatio: safeRatio,
                investRatio: Math.round((100 - safeRatio) * 100) / 100,
                bandPctPoint: bandPctPoint,
                investAssets: filledAssets.map((row) => ({
                    assetType: row.assetType,
                    targetRatio: Math.round(Number(row.ratio) * 100) / 100
                }))
            });
            modal.show = false;
            this.portfolio.allocationStatus = await API.getAllocationStatus(this.auth.userId).catch(() => null);
        } catch (e) {
            console.error('배분 목표 저장 실패:', e);
            alert('배분 목표 저장에 실패했습니다.');
        } finally {
            modal.saving = false;
        }
    },

    getItemsByType(type) {
        return this.portfolio.items.filter((item) => item.assetType === type);
    },

    getDomesticStocks() {
        return this.portfolio.items.filter((item) => item.assetType === 'STOCK' && item.stockDetail?.country === 'KR')
            .sort((a, b) => {
                const aIsEtf = a.stockDetail?.subType === 'ETF' ? 1 : 0;
                const bIsEtf = b.stockDetail?.subType === 'ETF' ? 1 : 0;
                return aIsEtf - bIsEtf;
            });
    },

    getOverseasStocks() {
        return this.portfolio.items.filter((item) => item.assetType === 'STOCK' && item.stockDetail && item.stockDetail.country !== 'KR');
    },

    getCashSubTypeKey(item) {
        return item.cashDetail?.subType || 'ETC';
    },

    getCashItemsSorted() {
        const order = ['DEPOSIT', 'SAVINGS', 'CMA'];
        return this.getItemsByType('CASH').slice().sort((a, b) => {
            const aIdx = order.indexOf(this.getCashSubTypeKey(a));
            const bIdx = order.indexOf(this.getCashSubTypeKey(b));
            return (aIdx === -1 ? order.length : aIdx) - (bIdx === -1 ? order.length : bIdx);
        });
    },

    getTotalInvested() {
        return this.portfolio.items.reduce((sum, item) => sum + this.getInvestedAmountKrw(item), 0);
    },

    toggleSection(sectionKey) {
        this.portfolio.expandedSections[sectionKey] = !this.portfolio.expandedSections[sectionKey];
    },

    expandAllSections() {
        this.getHoldingGroups().forEach((group) => {
            this.portfolio.expandedSections[group.key] = true;
        });
    },

    collapseAllSections() {
        this.portfolio.expandedSections = {};
    },

    // ──────────────────────────────────────────────────────────────────
    // 보유 자산 테이블 (#110)
    // 그룹은 자산군 단위. 주식만 표시 레벨에서 국내/해외로 쪼개고(AssetType 은 STOCK 그대로),
    // 현금성 자산은 예금/적금/CMA 서브그룹을 함께 넘긴다.

    // 템플릿 x-for·배지·초기 확장에서 반복 호출되므로 결과를 캐시한다 (#110 review L4).
    // items 와 stockPrices 가 바뀌는 지점(loadPortfolio·loadStockPrices)에서만 무효화한다.
    getHoldingGroups() {
        if (this.portfolio._holdingGroupsCache) return this.portfolio._holdingGroupsCache;
        const totalEval = this.getTotalEvalAmount();
        const build = (key, name, assetType, items, subGroups) => {
            const invested = items.reduce((sum, item) => sum + this.getInvestedAmountKrw(item), 0);
            const evaluated = items.reduce((sum, item) => sum + this.getEvalAmount(item), 0);
            return {
                key: key,
                name: name,
                assetType: assetType,
                items: items,
                subGroups: subGroups || null,
                count: items.length,
                invested: invested,
                evaluated: evaluated,
                profit: evaluated - invested,
                profitRate: invested > 0 ? (evaluated - invested) / invested * 100 : null,
                weight: totalEval > 0 ? evaluated / totalEval * 100 : 0
            };
        };

        const groups = [];
        this.getEvalAllocation().forEach((alloc) => {
            const type = alloc.assetType;

            if (type === 'STOCK') {
                const domestic = this.getDomesticStocks();
                const overseas = this.getOverseasStocks();
                if (domestic.length > 0) groups.push(build('STOCK_KR', '🇰🇷 국내 주식', 'STOCK', domestic));
                if (overseas.length > 0) groups.push(build('STOCK_OVERSEAS', '🌐 해외 주식', 'STOCK', overseas));
                return;
            }

            if (type === 'CASH') {
                const labels = { DEPOSIT: '예금', SAVINGS: '적금', CMA: 'CMA' };
                const sorted = this.getCashItemsSorted();
                const subGroups = [];
                sorted.forEach((item) => {
                    const subKey = this.getCashSubTypeKey(item);
                    let sub = subGroups.find((s) => s.key === subKey);
                    if (!sub) {
                        sub = { key: subKey, name: labels[subKey] || '기타', items: [] };
                        subGroups.push(sub);
                    }
                    sub.items.push(item);
                });
                subGroups.forEach((sub) => {
                    sub.count = sub.items.length;
                    sub.evaluated = sub.items.reduce((sum, item) => sum + this.getEvalAmount(item), 0);
                });
                groups.push(build('CASH', alloc.assetTypeName, 'CASH', sorted, subGroups));
                return;
            }

            groups.push(build(type, alloc.assetTypeName, type, this.getItemsByType(type)));
        });
        this.portfolio._holdingGroupsCache = groups;
        return groups;
    },

    invalidateHoldingGroups() {
        this.portfolio._holdingGroupsCache = null;
    },

    getDaysUntil(dateStr) {
        if (!dateStr) return null;
        const target = new Date(dateStr + 'T00:00:00');
        if (Number.isNaN(target.getTime())) return null;
        const today = new Date();
        today.setHours(0, 0, 0, 0);
        return Math.round((target - today) / 86400000);
    },

    getRowQuantity(item) {
        if (item.assetType === 'STOCK' && item.stockDetail?.quantity) {
            return Format.number(item.stockDetail.quantity, 0) + '주';
        }
        if (item.assetType === 'GOLD' && item.goldDetail?.quantityGrams) {
            return Format.number(item.goldDetail.quantityGrams, 2) + 'g';
        }
        return '—';
    },

    // 평단(주식) 또는 금리(현금성·채권·펀드 보수) 열
    getRowAvgLabel(item) {
        if (item.assetType === 'STOCK' && item.stockDetail?.avgBuyPrice) {
            const currency = item.stockDetail.priceCurrency;
            const isKrw = !currency || currency === 'KRW';
            return Format.number(item.stockDetail.avgBuyPrice, isKrw ? 0 : 2) + (isKrw ? '원' : ' ' + currency);
        }
        if (item.assetType === 'CASH' && item.cashDetail?.interestRate) {
            return '연 ' + item.cashDetail.interestRate + '%';
        }
        if (item.assetType === 'BOND' && item.bondDetail?.couponRate) {
            return '표면 ' + item.bondDetail.couponRate + '%';
        }
        if (item.assetType === 'FUND' && item.fundDetail?.managementFee) {
            return '보수 ' + item.fundDetail.managementFee + '%';
        }
        return '—';
    },

    // 현재가(주식) 또는 만기 D-day(현금성·채권) 열
    getRowCurrentLabel(item) {
        if (item.assetType === 'STOCK' && item.stockDetail) {
            const priceData = this.portfolio.stockPrices[item.stockDetail.stockCode];
            if (!priceData || !priceData.currentPrice) return '—';
            const currency = priceData.currency || 'KRW';
            return currency === 'KRW'
                ? Format.number(priceData.currentPrice, 0) + '원'
                : Format.number(priceData.currentPrice, 2) + ' ' + currency;
        }
        const maturity = item.cashDetail?.maturityDate || item.bondDetail?.maturityDate;
        if (maturity) {
            const days = this.getDaysUntil(maturity);
            if (days === null) return maturity;
            return days >= 0 ? 'D-' + days : '만기 경과';
        }
        if (item.assetType === 'CASH') return '수시입출';
        return '—';
    },

    getRowProfitRate(item) {
        const invested = this.getInvestedAmountKrw(item);
        if (!invested) return null;
        return (this.getEvalAmount(item) - invested) / invested * 100;
    },

    getRowWeight(item) {
        const totalEval = this.getTotalEvalAmount();
        if (totalEval === 0) return 0;
        return this.getEvalAmount(item) / totalEval * 100;
    },

    getTotalProfit() {
        return this.getTotalEvalAmount() - this.getTotalInvested();
    },

    getTotalProfitRate() {
        const invested = this.getTotalInvested();
        if (invested <= 0) return null;
        return (this.getTotalEvalAmount() - invested) / invested * 100;
    },

    getUnlinkedStockCount() {
        return this.portfolio.items.filter((item) => item.assetType === 'STOCK' && !item.linkedCashItemId).length;
    },

    // 가장 최근에 조회된 주가 캐시 1건 — 헤더의 "시세 N분 전 · TTL" 표기에 사용
    getLatestPriceCache() {
        const list = Object.values(this.portfolio.stockPrices || {});
        return list.length > 0 ? list[0] : null;
    },

    // ──────────────────────────────────────────────────────────────────
    // 행 액션 노출 규칙 (#110)
    // 기존 partial 의 x-show 조건식을 헬퍼로 옮긴 것 — 규칙 자체는 그대로다.

    // 현금성·연금은 상품명이 뉴스 키워드로 의미가 없어 제외한다 (#110 review L3)
    canKeyword(item) {
        if (item.assetType === 'CASH' || item.assetType === 'PENSION') return false;
        return item.stockDetail?.subType !== 'ETF';
    },

    canFinancial(item) {
        if (item.assetType !== 'STOCK' || !item.stockDetail) return false;
        if (item.stockDetail.subType === 'ETF') return false;
        return item.stockDetail.country === 'KR' || item.stockDetail.country === 'US';
    },

    canPurchase(item) {
        return item.assetType === 'STOCK' && !!item.stockDetail;
    },

    canSell(item) {
        return this.canPurchase(item) && (item.status === 'ACTIVE' || !item.status);
    },

    canDelete(item) {
        return !(item.assetType === 'STOCK' && this.hasSaleHistories(item.id));
    },

    getAnalysisTargets() {
        return this.portfolio.items.filter((item) => this.canFinancial(item));
    },

    renderDonutChart() {
        const canvas = document.getElementById('portfolioDonutChart');
        if (!canvas) return;

        if (this.portfolio.chartInstance) {
            this.portfolio.chartInstance.destroy();
            this.portfolio.chartInstance = null;
        }

        const allocation = this.getEvalAllocation();
        if (allocation.length === 0) return;

        const labels = [];
        const data = [];
        const colors = [];

        allocation.forEach((alloc) => {
            labels.push(alloc.assetTypeName);
            data.push(alloc.totalAmount);
            const config = this.assetTypeConfig[alloc.assetType];
            colors.push(config ? config.chartColor : '#94A3B8');
        });

        this.portfolio.chartInstance = new Chart(canvas, {
            type: 'doughnut',
            data: {
                labels: labels,
                datasets: [{
                    data: data,
                    backgroundColor: colors,
                    borderWidth: 2,
                    borderColor: '#ffffff',
                    hoverBorderWidth: 3
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: true,
                cutout: '65%',
                plugins: {
                    // 범례는 우측 "자산 비중" 카드가 대신한다 (#107) — 캔버스 내장 범례를 켜면
                    // 자산 유형 수에 따라 차트 영역이 줄어 중앙 오버레이가 원 중심을 벗어난다.
                    legend: {
                        display: false
                    },
                    tooltip: {
                        callbacks: {
                            label: (context) => {
                                const value = context.parsed;
                                const total = context.dataset.data.reduce((sum, val) => sum + val, 0);
                                const pct = total > 0 ? Math.round(value / total * 1000) / 10 : 0;
                                return context.label + ': ' + Format.number(value, 0) + '원 (' + pct + '%)';
                            }
                        }
                    }
                }
            }
        });
    },

    getItemSummary(item) {
        switch (item.assetType) {
            case 'STOCK':
                if (!item.stockDetail) return '';
                const parts = [item.stockDetail.market + ':' + item.stockDetail.stockCode];
                if (item.stockDetail.subType === 'ETF') parts.push('ETF');
                if (item.stockDetail.quantity) parts.push(item.stockDetail.quantity + '주');
                if (item.stockDetail.avgBuyPrice) {
                    const cur = item.stockDetail.priceCurrency;
                    const suffix = (!cur || cur === 'KRW') ? '원' : ' ' + cur;
                    const decimals = (!cur || cur === 'KRW') ? 0 : 2;
                    parts.push('평균 ' + Format.number(item.stockDetail.avgBuyPrice, decimals) + suffix);
                }
                return parts.join(' · ');
            case 'BOND':
                if (!item.bondDetail) return '';
                const bondParts = [];
                if (item.bondDetail.subType === 'GOVERNMENT') bondParts.push('국채');
                else if (item.bondDetail.subType === 'CORPORATE') bondParts.push('회사채');
                if (item.bondDetail.maturityDate) bondParts.push('만기 ' + item.bondDetail.maturityDate);
                if (item.bondDetail.couponRate) bondParts.push(item.bondDetail.couponRate + '%');
                return bondParts.join(' · ');
            case 'REAL_ESTATE':
                if (!item.realEstateDetail) return '';
                const reParts = [];
                const reSubTypes = { APARTMENT: '아파트', OFFICETEL: '오피스텔', LAND: '토지', COMMERCIAL: '상가' };
                if (item.realEstateDetail.subType) reParts.push(reSubTypes[item.realEstateDetail.subType] || item.realEstateDetail.subType);
                if (item.realEstateDetail.address) reParts.push(item.realEstateDetail.address);
                return reParts.join(' · ');
            case 'FUND':
                if (!item.fundDetail) return '';
                const fundParts = [];
                const fundSubTypes = { EQUITY_FUND: '주식형', BOND_FUND: '채권형', MIXED_FUND: '혼합형' };
                if (item.fundDetail.subType) fundParts.push(fundSubTypes[item.fundDetail.subType] || item.fundDetail.subType);
                if (item.fundDetail.managementFee) fundParts.push('보수 ' + item.fundDetail.managementFee + '%');
                if (item.fundDetail.monthlyDepositAmount) fundParts.push('월 ' + Format.number(item.fundDetail.monthlyDepositAmount, 0) + '원');
                if (item.depositOverdue) fundParts.push('⚠ 미납');
                return fundParts.join(' · ');
            case 'PENSION':
                if (!item.pensionDetail) return item.memo || '';
                const pensionParts = [];
                const pensionSubTypes = { IRP: 'IRP', PENSION_SAVING: '연금저축', DC: '퇴직연금 DC', DB: '퇴직연금 DB' };
                pensionParts.push(pensionSubTypes[item.pensionDetail.subType] || item.pensionDetail.subType);
                if (item.pensionDetail.provider) pensionParts.push(item.pensionDetail.provider);
                if (item.pensionDetail.monthlyDepositAmount) {
                    pensionParts.push('월 ' + Format.number(item.pensionDetail.monthlyDepositAmount, 0) + '원');
                }
                if (item.depositOverdue) pensionParts.push('⚠ 미납');
                return pensionParts.join(' · ');
            case 'CASH':
                if (!item.cashDetail) return item.memo || '';
                const cashParts = [];
                const cashSubTypes = { DEPOSIT: '예금', SAVINGS: '적금', CMA: 'CMA' };
                cashParts.push(cashSubTypes[item.cashDetail.subType] || item.cashDetail.subType);
                if (item.cashDetail.interestRate) cashParts.push(item.cashDetail.interestRate + '%');
                if (item.cashDetail.maturityDate) cashParts.push('만기 ' + item.cashDetail.maturityDate);
                if (item.cashDetail.monthlyDepositAmount) cashParts.push('월 ' + Format.number(item.cashDetail.monthlyDepositAmount, 0) + '원');
                if (item.depositOverdue) cashParts.push('⚠ 미납');
                if (item.expectedMaturityAmount) {
                    cashParts.push('만기 예상 ' + Format.number(item.expectedMaturityAmount, 0) + '원');
                } else {
                    const expectedReturn = this.getExpectedReturn(item);
                    if (expectedReturn) {
                        cashParts.push('예상 수령 ' + Format.number(expectedReturn.expectedTotal, 0) + '원');
                    }
                }
                return cashParts.join(' · ');
            default:
                return item.memo || '';
        }
    },

    async deleteItem(itemId) {
        const item = this.portfolio.items.find((i) => i.id === itemId);
        const hasLinkedCash = item && item.assetType === 'STOCK' && item.linkedCashItemId;

        if (hasLinkedCash) {
            this.portfolio.deleteConfirm = {
                show: true,
                itemId: itemId,
                itemName: item.itemName,
                restoreCash: false,
                restoreAmount: item.investedAmount || 0,
                linkedCashItemId: item.linkedCashItemId
            };
            return;
        }

        if (!confirm('자산 항목을 삭제하시겠습니까?\n관련 뉴스도 함께 삭제됩니다.')) return;
        try {
            await API.deletePortfolioItem(this.auth.userId, itemId);
            await this.loadPortfolio();
        } catch (e) {
            console.error('항목 삭제 실패:', e);
            alert('삭제에 실패했습니다.');
        }
    },

    async confirmDelete() {
        const dc = this.portfolio.deleteConfirm;
        try {
            await API.deletePortfolioItem(
                this.auth.userId, dc.itemId,
                dc.restoreCash, dc.restoreCash ? Number(dc.restoreAmount) : null
            );
            this.portfolio.deleteConfirm = { show: false };
            await this.loadPortfolio();
        } catch (e) {
            console.error('항목 삭제 실패:', e);
            alert('삭제에 실패했습니다: ' + e.message);
        }
    },

    cancelDelete() {
        this.portfolio.deleteConfirm = { show: false };
    },

    // ──────────────────────────────────────────────────────────────────
    // 키워드 등록 (#110)
    // 기사 열람·수집·삭제는 키워드 메뉴가 담당한다. 포트폴리오에는 등록 진입점만 남긴다.

    openKeywordModal(item) {
        if (item.newsEnabled) return;
        this.portfolio.keywordModal = { show: true, item: item, saving: false };
    },

    closeKeywordModal() {
        this.portfolio.keywordModal = { show: false, item: null, saving: false };
    },

    async submitKeyword() {
        const modal = this.portfolio.keywordModal;
        if (!modal.item || modal.saving) return;
        modal.saving = true;
        try {
            await API.togglePortfolioNews(this.auth.userId, modal.item.id, true);
            await this.loadPortfolio();
            this.closeKeywordModal();
        } catch (e) {
            console.error('키워드 등록 실패:', e);
            alert('키워드 등록에 실패했습니다.');
            modal.saving = false;
        }
    },

    // 서버는 item.region 을 그대로 키워드 region 으로 등록한다(PortfolioService.toggleNews).
    // 화면 표기도 stockDetail.country 가 아니라 item.region 을 따라가야 실제 등록값과 어긋나지 않는다.
    getKeywordRegion(item) {
        const labels = { DOMESTIC: '국내', INTERNATIONAL: '해외' };
        return labels[item?.region] || item?.region || '국내';
    },

    async toggleNotification() {
        try {
            var newValue = !this.auth.notificationEnabled;
            await API.toggleNotification(newValue);
            this.auth.notificationEnabled = newValue;
        } catch (e) {
            console.error('알림 설정 변경 실패:', e);
            alert('알림 설정 변경에 실패했습니다.');
        }
    },

    openAddModal() {
        this.portfolio.selectedAssetType = null;
        this.portfolio.addForm = {};
        this.portfolio.stockSearch = { query: '', results: [], loading: false, selected: null, debounceTimer: null };
        this.portfolio.showAddModal = true;
    },

    selectAssetType(type) {
        this.portfolio.selectedAssetType = type;
        this.portfolio.addForm = { region: 'DOMESTIC' };
        this.portfolio.stockSearch = { query: '', results: [], loading: false, selected: null, debounceTimer: null };

        if (type === 'CASH') {
            this.portfolio.addForm.cashType = 'DEPOSIT';
            this.portfolio.addForm.taxType = 'GENERAL';
        }
        if (type === 'GENERAL') {
            this.portfolio.addForm.assetType = 'CRYPTO';
        }
    },

    async searchStock() {
        const query = this.portfolio.stockSearch.query.trim();
        if (!query) return;
        this.portfolio.stockSearch.loading = true;
        try {
            this.portfolio.stockSearch.results = await API.searchStocks(query) || [];
        } catch (e) {
            console.error('종목 검색 실패:', e);
            this.portfolio.stockSearch.results = [];
        } finally {
            this.portfolio.stockSearch.loading = false;
        }
    },

    selectStock(stock) {
        this.portfolio.stockSearch.selected = stock;
        this.portfolio.stockSearch.results = [];
        this.portfolio.stockSearch.query = '';
        this.portfolio.addForm.itemName = stock.stockName;
        this.portfolio.addForm.ticker = stock.stockCode;
        this.portfolio.addForm.exchange = stock.marketType;
        this.portfolio.addForm.exchangeCode = stock.exchangeCode;
        this.portfolio.addForm.region = stock.exchangeCode === 'KRX' ? 'DOMESTIC' : 'INTERNATIONAL';
        this.portfolio.addForm.priceCurrency = this.getCurrencyByExchangeCode(stock.exchangeCode);
    },

    getCashItems() {
        return this.portfolio.items.filter(item => {
            return item.assetType === 'CASH';
        });
    },

    getLinkedCashName(item) {
        if (!item.linkedCashItemId) return null;
        const cash = this.portfolio.items.find(i => i.id === item.linkedCashItemId);
        return cash ? cash.itemName : null;
    },

    getCurrencyByExchangeCode(exchangeCode) {
        const mapping = {
            KRX: 'KRW',
            NAS: 'USD', NYS: 'USD', AMS: 'USD',
            SHS: 'CNY', SHI: 'CNY', SZS: 'CNY', SZI: 'CNY',
            TSE: 'JPY',
            HKS: 'HKD',
            HNX: 'VND', HSX: 'VND'
        };
        return mapping[exchangeCode] || 'KRW';
    },

    clearSelectedStock() {
        this.portfolio.stockSearch.selected = null;
        this.portfolio.addForm.itemName = '';
        this.portfolio.addForm.ticker = '';
        this.portfolio.addForm.exchange = '';
        this.portfolio.addForm.exchangeCode = '';
    },

    getCountryByExchangeCode(exchangeCode) {
        const mapping = {
            KRX: 'KR',
            NAS: 'US', NYS: 'US', AMS: 'US',
            SHS: 'CN', SHI: 'CN', SZS: 'CN', SZI: 'CN',
            TSE: 'JP',
            HKS: 'HK',
            HNX: 'VN', HSX: 'VN'
        };
        return mapping[exchangeCode] || null;
    },

    async submitAddItem() {
        const type = this.portfolio.selectedAssetType;
        const form = this.portfolio.addForm;
        const userId = this.auth.userId;

        if (!form.itemName) {
            alert('항목명은 필수입니다.');
            return;
        }
        if (type === 'STOCK') {
            if (!form.quantity || Number(form.quantity) <= 0) {
                alert('매수 수량은 필수입니다.');
                return;
            }
            if (!form.purchasePrice || Number(form.purchasePrice) <= 0) {
                alert('매수가는 필수입니다.');
                return;
            }
        } else {
            if (!form.investedAmount || Number(form.investedAmount) <= 0) {
                alert('투자 금액은 0보다 커야 합니다.');
                return;
            }
        }

        try {
            switch (type) {
                case 'STOCK':
                    await API.addStockItem(userId, {
                        itemName: form.itemName, region: form.region, memo: form.memo || null,
                        subType: form.subType || 'INDIVIDUAL', stockCode: form.ticker, market: form.exchange,
                        exchangeCode: form.exchangeCode, country: this.getCountryByExchangeCode(form.exchangeCode),
                        quantity: Number(form.quantity), purchasePrice: Number(form.purchasePrice),
                        dividendYield: form.dividendYield ? Number(form.dividendYield) : null,
                        priceCurrency: form.priceCurrency || 'KRW',
                        investedAmountKrw: form.investedAmountKrw ? Number(form.investedAmountKrw) : null,
                        cashItemId: form.cashItemId ? Number(form.cashItemId) : null
                    });
                    break;
                case 'BOND':
                    await API.addBondItem(userId, {
                        itemName: form.itemName, investedAmount: Number(form.investedAmount), region: form.region,
                        memo: form.memo || null, subType: form.subType || 'GOVERNMENT',
                        maturityDate: form.maturityDate || null,
                        couponRate: form.couponRate ? Number(form.couponRate) : null,
                        creditRating: form.creditRating || null
                    });
                    break;
                case 'REAL_ESTATE':
                    await API.addRealEstateItem(userId, {
                        itemName: form.itemName, investedAmount: Number(form.investedAmount), region: form.region,
                        memo: form.memo || null, subType: form.subType || 'APARTMENT',
                        address: form.address || null, area: form.area ? Number(form.area) : null
                    });
                    break;
                case 'FUND':
                    await API.addFundItem(userId, {
                        itemName: form.itemName, investedAmount: Number(form.investedAmount), region: form.region,
                        memo: form.memo || null, subType: form.subType || 'EQUITY_FUND',
                        managementFee: form.managementFee ? Number(form.managementFee) : null,
                        monthlyDepositAmount: form.monthlyDepositAmount ? Number(form.monthlyDepositAmount) : null,
                        depositDay: form.depositDay ? Number(form.depositDay) : null
                    });
                    break;
                case 'PENSION':
                    await API.addPensionItem(userId, {
                        itemName: form.itemName, investedAmount: Number(form.investedAmount), region: form.region,
                        memo: form.memo || null, subType: form.subType || 'IRP',
                        provider: form.provider || null,
                        evaluatedAmount: form.evaluatedAmount ? Number(form.evaluatedAmount) : null,
                        monthlyDepositAmount: form.monthlyDepositAmount ? Number(form.monthlyDepositAmount) : null,
                        depositDay: form.depositDay ? Number(form.depositDay) : null
                    });
                    break;
                case 'CASH':
                    await API.addCashItem(userId, {
                        itemName: form.itemName, investedAmount: Number(form.investedAmount), region: form.region,
                        memo: form.memo || null, cashType: form.cashType,
                        interestRate: form.interestRate ? Number(form.interestRate) : null,
                        startDate: form.startDate || null,
                        maturityDate: form.cashType !== 'CMA' ? (form.maturityDate || null) : null,
                        taxType: form.cashType !== 'CMA' ? (form.taxType || null) : null,
                        monthlyDepositAmount: form.monthlyDepositAmount ? Number(form.monthlyDepositAmount) : null,
                        depositDay: form.depositDay ? Number(form.depositDay) : null
                    });
                    break;
                case 'GENERAL':
                    await API.addGeneralItem(userId, {
                        assetType: form.assetType, itemName: form.itemName,
                        investedAmount: Number(form.investedAmount), region: form.region, memo: form.memo || null,
                        quantityGrams: form.assetType === 'GOLD' && form.quantityGrams
                            ? Number(form.quantityGrams) : null
                    });
                    break;
            }
            this.portfolio.showAddModal = false;
            await this.loadPortfolio();
        } catch (e) {
            console.error('자산 등록 실패:', e);
            alert('자산 등록에 실패했습니다.');
        }
    },

    closePurchaseModal() {
        this.portfolio.showPurchaseModal = false;
        this.portfolio.purchaseItem = null;
        this.portfolio.purchaseForm = { quantity: '', purchasePrice: '' };
        this.portfolio.purchaseHistories = [];
        this.portfolio.editingHistory = null;
        this.portfolio.editHistoryForm = { quantity: '', purchasePrice: '', purchasedAt: '', memo: '', fxRate: '' };
    },

    async openPurchaseModal(item) {
        if (!item || !item.stockDetail) {
            console.warn('Cannot open purchase modal without stockDetail', {
                id: item?.id,
                itemName: item?.itemName,
                assetType: item?.assetType
            });
            return;
        }
        this.portfolio.purchaseItem = item;
        this.portfolio.purchaseForm = { quantity: '', purchasePrice: '' };
        this.portfolio.purchaseHistories = [];
        this.portfolio.editingHistory = null;
        this.portfolio.showPurchaseModal = true;
        await this.loadPurchaseHistories(item.id);
    },

    async submitPurchase() {
        const form = this.portfolio.purchaseForm;
        const item = this.portfolio.purchaseItem;

        if (!form.quantity || Number(form.quantity) <= 0) {
            alert('매수 수량을 입력해주세요.');
            return;
        }
        if (!form.purchasePrice || Number(form.purchasePrice) <= 0) {
            alert('매수 단가를 입력해주세요.');
            return;
        }

        try {
            const response = await API.addStockPurchase(this.auth.userId, item.id, {
                quantity: Number(form.quantity),
                purchasePrice: Number(form.purchasePrice)
            });
            this.portfolio.purchaseForm = { quantity: '', purchasePrice: '' };
            this.portfolio.purchaseItem = { ...this.portfolio.purchaseItem, ...response };
            await this.loadPurchaseHistories(item.id);
            await this.loadPortfolio();
        } catch (e) {
            console.error('추가 매수 실패:', e);
            alert('추가 매수에 실패했습니다.');
        }
    },

    async loadPurchaseHistories(itemId) {
        try {
            this.portfolio.purchaseHistories = await API.getPurchaseHistories(this.auth.userId, itemId) || [];
        } catch (e) {
            console.error('매수이력 조회 실패:', e);
            this.portfolio.purchaseHistories = [];
        }
    },

    startEditHistory(history) {
        this.portfolio.editingHistory = history.id;
        this.portfolio.editHistoryForm = {
            quantity: history.quantity,
            purchasePrice: history.purchasePrice,
            purchasedAt: history.purchasedAt || '',
            memo: history.memo || '',
            // 폼에서 빼면 저장 시 서버가 환율을 null 로 덮어써 기록이 사라진다 (#110 review R3)
            fxRate: history.fxRate ?? ''
        };
    },

    cancelEditHistory() {
        this.portfolio.editingHistory = null;
    },

    async submitEditHistory(historyId) {
        const form = this.portfolio.editHistoryForm;
        const item = this.portfolio.purchaseItem;

        if (!form.quantity || Number(form.quantity) <= 0) {
            alert('수량을 입력해주세요.');
            return;
        }
        if (!form.purchasePrice || Number(form.purchasePrice) <= 0) {
            alert('매수 단가를 입력해주세요.');
            return;
        }

        try {
            await API.updatePurchaseHistory(this.auth.userId, item.id, historyId, {
                quantity: Number(form.quantity),
                purchasePrice: Number(form.purchasePrice),
                purchasedAt: form.purchasedAt || null,
                memo: form.memo || null,
                fxRate: form.fxRate === '' || form.fxRate === null ? null : Number(form.fxRate)
            });
            this.portfolio.editingHistory = null;
            await this.loadPurchaseHistories(item.id);
            await this.loadPortfolio();
        } catch (e) {
            console.error('매수이력 수정 실패:', e);
            alert('매수이력 수정에 실패했습니다.');
        }
    },

    async deleteHistory(historyId) {
        if (!confirm('이 매수 이력을 삭제하시겠습니까?\n삭제 후 수량과 평균단가가 재계산됩니다.')) return;
        const item = this.portfolio.purchaseItem;

        try {
            await API.deletePurchaseHistory(this.auth.userId, item.id, historyId);
            await this.loadPurchaseHistories(item.id);
            await this.loadPortfolio();
        } catch (e) {
            console.error('매수이력 삭제 실패:', e);
            alert(e.message || '매수이력 삭제에 실패했습니다.');
        }
    },

    // ─────────────────────────────────────────────────────────────────────
    // 매도 모달 / 매도 이력 / 사후 수정·삭제
    // ─────────────────────────────────────────────────────────────────────

    SALE_REASON_LABELS: {
        TARGET_PRICE_REACHED: '목표가 도달',
        STOP_LOSS: '손절',
        CASH_NEEDED: '현금 확보',
        REBALANCING: '리밸런싱',
        OTHER: '기타'
    },

    saleReasonLabel(reason) {
        return this.SALE_REASON_LABELS[reason] || reason || '기타';
    },

    hasSaleHistories(itemId) {
        return this.portfolio.userSalesItemIdsWithSales.includes(itemId);
    },

    closeSaleModal() {
        this.portfolio.showSaleModal = false;
        this.portfolio.saleItem = null;
        this.portfolio.saleForm = { quantity: '', salePrice: '', soldAt: '', reason: 'OTHER', memo: '', fxRate: '', deductionAmountKrw: '', netProceedsKrw: '', depositCashItemId: '', confirmUnrecorded: false };
        this.portfolio.saleContext = { currentPriceKrw: null, currentPriceOriginal: null, currency: 'KRW', fxRate: null, totalAsset: null };
        this.portfolio.salePreview = { profit: 0, profitRate: 0, contributionRate: 0, salePriceKrw: 0, profitKrw: 0, deductionAmountKrw: 0, netProceedsKrw: 0, netProfitKrw: 0, netProfitRate: 0, netContributionRate: 0 };
        this.portfolio.userCashItems = [];
    },

    async openSaleModal(item) {
        if (!item || !item.stockDetail) {
            console.warn('Cannot open sale modal without stockDetail', { id: item?.id, itemName: item?.itemName });
            return;
        }
        this.portfolio.saleItem = item;
        const today = new Date().toISOString().slice(0, 10);
        this.portfolio.saleForm = {
            quantity: item.stockDetail.quantity || '',
            salePrice: '',
            soldAt: today,
            reason: 'OTHER',
            memo: '',
            fxRate: '',
            deductionAmountKrw: '',
            netProceedsKrw: '',
            depositCashItemId: '',
            confirmUnrecorded: false
        };
        this.portfolio.saleContext = { currentPriceKrw: null, currentPriceOriginal: null, currency: item.stockDetail.priceCurrency || 'KRW', fxRate: null, totalAsset: null };
        this.portfolio.salePreview = { profit: 0, profitRate: 0, contributionRate: 0, salePriceKrw: 0, profitKrw: 0, deductionAmountKrw: 0, netProceedsKrw: 0, netProfitKrw: 0, netProfitRate: 0, netContributionRate: 0 };
        this.portfolio.userCashItems = (this.portfolio.items || []).filter(i => i.assetType === 'CASH');
        this.portfolio.showSaleModal = true;

        try {
            const ctx = await API.getSaleContext(this.auth.userId, item.id);
            this.portfolio.saleContext = ctx || this.portfolio.saleContext;

            // 자동 입력: 단가는 종목 통화 기준 currentPriceOriginal, 환율은 자동 조회 결과
            if (ctx && ctx.currentPriceOriginal != null) {
                this.portfolio.saleForm.salePrice = Number(ctx.currentPriceOriginal);
            }
            if (ctx && ctx.currency !== 'KRW' && ctx.fxRate != null) {
                this.portfolio.saleForm.fxRate = Number(ctx.fxRate);
            }
            this.recalculateSalePreview();
        } catch (e) {
            console.error('매도 컨텍스트 조회 실패:', e);
        }
    },

    recalculateSalePreview() {
        const item = this.portfolio.saleItem;
        if (!item || !item.stockDetail) return;

        const quantity = Number(this.portfolio.saleForm.quantity) || 0;
        const salePrice = Number(this.portfolio.saleForm.salePrice) || 0;
        const avgBuyPrice = Number(item.stockDetail.avgBuyPrice) || 0;
        const currency = this.portfolio.saleContext.currency || 'KRW';

        let fxRate = currency === 'KRW' ? 1 : Number(this.portfolio.saleForm.fxRate) || 0;

        const totalAsset = Number(this.portfolio.saleContext.totalAsset) || 0;

        const profit = (salePrice - avgBuyPrice) * quantity;
        const cost = avgBuyPrice * quantity;
        const profitRate = cost > 0 ? (profit / cost) * 100 : 0;

        let salePriceKrw = 0;
        let profitKrw = 0;
        if (fxRate > 0) {
            salePriceKrw = salePrice * fxRate * quantity;
            profitKrw = profit * fxRate;
        }

        const contributionBase = fxRate > 0 ? profitKrw : profit;
        const contributionRate = totalAsset > 0 ? (contributionBase / totalAsset) * 100 : 0;
        const settlement = this.resolveSaleSettlement(salePriceKrw, this.portfolio.saleForm);
        const costKrw = fxRate > 0 ? avgBuyPrice * fxRate * quantity : 0;
        const netProfitKrw = settlement.netProceedsKrw - costKrw;
        const netProfitRate = costKrw > 0 ? (netProfitKrw / costKrw) * 100 : 0;
        const netContributionRate = totalAsset > 0 ? (netProfitKrw / totalAsset) * 100 : 0;

        this.portfolio.salePreview = {
            profit: Math.round(profit * 100) / 100,
            profitRate: Math.round(profitRate * 100) / 100,
            contributionRate: Math.round(contributionRate * 100) / 100,
            salePriceKrw: Math.round(salePriceKrw * 100) / 100,
            profitKrw: Math.round(profitKrw * 100) / 100,
            deductionAmountKrw: Math.round(settlement.deductionAmountKrw * 100) / 100,
            netProceedsKrw: Math.round(settlement.netProceedsKrw * 100) / 100,
            netProfitKrw: Math.round(netProfitKrw * 100) / 100,
            netProfitRate: Math.round(netProfitRate * 100) / 100,
            netContributionRate: Math.round(netContributionRate * 100) / 100
        };
    },

    resolveSaleSettlement(grossProceedsKrw, form) {
        const directNet = form.netProceedsKrw !== '' && form.netProceedsKrw !== null && form.netProceedsKrw !== undefined;
        const directDeduction = form.deductionAmountKrw !== '' && form.deductionAmountKrw !== null && form.deductionAmountKrw !== undefined;
        if (directNet) {
            const netProceedsKrw = Math.max(0, Number(form.netProceedsKrw) || 0);
            return { netProceedsKrw, deductionAmountKrw: Math.max(0, grossProceedsKrw - netProceedsKrw) };
        }
        if (directDeduction) {
            const deductionAmountKrw = Math.max(0, Number(form.deductionAmountKrw) || 0);
            return { deductionAmountKrw, netProceedsKrw: Math.max(0, grossProceedsKrw - deductionAmountKrw) };
        }
        return { deductionAmountKrw: 0, netProceedsKrw: grossProceedsKrw };
    },

    updateSaleNetFromDeduction() {
        this.portfolio.saleForm.netProceedsKrw = '';
        this.recalculateSalePreview();
    },

    updateSaleDeductionFromNet() {
        this.recalculateSalePreview();
        this.portfolio.saleForm.deductionAmountKrw = this.portfolio.salePreview.deductionAmountKrw || '';
    },

    async submitSale() {
        const form = this.portfolio.saleForm;
        const item = this.portfolio.saleItem;
        if (!item || !item.stockDetail) return;

        if (!form.quantity || Number(form.quantity) <= 0) {
            alert('매도 수량을 입력해주세요.');
            return;
        }
        if (Number(form.quantity) > item.stockDetail.quantity) {
            alert(`보유 수량(${item.stockDetail.quantity})을 초과한 매도입니다.`);
            return;
        }
        if (!form.salePrice || Number(form.salePrice) <= 0) {
            alert('판매 단가를 입력해주세요.');
            return;
        }
        if (!form.soldAt) {
            alert('매도일을 입력해주세요.');
            return;
        }
        if (!form.reason) {
            alert('매도 사유를 선택해주세요.');
            return;
        }

        const currency = this.portfolio.saleContext.currency || 'KRW';
        if (currency !== 'KRW' && (!form.fxRate || Number(form.fxRate) <= 0)) {
            alert('환율을 입력해주세요. 자동 조회에 실패한 경우 직접 입력이 필요합니다.');
            return;
        }
        if (form.deductionAmountKrw !== '' && Number(form.deductionAmountKrw) < 0) {
            alert('차감액은 0원 이상이어야 합니다.');
            return;
        }
        if (form.netProceedsKrw !== '' && Number(form.netProceedsKrw) < 0) {
            alert('실입금액은 0원 이상이어야 합니다.');
            return;
        }
        if (form.netProceedsKrw !== '' && Number(form.netProceedsKrw) > this.portfolio.salePreview.salePriceKrw) {
            alert('실입금액은 총 체결금액보다 클 수 없습니다.');
            return;
        }
        if (form.deductionAmountKrw !== '' && Number(form.deductionAmountKrw) > this.portfolio.salePreview.salePriceKrw) {
            alert('차감액은 총 체결금액보다 클 수 없습니다.');
            return;
        }

        const link = this.portfolio.userCashItems.length === 0;
        if (link && !form.confirmUnrecorded) {
            // CASH 항목이 없는 경우 사용자 명시 확인 (서버가 unrecordedDeposit=true로 처리)
        }

        const payload = {
            quantity: Number(form.quantity),
            salePrice: Number(form.salePrice),
            soldAt: form.soldAt,
            reason: form.reason,
            memo: form.memo || null,
            fxRate: form.fxRate ? Number(form.fxRate) : null,
            deductionAmountKrw: form.deductionAmountKrw !== '' ? Number(form.deductionAmountKrw) : null,
            netProceedsKrw: form.netProceedsKrw !== '' ? Number(form.netProceedsKrw) : null,
            depositCashItemId: form.depositCashItemId ? Number(form.depositCashItemId) : null
        };

        try {
            await API.addStockSale(this.auth.userId, item.id, payload);
            this.closeSaleModal();
            await this.loadPortfolio();
            if (this.portfolio.activeTab === 'sales') {
                await this.loadAllUserSales();
            }
        } catch (e) {
            console.error('매도 등록 실패:', e);
            alert(e.message || '매도 등록에 실패했습니다.');
        }
    },

    async loadSaleItemIds() {
        try {
            const ids = await API.getSaleItemIds(this.auth.userId) || [];
            this.portfolio.userSalesItemIdsWithSales = ids;
        } catch (e) {
            console.error('매도 이력 itemId 조회 실패:', e);
            this.portfolio.userSalesItemIdsWithSales = [];
        }
    },

    async loadAllUserSales() {
        this.portfolio.userSalesLoading = true;
        try {
            const sales = await API.getAllUserSaleHistories(this.auth.userId) || [];
            this.portfolio.userSales = sales;
            this.portfolio.userSalesItemIdsWithSales = [...new Set(sales.map(s => s.portfolioItemId))];
        } catch (e) {
            console.error('매도 이력 조회 실패:', e);
            this.portfolio.userSales = [];
            this.portfolio.userSalesItemIdsWithSales = [];
        } finally {
            this.portfolio.userSalesLoading = false;
        }
    },

    groupSalesByMonth() {
        const groups = {};
        for (const sale of this.portfolio.userSales) {
            const month = (sale.soldAt || '').slice(0, 7);
            if (!groups[month]) {
                groups[month] = { month, items: [], totalSaleAmount: 0, totalProfit: 0 };
            }
            groups[month].items.push(sale);
            groups[month].totalSaleAmount += Number(this.saleNetProceeds(sale) || 0);
            groups[month].totalProfit += Number(this.saleNetProfit(sale) || 0);
        }
        return Object.values(groups).sort((a, b) => b.month.localeCompare(a.month));
    },

    getSalesSummary() {
        const sales = this.portfolio.userSales;
        const totalProceeds = sales.reduce((sum, s) => sum + Number(this.saleNetProceeds(s) || 0), 0);
        const totalProfit = sales.reduce((sum, s) => sum + Number(this.saleNetProfit(s) || 0), 0);
        // 원가 = 실입금 − 손익 (KRW 기준이라 해외 매도 통화 혼합 없이 안전)
        const cost = totalProceeds - totalProfit;
        const profitRate = cost > 0 ? Math.round(totalProfit / cost * 10000) / 100 : null;
        return { count: sales.length, totalProceeds, totalProfit, profitRate };
    },

    saleNetProceeds(sale) {
        return sale?.netProceedsKrw ?? sale?.salePriceKrw ?? 0;
    },

    saleNetProfit(sale) {
        return sale?.netProfitKrw ?? sale?.profitKrw ?? sale?.profit ?? 0;
    },

    // 탭: holdings / sales / targets / analysis (#110)
    setActiveTab(tab) {
        this.portfolio.activeTab = tab;
        if (tab === 'sales') {
            this.loadAllUserSales();
        }
        if (tab === 'analysis') {
            // 분석 탭은 재무 슬라이드 패널을 재사용한다. 탭 이탈 시 열려 있던 패널은 닫는다.
            return;
        }
        // selectedStockItem 만 비우면 재무 패널의 Chart.js 인스턴스가 남는다 → closeStockDetail 로 정리
        if (this.portfolio.selectedStockItem) {
            this.closeStockDetail();
        }
    },

    openSaleDetailModal(history) {
        this.portfolio.saleDetail = history;
        this.portfolio.editingSaleHistory = false;
        this.portfolio.editSaleForm = this.createSaleEditForm(history);
        this.portfolio.showSaleDetailModal = true;
    },

    openSaleEditModal(history) {
        this.portfolio.saleDetail = history;
        this.portfolio.editSaleForm = this.createSaleEditForm(history);
        this.portfolio.editingSaleHistory = true;
        this.portfolio.showSaleDetailModal = true;
    },

    closeSaleDetailModal() {
        this.portfolio.showSaleDetailModal = false;
        this.portfolio.saleDetail = null;
        this.portfolio.editingSaleHistory = false;
    },

    startEditSaleHistory() {
        const history = this.portfolio.saleDetail;
        if (!history) return;
        this.portfolio.editSaleForm = this.createSaleEditForm(history);
        this.portfolio.editingSaleHistory = true;
    },

    createSaleEditForm(history) {
        return {
            quantity: history.quantity,
            salePrice: history.salePrice,
            deductionAmountKrw: '',
            netProceedsKrw: history.netProceedsKrw ?? '',
            reason: history.reason,
            memo: history.memo || ''
        };
    },

    cancelEditSaleHistory() {
        this.portfolio.editingSaleHistory = false;
    },

    async submitSaleEdit() {
        const history = this.portfolio.saleDetail;
        const form = this.portfolio.editSaleForm;
        if (!history) return;

        if (!form.quantity || Number(form.quantity) <= 0) {
            alert('수량을 입력해주세요.');
            return;
        }
        if (!form.salePrice || Number(form.salePrice) <= 0) {
            alert('판매 단가를 입력해주세요.');
            return;
        }
        if (form.deductionAmountKrw !== '' && Number(form.deductionAmountKrw) < 0) {
            alert('차감액은 0원 이상이어야 합니다.');
            return;
        }
        if (form.netProceedsKrw !== '' && Number(form.netProceedsKrw) < 0) {
            alert('실입금액은 0원 이상이어야 합니다.');
            return;
        }

        try {
            const updated = await API.updateSaleHistory(this.auth.userId, history.portfolioItemId, history.id, {
                quantity: Number(form.quantity),
                salePrice: Number(form.salePrice),
                deductionAmountKrw: form.deductionAmountKrw !== '' ? Number(form.deductionAmountKrw) : null,
                netProceedsKrw: form.netProceedsKrw !== '' ? Number(form.netProceedsKrw) : null,
                reason: form.reason,
                memo: form.memo || null
            });
            this.portfolio.saleDetail = updated;
            this.portfolio.editingSaleHistory = false;
            await this.loadAllUserSales();
            await this.loadPortfolio();
        } catch (e) {
            console.error('매도 이력 수정 실패:', e);
            alert(e.message || '매도 이력 수정에 실패했습니다.');
        }
    },

    async confirmDeleteSale() {
        const history = this.portfolio.saleDetail;
        if (!history) return;
        if (!confirm('이 매도 이력을 삭제하시겠습니까?\n보유 수량 복원, CASH 차감(미입금 제외), 마감 항목은 보유 상태로 복원됩니다.')) return;

        try {
            await API.deleteSaleHistory(this.auth.userId, history.portfolioItemId, history.id);
            this.closeSaleDetailModal();
            await this.loadAllUserSales();
            await this.loadPortfolio();
        } catch (e) {
            console.error('매도 이력 삭제 실패:', e);
            alert(e.message || '매도 이력 삭제에 실패했습니다.');
        }
    },

    openEditModal(item) {
        this.portfolio.editingItem = item;
        this.portfolio.stockSearch = { query: '', results: [], loading: false, selected: null, debounceTimer: null };

        const form = {
            itemName: item.itemName,
            investedAmount: item.investedAmount,
            region: item.region || 'DOMESTIC',
            memo: item.memo || ''
        };

        switch (item.assetType) {
            case 'STOCK':
                if (item.stockDetail) {
                    form.subType = item.stockDetail.subType;
                    form.ticker = item.stockDetail.stockCode;
                    form.exchange = item.stockDetail.market;
                    form.exchangeCode = item.stockDetail.exchangeCode;
                    form.country = item.stockDetail.country;
                    form.quantity = item.stockDetail.quantity;
                    form.purchasePrice = item.stockDetail.avgBuyPrice;
                    form.dividendYield = item.stockDetail.dividendYield;
                    form.priceCurrency = item.stockDetail.priceCurrency || 'KRW';
                    form.investedAmountKrw = item.stockDetail.investedAmountKrw;
                }
                form.cashItemId = item.linkedCashItemId != null ? String(item.linkedCashItemId) : '';
                break;
            case 'BOND':
                if (item.bondDetail) {
                    form.subType = item.bondDetail.subType;
                    form.maturityDate = item.bondDetail.maturityDate;
                    form.couponRate = item.bondDetail.couponRate;
                    form.creditRating = item.bondDetail.creditRating;
                }
                break;
            case 'REAL_ESTATE':
                if (item.realEstateDetail) {
                    form.subType = item.realEstateDetail.subType;
                    form.address = item.realEstateDetail.address;
                    form.area = item.realEstateDetail.area;
                }
                break;
            case 'FUND':
                if (item.fundDetail) {
                    form.subType = item.fundDetail.subType;
                    form.managementFee = item.fundDetail.managementFee;
                    form.monthlyDepositAmount = item.fundDetail.monthlyDepositAmount;
                    form.depositDay = item.fundDetail.depositDay;
                }
                break;
            case 'CASH':
                if (item.cashDetail) {
                    form.cashType = item.cashDetail.subType;
                    form.interestRate = item.cashDetail.interestRate;
                    form.startDate = item.cashDetail.startDate;
                    form.maturityDate = item.cashDetail.maturityDate;
                    form.taxType = item.cashDetail.taxType;
                    form.monthlyDepositAmount = item.cashDetail.monthlyDepositAmount;
                    form.depositDay = item.cashDetail.depositDay;
                }
                break;
            case 'PENSION':
                if (item.pensionDetail) {
                    form.subType = item.pensionDetail.subType;
                    form.provider = item.pensionDetail.provider;
                    form.evaluatedAmount = item.pensionDetail.evaluatedAmount;
                    form.monthlyDepositAmount = item.pensionDetail.monthlyDepositAmount;
                    form.depositDay = item.pensionDetail.depositDay;
                }
                break;
            case 'GOLD':
                form.quantityGrams = item.goldDetail ? item.goldDetail.quantityGrams : '';
                break;
        }

        this.portfolio.editForm = form;
        this.portfolio.showEditModal = true;
    },

    async searchStockForEdit() {
        const query = this.portfolio.stockSearch.query.trim();
        if (!query) return;
        this.portfolio.stockSearch.loading = true;
        try {
            this.portfolio.stockSearch.results = await API.searchStocks(query) || [];
        } catch (e) {
            console.error('종목 검색 실패:', e);
            this.portfolio.stockSearch.results = [];
        } finally {
            this.portfolio.stockSearch.loading = false;
        }
    },

    selectStockForEdit(stock) {
        this.portfolio.stockSearch.selected = stock;
        this.portfolio.stockSearch.results = [];
        this.portfolio.stockSearch.query = '';
        this.portfolio.editForm.itemName = stock.stockName;
        this.portfolio.editForm.ticker = stock.stockCode;
        this.portfolio.editForm.exchange = stock.marketType;
        this.portfolio.editForm.exchangeCode = stock.exchangeCode;
        this.portfolio.editForm.priceCurrency = this.getCurrencyByExchangeCode(stock.exchangeCode);
    },

    async submitEditItem() {
        const item = this.portfolio.editingItem;
        const form = this.portfolio.editForm;
        const userId = this.auth.userId;

        if (!form.itemName) {
            alert('항목명은 필수입니다.');
            return;
        }
        if (item.assetType === 'STOCK') {
            if (!form.quantity || Number(form.quantity) <= 0) {
                alert('매수 수량은 필수입니다.');
                return;
            }
            if (!form.purchasePrice || Number(form.purchasePrice) <= 0) {
                alert('매수가는 필수입니다.');
                return;
            }
        } else {
            if (!form.investedAmount || Number(form.investedAmount) <= 0) {
                alert('투자 금액은 0보다 커야 합니다.');
                return;
            }
        }

        try {
            switch (item.assetType) {
                case 'STOCK':
                    const stockDetail = item.stockDetail || {};
                    const newCashItemId = form.cashItemId ? Number(form.cashItemId) : null;
                    const oldCashItemId = item.linkedCashItemId || null;
                    const isNewLink = newCashItemId && !oldCashItemId;
                    let deductOnLink = false;

                    if (isNewLink) {
                        deductOnLink = confirm(
                            '이미 보유 중인 주식에 원화를 연결합니다.\n' +
                            '현재 투자금(' + Number(item.investedAmount).toLocaleString() + '원)을 원화에서 차감할까요?\n\n' +
                            '확인: 차감함 (신규 매수와 동일)\n' +
                            '취소: 연결만 (이후 추가 매수부터 차감)'
                        );
                    }

                    await API.updateStockItem(userId, item.id, {
                        itemName: form.itemName, memo: form.memo || null,
                        subType: form.subType || 'INDIVIDUAL',
                        stockCode: stockDetail.stockCode, market: stockDetail.market,
                        exchangeCode: stockDetail.exchangeCode, country: stockDetail.country,
                        quantity: Number(form.quantity), purchasePrice: Number(form.purchasePrice),
                        dividendYield: form.dividendYield ? Number(form.dividendYield) : null,
                        priceCurrency: form.priceCurrency || 'KRW',
                        investedAmountKrw: form.investedAmountKrw ? Number(form.investedAmountKrw) : null,
                        cashItemId: newCashItemId, deductOnLink: deductOnLink
                    });
                    break;
                case 'BOND':
                    await API.updateBondItem(userId, item.id, {
                        itemName: form.itemName, investedAmount: Number(form.investedAmount),
                        memo: form.memo || null, subType: form.subType || 'GOVERNMENT',
                        maturityDate: form.maturityDate || null,
                        couponRate: form.couponRate ? Number(form.couponRate) : null,
                        creditRating: form.creditRating || null
                    });
                    break;
                case 'REAL_ESTATE':
                    await API.updateRealEstateItem(userId, item.id, {
                        itemName: form.itemName, investedAmount: Number(form.investedAmount),
                        memo: form.memo || null, subType: form.subType || 'APARTMENT',
                        address: form.address || null, area: form.area ? Number(form.area) : null
                    });
                    break;
                case 'FUND':
                    await API.updateFundItem(userId, item.id, {
                        itemName: form.itemName, investedAmount: Number(form.investedAmount),
                        memo: form.memo || null, subType: form.subType || 'EQUITY_FUND',
                        managementFee: form.managementFee ? Number(form.managementFee) : null,
                        monthlyDepositAmount: form.monthlyDepositAmount ? Number(form.monthlyDepositAmount) : null,
                        depositDay: form.depositDay ? Number(form.depositDay) : null
                    });
                    break;
                case 'CASH':
                    await API.updateCashItem(userId, item.id, {
                        itemName: form.itemName, investedAmount: Number(form.investedAmount),
                        memo: form.memo || null,
                        interestRate: form.interestRate ? Number(form.interestRate) : null,
                        startDate: form.startDate || null,
                        maturityDate: form.cashType !== 'CMA' ? (form.maturityDate || null) : null,
                        taxType: form.cashType !== 'CMA' ? (form.taxType || null) : null,
                        monthlyDepositAmount: form.monthlyDepositAmount ? Number(form.monthlyDepositAmount) : null,
                        depositDay: form.depositDay ? Number(form.depositDay) : null
                    });
                    break;
                case 'PENSION':
                    await API.updatePensionItem(userId, item.id, {
                        itemName: form.itemName, investedAmount: Number(form.investedAmount),
                        memo: form.memo || null, subType: form.subType || 'IRP',
                        provider: form.provider || null,
                        evaluatedAmount: form.evaluatedAmount ? Number(form.evaluatedAmount) : null,
                        monthlyDepositAmount: form.monthlyDepositAmount ? Number(form.monthlyDepositAmount) : null,
                        depositDay: form.depositDay ? Number(form.depositDay) : null
                    });
                    break;
                default:
                    await API.updateGeneralItem(userId, item.id, {
                        itemName: form.itemName, investedAmount: Number(form.investedAmount),
                        memo: form.memo || null,
                        quantityGrams: item.assetType === 'GOLD' && form.quantityGrams
                            ? Number(form.quantityGrams) : null
                    });
                    break;
            }
            this.portfolio.showEditModal = false;
            this.portfolio.editingItem = null;
            await this.loadPortfolio();
        } catch (e) {
            console.error('자산 수정 실패:', e);
            alert('수정에 실패했습니다.');
        }
    },

    // ──────────────────────────────────────────────────────────────────
    // 납입 이력 CRUD
    // ──────────────────────────────────────────────────────────────────

    isDepositTarget(item) {
        return item && (item.assetType === 'CASH' || item.assetType === 'FUND' || item.assetType === 'PENSION');
    },

    async openDepositModal(item) {
        this.portfolio.depositItem = item;
        this.portfolio.depositForm = { depositDate: new Date().toISOString().split('T')[0], amount: '', units: '', memo: '' };
        this.portfolio.depositHistories = [];
        this.portfolio.editingDeposit = null;
        this.portfolio.showDepositModal = true;
        await this.loadDepositHistories(item.id);
    },

    async loadDepositHistories(itemId) {
        try {
            this.portfolio.depositHistories = await API.getDepositHistories(this.auth.userId, itemId) || [];
        } catch (e) {
            console.error('납입이력 조회 실패:', e);
            this.portfolio.depositHistories = [];
        }
    },

    refreshDepositItem() {
        const current = this.portfolio.depositItem;
        if (!current) return;
        const fresh = this.portfolio.items.find((i) => i.id === current.id);
        if (fresh) this.portfolio.depositItem = fresh;
    },

    // ──────────────────────────────────────────────────────────────────
    // 미납 납입 리마인더 (#100)
    // ──────────────────────────────────────────────────────────────────

    // 스누즈는 사용자 로컬 하루 기준 (toISOString 은 UTC 라 KST 오전에 날짜가 어긋남)
    _depositReminderToday() {
        const d = new Date();
        return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
    },

    _depositReminderSnoozedToday() {
        try {
            return localStorage.getItem('depositReminderSnoozeDate') === this._depositReminderToday();
        } catch (e) {
            return false;
        }
    },

    async checkDepositReminder() {
        try {
            if (this._depositReminderSnoozedToday()) return;
            const items = await API.getPortfolioItems(this.auth.userId) || [];
            const overdue = items.filter((i) => i.depositOverdue === true);
            if (overdue.length === 0) return;
            this.portfolio.depositReminder.items = overdue;
            this.portfolio.depositReminder.snoozeChecked = false;
            this.portfolio.depositReminder.show = true;
        } catch (e) {
            console.error('미납 납입 리마인더 확인 실패:', e);
        }
    },

    depositReminderMeta(item) {
        const detail = item.assetType === 'FUND' ? item.fundDetail
            : item.assetType === 'CASH' ? item.cashDetail
                : item.assetType === 'PENSION' ? item.pensionDetail : null;
        if (!detail) return '';
        const parts = [];
        if (detail.monthlyDepositAmount) parts.push('월 ' + Format.number(detail.monthlyDepositAmount, 0) + '원');
        if (detail.depositDay) parts.push('매월 ' + detail.depositDay + '일');
        return parts.join(' · ');
    },

    async openDepositFromReminder(item) {
        this.portfolio.depositReminder.show = false;
        if (this.currentPage !== 'portfolio') {
            await this.navigateTo('portfolio');
        }
        await this.openDepositModal(item);
    },

    closeDepositReminder() {
        if (this.portfolio.depositReminder.snoozeChecked) {
            try {
                localStorage.setItem('depositReminderSnoozeDate', this._depositReminderToday());
            } catch (e) { /* localStorage 불가 시 스누즈 없이 매번 노출 */ }
        }
        this.portfolio.depositReminder.show = false;
    },

    async submitDeposit() {
        const form = this.portfolio.depositForm;
        const item = this.portfolio.depositItem;

        if (!form.amount || Number(form.amount) <= 0) {
            alert('납입 금액을 입력해주세요.');
            return;
        }

        try {
            await API.addDeposit(this.auth.userId, item.id, {
                depositDate: form.depositDate || null,
                amount: Number(form.amount),
                units: form.units ? Number(form.units) : null,
                memo: form.memo || null
            });
            this.portfolio.depositForm = { depositDate: new Date().toISOString().split('T')[0], amount: '', units: '', memo: '' };
            await this.loadDepositHistories(item.id);
            await this.loadPortfolio();
            this.refreshDepositItem();
            // 납입 처리된 항목은 리마인더 목록에서 제거 (#100)
            this.portfolio.depositReminder.items =
                this.portfolio.depositReminder.items.filter((r) => r.id !== item.id);
        } catch (e) {
            console.error('납입 추가 실패:', e);
            alert('납입 추가에 실패했습니다.');
        }
    },

    startEditDeposit(history) {
        this.portfolio.editingDeposit = history.id;
        this.portfolio.editDepositForm = {
            depositDate: history.depositDate || '',
            amount: history.amount,
            units: history.units || '',
            memo: history.memo || ''
        };
    },

    cancelEditDeposit() {
        this.portfolio.editingDeposit = null;
    },

    async submitEditDeposit(historyId) {
        const form = this.portfolio.editDepositForm;
        const item = this.portfolio.depositItem;

        if (!form.amount || Number(form.amount) <= 0) {
            alert('납입 금액을 입력해주세요.');
            return;
        }

        try {
            await API.updateDeposit(this.auth.userId, item.id, historyId, {
                depositDate: form.depositDate || null,
                amount: Number(form.amount),
                units: form.units ? Number(form.units) : null,
                memo: form.memo || null
            });
            this.portfolio.editingDeposit = null;
            await this.loadDepositHistories(item.id);
            await this.loadPortfolio();
            this.refreshDepositItem();
        } catch (e) {
            console.error('납입이력 수정 실패:', e);
            alert('납입이력 수정에 실패했습니다.');
        }
    },

    async deleteDeposit(historyId) {
        if (!confirm('이 납입 이력을 삭제하시겠습니까?\n삭제 후 투자금액이 재계산됩니다.')) return;
        const item = this.portfolio.depositItem;

        try {
            await API.deleteDeposit(this.auth.userId, item.id, historyId);
            await this.loadDepositHistories(item.id);
            await this.loadPortfolio();
            this.refreshDepositItem();
        } catch (e) {
            console.error('납입이력 삭제 실패:', e);
            alert(e.message || '납입이력 삭제에 실패했습니다.');
        }
    },

    getDepositTotal() {
        return this.portfolio.depositHistories.reduce((sum, h) => sum + (h.amount || 0), 0);
    }
};
