/**
 * PortfolioComponent - 포트폴리오 CRUD, 주가검색, 자산배분, 뉴스, 매수이력
 * 소유 프로퍼티: portfolio (financial 관련 상태 포함)
 */
const PortfolioComponent = {
    portfolio: {
        items: [],
        allocation: [],
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
        editHistoryForm: { quantity: '', purchasePrice: '', purchasedAt: '', memo: '' },
        showDepositModal: false,
        depositItem: null,
        depositForm: { depositDate: '', amount: '', units: '', memo: '' },
        depositHistories: [],
        editingDeposit: null,
        editDepositForm: { depositDate: '', amount: '', units: '', memo: '' },
        selectedNewsItemId: null,
        news: { list: [], page: 0, size: 20, totalPages: 0, totalElements: 0, loading: false },
        collectingItemId: null,
        chartInstance: null,
        // 재무정보 상태
        financialChartInstance: null,
        financialOptions: null,
        financialResult: null,
        financialLoading: false,
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
        financialMenus: [
            { key: 'accounts', label: '재무계정' },
            { key: 'indices', label: '재무지표' },
            { key: 'full-statements', label: '전체재무제표' },
            { key: 'stock-quantities', label: '주식수량' },
            { key: 'dividends', label: '배당정보' },
            { key: 'lawsuits', label: '소송현황' },
            { key: 'private-fund', label: '사모자금사용' },
            { key: 'public-fund', label: '공모자금사용' }
        ],
        _krFinancialMenus: [
            { key: 'accounts', label: '재무계정' },
            { key: 'indices', label: '재무지표' },
            { key: 'full-statements', label: '전체재무제표' },
            { key: 'stock-quantities', label: '주식수량' },
            { key: 'dividends', label: '배당정보' },
            { key: 'lawsuits', label: '소송현황' },
            { key: 'private-fund', label: '사모자금사용' },
            { key: 'public-fund', label: '공모자금사용' }
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
        secEdgarUrl: null,
        // 해외뉴스 상태
        _overseasNewsGeneration: 0,
        _overseasNewsDebounceTimer: null,
        overseasNews: {
            selectedItemId: null,
            activeTab: 'breaking',
            breaking: { list: [], loading: false, error: null },
            comprehensive: { list: [], loading: false, error: null, hasMore: false, lastDt: '', lastTm: '' }
        }
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
                API.getPortfolioAllocation(this.auth.userId)
            ]);
            this.portfolio.items = results[0] || [];
            this.portfolio.allocation = results[1] || [];

            await this.loadStockPrices();

            const sections = {};
            this.portfolio.items.forEach((item) => {
                if (sections[item.assetType] === undefined) {
                    sections[item.assetType] = true;
                }
            });
            this.portfolio.expandedSections = sections;
        } catch (e) {
            console.error('포트폴리오 로드 실패:', e);
            this.portfolio.items = [];
            this.portfolio.allocation = [];
        } finally {
            this.portfolio.loading = false;
            this.portfolio._initialized = true;
            this.$nextTick(() => {
                this.renderDonutChart();
            });
        }
    },

    async loadStockPrices() {
        const stockItems = this.portfolio.items.filter((item) => item.assetType === 'STOCK' && item.stockDetail);
        if (stockItems.length === 0) {
            this.portfolio.stockPrices = {};
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
    },

    getEvalAmount(item) {
        if (item.assetType === 'STOCK' && item.stockDetail) {
            const priceData = this.portfolio.stockPrices[item.stockDetail.stockCode];
            if (priceData && priceData.currentPriceKrw) {
                return parseFloat(priceData.currentPriceKrw) * item.stockDetail.quantity;
            }
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

    getProfitRate(item) {
        if (item.assetType !== 'STOCK' || !item.stockDetail || !item.stockDetail.avgBuyPrice) return null;
        const priceData = this.portfolio.stockPrices[item.stockDetail.stockCode];
        if (!priceData || !priceData.currentPrice) return null;
        const currentPrice = parseFloat(priceData.currentPrice);
        const avgPrice = parseFloat(item.stockDetail.avgBuyPrice);
        if (avgPrice === 0) return null;
        return ((currentPrice - avgPrice) / avgPrice * 100);
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

        const assetTypeOrder = ['STOCK', 'BOND', 'REAL_ESTATE', 'FUND', 'OTHER', 'CRYPTO', 'GOLD', 'COMMODITY', 'CASH'];
        return Object.values(typeMap).map((alloc) => {
            alloc.percentage = Math.round(alloc.totalAmount / totalEval * 1000) / 10;
            return alloc;
        }).sort((a, b) => assetTypeOrder.indexOf(a.assetType) - assetTypeOrder.indexOf(b.assetType));
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
        return this.portfolio.items.filter((item) => item.assetType === 'STOCK' && item.stockDetail?.country !== 'KR');
    },

    getTotalInvested() {
        return this.portfolio.items.reduce((sum, item) => sum + this.getInvestedAmountKrw(item), 0);
    },

    getSubTotalInvested(assetType) {
        return this.portfolio.items
            .filter((item) => item.assetType === assetType)
            .reduce((sum, item) => sum + this.getInvestedAmountKrw(item), 0);
    },

    getSubTotalEvalAmount(assetType) {
        return this.portfolio.items
            .filter((item) => item.assetType === assetType)
            .reduce((sum, item) => sum + this.getEvalAmount(item), 0);
    },

    getSubTotalProfitRate(assetType) {
        const invested = this.getSubTotalInvested(assetType);
        if (invested === 0) return null;
        const evalAmount = this.getSubTotalEvalAmount(assetType);
        return ((evalAmount - invested) / invested * 100);
    },

    getNewsEnabledCount() {
        return this.portfolio.items.filter((item) => item.newsEnabled).length;
    },

    toggleSection(assetType) {
        this.portfolio.expandedSections[assetType] = !this.portfolio.expandedSections[assetType];
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
                    legend: {
                        display: true,
                        position: 'bottom',
                        labels: {
                            padding: 16,
                            usePointStyle: true,
                            pointStyle: 'circle',
                            font: { size: 12 },
                            generateLabels: (chart) => {
                                const dataset = chart.data.datasets[0];
                                const total = dataset.data.reduce((sum, val) => sum + val, 0);
                                return chart.data.labels.map((label, i) => {
                                    const pct = total > 0 ? Math.round(dataset.data[i] / total * 1000) / 10 : 0;
                                    return {
                                        text: label + ' ' + pct + '%',
                                        fillStyle: dataset.backgroundColor[i],
                                        strokeStyle: '#ffffff',
                                        lineWidth: 1,
                                        hidden: false,
                                        index: i,
                                        pointStyle: 'circle'
                                    };
                                });
                            }
                        }
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

    getStockPriceSummary(item) {
        if (item.assetType !== 'STOCK' || !item.stockDetail) return '';
        const priceData = this.portfolio.stockPrices[item.stockDetail.stockCode];
        if (!priceData || !priceData.currentPrice) return '';
        const currency = priceData.currency || 'KRW';
        const priceDisplay = currency === 'KRW'
            ? '현재가 ' + Format.number(priceData.currentPrice) + '원'
            : '현재가 ' + Format.number(priceData.currentPrice, 2) + ' ' + currency;
        const parts = [priceDisplay];
        const rate = this.getProfitRate(item);
        if (rate !== null) {
            const sign = rate >= 0 ? '+' : '';
            parts.push(sign + rate.toFixed(2) + '%');
        }
        parts.push('총 ' + Format.number(this.getEvalAmount(item), 0) + '원');
        return parts.join(' · ');
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

    async toggleNews(item) {
        try {
            await API.togglePortfolioNews(this.auth.userId, item.id, !item.newsEnabled);
            await this.loadPortfolio();
        } catch (e) {
            console.error('뉴스 토글 실패:', e);
            alert('뉴스 설정 변경에 실패했습니다.');
        }
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
                        investedAmount: Number(form.investedAmount), region: form.region, memo: form.memo || null
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

    async findKeywordIdByItemName(itemName, region) {
        const keywords = await API.getKeywords(this.auth.userId) || [];
        const matched = keywords.find(k => k.keyword === itemName && k.region === region);
        return matched ? matched.id : null;
    },

    async selectPortfolioNewsItem(item) {
        if (this.portfolio.selectedNewsItemId === item.id) {
            this.portfolio.selectedNewsItemId = null;
            this.portfolio.selectedNewsKeywordId = null;
            this.portfolio.news = { list: [], page: 0, size: 20, totalPages: 0, totalElements: 0, loading: false };
            return;
        }
        this.portfolio.selectedNewsItemId = item.id;
        this.portfolio.selectedNewsKeywordId = await this.findKeywordIdByItemName(item.itemName, item.region);
        this.portfolio.news.page = 0;
        await this.loadPortfolioNews(0);
    },

    async loadPortfolioNews(page) {
        if (!this.portfolio.selectedNewsKeywordId) return;
        this.portfolio.news.loading = true;
        try {
            const result = await API.getNewsByKeyword(this.portfolio.selectedNewsKeywordId, page, this.portfolio.news.size);
            this.portfolio.news.list = result.content || [];
            this.portfolio.news.page = result.page;
            this.portfolio.news.totalPages = result.totalPages;
            this.portfolio.news.totalElements = result.totalElements;
        } catch (e) {
            console.error('포트폴리오 뉴스 로드 실패:', e);
            this.portfolio.news.list = [];
        } finally {
            this.portfolio.news.loading = false;
        }
    },

    async collectPortfolioNews(item) {
        if (this.portfolio.collectingItemId) return;
        this.portfolio.collectingItemId = item.id;
        try {
            const keywordId = await this.findKeywordIdByItemName(item.itemName, item.region);
            if (!keywordId) {
                alert('키워드를 찾을 수 없습니다. 뉴스를 먼저 활성화해주세요.');
                return;
            }
            const result = await API.collectNewsByKeyword(keywordId, item.itemName, item.region);
            let msg = '수집 완료: ' + result.successCount + '건 저장';
            if (result.ignoredCount > 0) msg += ', ' + result.ignoredCount + '건 중복';
            alert(msg);

            if (this.portfolio.selectedNewsItemId === item.id) {
                await this.loadPortfolioNews(0);
            }
        } catch (e) {
            console.error('포트폴리오 뉴스 수집 실패:', e);
            alert('뉴스 수집에 실패했습니다.');
        } finally {
            this.portfolio.collectingItemId = null;
        }
    },

    // ==================== 해외뉴스 (속보/뉴스종합) ====================

    toggleOverseasNews(item) {
        const news = this.portfolio.overseasNews;

        // 기존 키워드 뉴스 패널 닫기
        if (this.portfolio.selectedNewsItemId) {
            this.portfolio.selectedNewsItemId = null;
            this.portfolio.selectedNewsKeywordId = null;
            this.portfolio.news = { list: [], page: 0, size: 20, totalPages: 0, totalElements: 0, loading: false };
        }

        // 동일 종목 재클릭 → 토글 닫기
        if (news.selectedItemId === item.id) {
            this.portfolio._overseasNewsGeneration++;
            clearTimeout(this.portfolio._overseasNewsDebounceTimer);
            news.selectedItemId = null;
            news.activeTab = 'breaking';
            news.breaking = { list: [], loading: false, error: null };
            news.comprehensive = { list: [], loading: false, error: null, hasMore: false, lastDt: '', lastTm: '' };
            return;
        }

        // 새 종목 열기
        this.portfolio._overseasNewsGeneration++;
        clearTimeout(this.portfolio._overseasNewsDebounceTimer);
        news.selectedItemId = item.id;
        news.activeTab = 'breaking';
        news.breaking = { list: [], loading: false, error: null };
        news.comprehensive = { list: [], loading: false, error: null, hasMore: false, lastDt: '', lastTm: '' };
        this._overseasNewsItem = item;
        this.loadOverseasNews('breaking');
    },

    switchOverseasNewsTab(tab) {
        const news = this.portfolio.overseasNews;
        news.activeTab = tab;

        clearTimeout(this.portfolio._overseasNewsDebounceTimer);
        this.portfolio._overseasNewsDebounceTimer = setTimeout(() => {
            this.loadOverseasNews(tab);
        }, 200);
    },

    async loadOverseasNews(tab) {
        const item = this._overseasNewsItem;
        if (!item || !item.stockDetail) return;

        const gen = ++this.portfolio._overseasNewsGeneration;
        const tabState = this.portfolio.overseasNews[tab];
        tabState.loading = true;
        tabState.error = null;
        tabState.list = [];
        if (tab === 'comprehensive') {
            tabState.hasMore = false;
            tabState.lastDt = '';
            tabState.lastTm = '';
        }

        try {
            const { stockCode, exchangeCode, country } = item.stockDetail;
            let result;

            if (tab === 'breaking') {
                result = await API.getOverseasBreakingNews(stockCode, exchangeCode);
                if (gen !== this.portfolio._overseasNewsGeneration) return;
                tabState.list = result || [];
            } else {
                result = await API.getOverseasComprehensiveNews(stockCode, exchangeCode, country);
                if (gen !== this.portfolio._overseasNewsGeneration) return;
                tabState.list = result.items || [];
                tabState.hasMore = result.hasMore || false;
                tabState.lastDt = result.lastDataDt || '';
                tabState.lastTm = result.lastDataTm || '';
            }
        } catch (e) {
            if (gen !== this.portfolio._overseasNewsGeneration) return;
            tabState.error = '뉴스를 불러올 수 없습니다';
            console.error('해외뉴스 로드 실패:', e);
        } finally {
            if (gen === this.portfolio._overseasNewsGeneration) {
                tabState.loading = false;
            }
        }
    },

    async loadMoreOverseasNews() {
        const news = this.portfolio.overseasNews;
        const tabState = news.comprehensive;
        const item = this._overseasNewsItem;
        if (!item || !item.stockDetail || !tabState.hasMore) return;

        const gen = ++this.portfolio._overseasNewsGeneration;
        tabState.loading = true;

        try {
            const { stockCode, exchangeCode, country } = item.stockDetail;
            const result = await API.getOverseasComprehensiveNews(
                stockCode, exchangeCode, country, tabState.lastDt, tabState.lastTm);
            if (gen !== this.portfolio._overseasNewsGeneration) return;
            tabState.list = [...tabState.list, ...(result.items || [])];
            tabState.hasMore = result.hasMore || false;
            tabState.lastDt = result.lastDataDt || '';
            tabState.lastTm = result.lastDataTm || '';
        } catch (e) {
            if (gen !== this.portfolio._overseasNewsGeneration) return;
            tabState.error = '추가 뉴스를 불러올 수 없습니다';
            console.error('해외뉴스 추가 로드 실패:', e);
        } finally {
            if (gen === this.portfolio._overseasNewsGeneration) {
                tabState.loading = false;
            }
        }
    },

    async openPurchaseModal(item) {
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
            memo: history.memo || ''
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
                memo: form.memo || null
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
                default:
                    await API.updateGeneralItem(userId, item.id, {
                        itemName: form.itemName, investedAmount: Number(form.investedAmount),
                        memo: form.memo || null
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
        return item && (item.assetType === 'CASH' || item.assetType === 'FUND');
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