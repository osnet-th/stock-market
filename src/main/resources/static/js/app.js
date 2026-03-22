function dashboard() {
    return {
        // ==================== Routing ====================
        currentPage: 'home',

        menus: [
            { key: 'home', label: '대시보드', icon: 'home' },
            { key: 'keywords', label: '키워드', icon: 'tag' },
            { key: 'ecos', label: '국내 경제지표', icon: 'chart' },
            { key: 'global', label: '글로벌 경제지표', icon: 'globe' },
            { key: 'portfolio', label: '포트폴리오', icon: 'portfolio' }
        ],

        sidebarCollapsed: localStorage.getItem('sidebarCollapsed') === 'true',

        toggleSidebar() {
            this.sidebarCollapsed = !this.sidebarCollapsed;
            localStorage.setItem('sidebarCollapsed', this.sidebarCollapsed);
        },

        // ==================== Auth ====================
        auth: {
            token: localStorage.getItem('accessToken'),
            userId: localStorage.getItem('userId'),
            role: localStorage.getItem('role'),
            displayName: localStorage.getItem('displayName')
        },

        checkLoggedIn() {
            return !!this.auth.token && !!this.auth.userId;
        },

        // ==================== Keywords State ====================
        keywords: {
            list: [],
            filter: 'all',
            regionFilter: 'all',
            showAddModal: false,
            newKeyword: { keyword: '', region: 'DOMESTIC' }
        },

        // ==================== ECOS State ====================
        ecos: {
            categories: [],
            selectedCategory: null,
            indicators: [],
            loading: false,
            _requestGeneration: 0
        },

        // ==================== News State ====================
        news: {
            selectedKeywordId: null,
            selectedKeywordText: null,
            collectingKeywordId: null,
            list: [],
            page: 0,
            size: 20,
            totalPages: 0,
            totalElements: 0,
            loading: false
        },

        // ==================== Global State ====================
        globalData: {
            categories: [],
            selectedCategory: null,
            selectedIndicator: null,
            indicatorData: null,
            loading: false
        },

        // ==================== Home Summary ====================
        homeSummary: {
            keywordCount: 0,
            activeKeywordCount: 0,
            domesticKeywordCount: 0,
            internationalKeywordCount: 0,
            ecosCategories: [],
            globalCategories: []
        },

        // ==================== Portfolio State ====================
        portfolio: {
            items: [],
            allocation: [],
            loading: false,
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
            selectedNewsItemId: null,
            news: { list: [], page: 0, size: 20, totalPages: 0, totalElements: 0, loading: false },
            collectingItemId: null,
            chartInstance: null,
            // 재무정보
            financialChartInstance: null,
            financialOptions: null,
            financialResult: null,
            financialLoading: false,
            selectedStockItem: null,
            financialYear: String(new Date().getFullYear()),
            financialReportCode: 'ANNUAL',
            selectedFinancialMenu: null,
            financialIndexClass: 'PROFITABILITY',
            financialFsDiv: 'CFS',
            financialAccountFsFilter: '',
            financialStatementFilter: '',
            financialMenus: [
                { key: 'accounts', label: '재무계정' },
                { key: 'indices', label: '재무지표' },
                { key: 'full-statements', label: '전체재무제표' },
                { key: 'stock-quantities', label: '주식수량' },
                { key: 'dividends', label: '배당정보' },
                { key: 'lawsuits', label: '소송현황' },
                { key: 'private-fund', label: '사모자금사용' },
                { key: 'public-fund', label: '공모자금사용' }
            ]
        },

        financialColumns: {
            accounts: [
                { key: 'accountName', label: '계정명', type: 'text' },
                { key: 'fsName', label: '재무제표', type: 'text' },
                { key: 'currentTermAmount', label: '당기', type: 'amount' },
                { key: 'previousTermAmount', label: '전기', type: 'amount' },
                { key: 'beforePreviousTermAmount', label: '전전기', type: 'amount' }
            ],
            indices: [
                { key: 'indexName', label: '지표명', type: 'text' },
                { key: 'indexValue', label: '값', type: 'number' }
            ],
            'full-statements': [
                { key: 'statementName', label: '제표종류', type: 'text' },
                { key: 'accountName', label: '계정명', type: 'text' },
                { key: 'accountDetail', label: '상세', type: 'text' },
                { key: 'currentTermAmount', label: '당기', type: 'amount' },
                { key: 'previousTermAmount', label: '전기', type: 'amount' }
            ],
            'stock-quantities': [
                { key: 'category', label: '구분', type: 'text' },
                { key: 'issuedTotalQuantity', label: '발행주식총수', type: 'amount' },
                { key: 'treasuryStockCount', label: '자기주식수', type: 'amount' },
                { key: 'distributedStockCount', label: '유통주식수', type: 'amount' }
            ],
            dividends: [
                { key: 'category', label: '구분', type: 'text' },
                { key: 'stockKind', label: '주식종류', type: 'text' },
                { key: 'currentTerm', label: '당기', type: 'number' },
                { key: 'previousTerm', label: '전기', type: 'number' },
                { key: 'beforePreviousTerm', label: '전전기', type: 'number' }
            ],
            lawsuits: [
                { key: 'plaintiffName', label: '원고', type: 'text' },
                { key: 'lawsuitAmount', label: '소송금액', type: 'amount' },
                { key: 'claimContent', label: '청구내용', type: 'text' },
                { key: 'currentProgress', label: '현재진행', type: 'text' },
                { key: 'litigationDate', label: '소송제기일', type: 'text' }
            ],
            'private-fund': [
                { key: 'category', label: '구분', type: 'text' },
                { key: 'usePurpose', label: '사용목적', type: 'text' },
                { key: 'planAmount', label: '계획금액', type: 'amount' },
                { key: 'actualAmount', label: '실제금액', type: 'amount' },
                { key: 'differenceReason', label: '차이사유', type: 'text' }
            ],
            'public-fund': [
                { key: 'category', label: '구분', type: 'text' },
                { key: 'usePurpose', label: '사용목적', type: 'text' },
                { key: 'planAmount', label: '계획금액', type: 'amount' },
                { key: 'actualAmount', label: '실제금액', type: 'amount' },
                { key: 'differenceReason', label: '차이사유', type: 'text' }
            ]
        },

        financialSummaryConfig: {
            accounts: [
                { match: '매출액', label: '매출액' },
                { match: '영업이익', label: '영업이익' },
                { match: '당기순이익', label: '당기순이익' },
                { match: '자산총계', label: '자산총계' },
                { match: '부채총계', label: '부채총계' },
                { match: '자본총계', label: '자본총계' }
            ],
            dividends: [
                { match: '주당 현금배당금', label: '주당배당금' },
                { match: '현금배당수익률', label: '배당수익률' },
                { match: '현금배당성향', label: '배당성향' }
            ]
        },

        assetTypeConfig: {
            STOCK:       { label: '주식',     color: 'blue',   barColor: 'bg-blue-500',   chartColor: '#3B82F6' },
            BOND:        { label: '채권',     color: 'green',  barColor: 'bg-green-500',  chartColor: '#22C55E' },
            REAL_ESTATE: { label: '부동산',   color: 'yellow', barColor: 'bg-yellow-500', chartColor: '#EAB308' },
            FUND:        { label: '펀드',     color: 'purple', barColor: 'bg-purple-500', chartColor: '#A855F7' },
            CRYPTO:      { label: '암호화폐', color: 'orange', barColor: 'bg-orange-500', chartColor: '#F97316' },
            GOLD:        { label: '금',       color: 'amber',  barColor: 'bg-amber-500',  chartColor: '#F59E0B' },
            COMMODITY:   { label: '원자재',   color: 'red',    barColor: 'bg-red-500',    chartColor: '#EF4444' },
            CASH:        { label: '현금',     color: 'gray',   barColor: 'bg-gray-500',   chartColor: '#6B7280' },
            OTHER:       { label: '기타',     color: 'slate',  barColor: 'bg-slate-500',  chartColor: '#64748B' }
        },

        // ==================== Init ====================
        async init() {
            this.handleOAuthCallback();

            if (!this.checkLoggedIn()) {
                window.location.href = '/login.html';
                return;
            }

            await this.loadMyProfile();

            // SIGNING_USER면 회원가입 페이지로 리다이렉트
            if (this.auth.role === 'SIGNING_USER') {
                window.location.href = '/signup.html';
                return;
            }

            await this.loadHomeSummary();
        },

        handleOAuthCallback() {
            const params = new URLSearchParams(window.location.search);
            const token = params.get('token');
            const userId = params.get('userId');
            const role = params.get('role');

            if (token && userId) {
                localStorage.setItem('accessToken', token);
                localStorage.setItem('userId', userId);
                if (role) localStorage.setItem('role', role);
                this.auth.token = token;
                this.auth.userId = userId;
                this.auth.role = role;
                window.history.replaceState({}, '', '/');
            }
        },

        async loadMyProfile() {
            try {
                const profile = await API.getMyProfile();
                this.auth.displayName = profile.displayName;
                this.auth.role = profile.role;
                localStorage.setItem('displayName', profile.displayName);
                localStorage.setItem('role', profile.role);
            } catch (e) {
                console.error('프로필 로드 실패:', e);
            }
        },

        logout() {
            localStorage.removeItem('accessToken');
            localStorage.removeItem('userId');
            localStorage.removeItem('role');
            localStorage.removeItem('displayName');
            this.auth.token = null;
            this.auth.userId = null;
            this.auth.role = null;
            this.auth.displayName = null;
            window.location.href = '/login.html';
        },

        async navigateTo(page) {
            this.currentPage = page;
            switch (page) {
                case 'home':
                    await this.loadHomeSummary();
                    break;
                case 'keywords':
                    if (this.checkLoggedIn()) {
                        await this.loadKeywords();
                        this.news.selectedKeywordId = null;
                        this.news.selectedKeywordText = null;
                        this.news.list = [];
                    }
                    break;
                case 'ecos':
                    if (this.ecos.categories.length === 0) await this.loadEcosCategories();
                    break;
                case 'global':
                    if (this.globalData.categories.length === 0) await this.loadGlobalCategories();
                    break;
                case 'portfolio':
                    await this.loadPortfolio();
                    break;
            }
        },

        async loadHomeSummary() {
            try {
                if (this.checkLoggedIn()) {
                    const allKeywords = await API.getKeywords(this.auth.userId) || [];
                    this.homeSummary.keywordCount = allKeywords.length;
                    this.homeSummary.activeKeywordCount = allKeywords.filter(k => k.active).length;
                    this.homeSummary.domesticKeywordCount = allKeywords.filter(k => k.region === 'DOMESTIC').length;
                    this.homeSummary.internationalKeywordCount = allKeywords.filter(k => k.region === 'INTERNATIONAL').length;
                }
                try {
                    this.homeSummary.ecosCategories = await API.getEcosCategories() || [];
                } catch (e) { /* skip */ }
                try {
                    this.homeSummary.globalCategories = await API.getGlobalCategories() || [];
                } catch (e) { /* skip */ }
                try {
                    var portfolioItems = await API.getPortfolioItems(this.auth.userId) || [];
                    this.homeSummary.portfolioItemCount = portfolioItems.length;
                    this.homeSummary.portfolioNewsEnabledCount = portfolioItems.filter(function(item) { return item.newsEnabled; }).length;
                    var typeCounts = {};
                    var assetTypeConfig = this.assetTypeConfig;
                    portfolioItems.forEach(function(item) {
                        var label = assetTypeConfig[item.assetType]?.label || item.assetType;
                        typeCounts[label] = (typeCounts[label] || 0) + 1;
                    });
                    this.homeSummary.portfolioTypeSummary = Object.keys(typeCounts)
                        .map(function(label) { return label + ' ' + typeCounts[label] + '건'; })
                        .join(' · ');
                } catch (e) { /* skip */ }
            } catch (e) {
                console.error('홈 요약 로드 실패:', e);
            }
        },

        // ==================== Keyword Methods ====================
        async loadKeywords() {
            try {
                const active = this.keywords.filter === 'active' ? true
                    : this.keywords.filter === 'inactive' ? false : null;
                this.keywords.list = await API.getKeywords(this.auth.userId, active) || [];
            } catch (e) {
                console.error('키워드 로드 실패:', e);
                this.keywords.list = [];
            }
        },

        getFilteredKeywords() {
            var list = this.keywords.list;
            if (this.keywords.regionFilter !== 'all') {
                list = list.filter(function(k) { return k.region === this.keywords.regionFilter; }.bind(this));
            }
            return list;
        },

        async addKeyword() {
            var kw = this.keywords.newKeyword;
            if (!kw.keyword.trim()) return;
            try {
                await API.registerKeyword(kw.keyword.trim(), this.auth.userId, kw.region);
                this.keywords.showAddModal = false;
                this.keywords.newKeyword = { keyword: '', region: 'DOMESTIC' };
                await this.loadKeywords();
            } catch (e) {
                console.error('키워드 등록 실패:', e);
                alert('키워드 등록에 실패했습니다.');
            }
        },

        async toggleKeyword(kw) {
            try {
                if (kw.active) {
                    await API.deactivateKeyword(kw.id, this.auth.userId);
                } else {
                    await API.activateKeyword(kw.id, this.auth.userId);
                }
                await this.loadKeywords();
            } catch (e) {
                console.error('키워드 상태 변경 실패:', e);
            }
        },

        async removeKeyword(id) {
            if (!confirm('키워드를 삭제하시겠습니까?')) return;
            try {
                await API.deleteKeyword(id, this.auth.userId);
                await this.loadKeywords();
            } catch (e) {
                console.error('키워드 삭제 실패:', e);
            }
        },

        // ==================== News Methods ====================
        async collectNews(kw) {
            if (this.news.collectingKeywordId) return;
            this.news.collectingKeywordId = kw.id;
            try {
                var result = await API.collectNewsByKeyword(kw.id, kw.keyword, kw.region);
                var msg = '수집 완료: ' + result.successCount + '건 저장';
                if (result.ignoredCount > 0) msg += ', ' + result.ignoredCount + '건 중복';
                alert(msg);

                if (this.news.selectedKeywordId === kw.id) {
                    await this.loadNews(0);
                }
            } catch (e) {
                console.error('뉴스 수집 실패:', e);
                alert('뉴스 수집에 실패했습니다.');
            } finally {
                this.news.collectingKeywordId = null;
            }
        },

        async selectNewsKeyword(kw) {
            if (this.news.selectedKeywordId === kw.id) {
                this.news.selectedKeywordId = null;
                this.news.selectedKeywordText = null;
                this.news.list = [];
                return;
            }
            this.news.selectedKeywordId = kw.id;
            this.news.selectedKeywordText = kw.keyword;
            this.news.page = 0;
            await this.loadNews(0);
        },

        async loadNews(page) {
            if (!this.news.selectedKeywordId) return;
            this.news.loading = true;
            try {
                var result = await API.getNewsByKeyword(this.news.selectedKeywordId, page, this.news.size);
                this.news.list = result.content || [];
                this.news.page = result.page;
                this.news.totalPages = result.totalPages;
                this.news.totalElements = result.totalElements;
            } catch (e) {
                console.error('뉴스 로드 실패:', e);
                this.news.list = [];
            } finally {
                this.news.loading = false;
            }
        },

        // ==================== ECOS Methods ====================
        async loadEcosCategories() {
            try {
                this.ecos.categories = await API.getEcosCategories() || [];
                if (this.ecos.categories.length > 0 && !this.ecos.selectedCategory) {
                    this.ecos.selectedCategory = this.ecos.categories[0].name;
                    await this.loadEcosIndicators();
                }
            } catch (e) {
                console.error('ECOS 카테고리 로드 실패:', e);
                this.ecos.categories = [];
            }
        },

        async loadEcosIndicators() {
            if (!this.ecos.selectedCategory) return;

            const thisGeneration = ++this.ecos._requestGeneration;
            this.ecos.loading = true;

            try {
                const result = await API.getEcosIndicators(this.ecos.selectedCategory) || [];
                if (thisGeneration !== this.ecos._requestGeneration) return;
                this.ecos.indicators = result;
            } catch (e) {
                if (thisGeneration !== this.ecos._requestGeneration) return;
                console.error('ECOS 지표 로드 실패:', e);
                this.ecos.indicators = [];
            } finally {
                if (thisGeneration === this.ecos._requestGeneration) {
                    this.ecos.loading = false;
                }
            }
        },

        async selectEcosCategory(categoryName) {
            this.ecos.selectedCategory = categoryName;
            await this.loadEcosIndicators();
        },

        getEcosKeyIndicators() {
            return this.ecos.indicators.filter(ind => ind.keyIndicator);
        },

        getEcosSortedIndicators() {
            return [...this.ecos.indicators].sort((a, b) =>
                a.className.localeCompare(b.className)
            );
        },

        getEcosCardBorderClass(ind) {
            const change = Format.change(ind.dataValue, ind.previousDataValue);
            if (change.direction === 'none' || change.direction === 'same') {
                return 'border-gray-300';
            }
            const positive = ind.positiveDirection || 'NEUTRAL';
            if (positive === 'NEUTRAL') return 'border-gray-300';

            const isPositiveChange =
                (positive === 'UP' && change.direction === 'up') ||
                (positive === 'DOWN' && change.direction === 'down');

            return isPositiveChange ? 'border-green-500' : 'border-red-500';
        },

        getInterestRateSpreads() {
            const find = (name) => {
                const ind = this.ecos.indicators.find(i => i.keystatName === name);
                return ind ? parseFloat(ind.dataValue) : null;
            };

            const calc = (a, b) => {
                if (a === null || b === null || isNaN(a) || isNaN(b)) return null;
                return Math.round((a - b) * 1000) / 1000;
            };

            const bond5 = find('국고채수익률(5년)');
            const bond3 = find('국고채수익률(3년)');
            const cd91 = find('CD수익률(91일)');
            const corpBond = find('회사채수익률(3년,AA-)');
            const loanRate = find('예금은행 대출금리');
            const depositRate = find('예금은행 수신금리');
            const callRate = find('콜금리(익일물)');
            const baseRate = find('한국은행 기준금리');

            return [
                { name: '장단기 금리차', value: calc(bond5, cd91), desc: '시장 구조 판단', sub: '국고채5년 − CD91일',
                  description: '양수(+)면 정상적인 우상향 금리 곡선. 0에 가까워지거나 음수(−)면 장단기 금리 역전으로 경기침체 신호' },
                { name: '중기-단기 금리차', value: calc(bond3, cd91), desc: '금리 기대 방향', sub: '국고채3년 − CD91일',
                  description: '양수(+)가 클수록 시장이 향후 금리 인상을 예상. 줄어들면 금리 인하 기대가 반영된 것' },
                { name: '장기 금리 기울기', value: calc(bond5, bond3), desc: '장기 기대 (인플레/성장)', sub: '국고채5년 − 국고채3년',
                  description: '양수(+)면 장기 인플레이션이나 경제성장 기대가 있다는 의미. 축소되면 장기 성장 기대가 약해지는 것' },
                { name: '신용 스프레드', value: calc(corpBond, bond3), desc: '시장 리스크 수준', sub: '회사채AA− − 국고채3년',
                  description: '기업 채권과 국채의 금리 차이. 벌어지면 시장이 기업 부도 위험을 높게 보는 것, 좁으면 안정적' },
                { name: '예대금리차', value: calc(loanRate, depositRate), desc: '금융 부담 / 은행 구조', sub: '대출금리 − 예금금리',
                  description: '은행이 예금자에게 주는 이자와 대출자에게 받는 이자의 차이. 클수록 대출자 부담이 크고 은행 수익성이 높음' },
                { name: '단기 vs 기준금리', value: calc(callRate, baseRate), desc: '유동성 상태', sub: '콜금리 − 기준금리',
                  description: '콜금리가 기준금리보다 높으면 시중 자금이 부족한 상태, 낮으면 유동성이 풍부한 상태' },
            ];
        },

        // ==================== Global Methods ====================
        async loadGlobalCategories() {
            try {
                this.globalData.categories = await API.getGlobalCategories() || [];
                if (this.globalData.categories.length > 0 && !this.globalData.selectedCategory) {
                    this.globalData.selectedCategory = this.globalData.categories[0].key;
                    var firstIndicators = this.globalData.categories[0].indicators;
                    if (firstIndicators && firstIndicators.length > 0) {
                        await this.selectGlobalIndicator(firstIndicators[0].key);
                    }
                }
            } catch (e) {
                console.error('글로벌 카테고리 로드 실패:', e);
                this.globalData.categories = [];
            }
        },

        async selectGlobalCategory(categoryKey) {
            this.globalData.selectedCategory = categoryKey;
            this.globalData.selectedIndicator = null;
            this.globalData.indicatorData = null;
            var cat = this.globalData.categories.find(function(c) { return c.key === categoryKey; });
            if (cat && cat.indicators && cat.indicators.length > 0) {
                await this.selectGlobalIndicator(cat.indicators[0].key);
            }
        },

        async selectGlobalIndicator(indicatorKey) {
            this.globalData.selectedIndicator = indicatorKey;
            this.globalData.loading = true;
            try {
                this.globalData.indicatorData = await API.getGlobalIndicator(indicatorKey);
            } catch (e) {
                console.error('글로벌 지표 로드 실패:', e);
                this.globalData.indicatorData = null;
            } finally {
                this.globalData.loading = false;
            }
        },

        getCurrentCategoryIndicators() {
            if (!this.globalData.selectedCategory) return [];
            var cat = this.globalData.categories.find(function(c) { return c.key === this.globalData.selectedCategory; }.bind(this));
            return cat ? cat.indicators || [] : [];
        },

        getChangeInfo(current, previous) {
            return Format.change(current, previous);
        },

        // ==================== Portfolio Methods ====================

        getAssetTypeLabel(type) {
            return this.assetTypeConfig[type]?.label || type;
        },

        getAssetTypeBadgeClass(type) {
            var color = this.assetTypeConfig[type]?.color || 'gray';
            return 'bg-' + color + '-100 text-' + color + '-700';
        },

        getAssetTypeBarClass(type) {
            return this.assetTypeConfig[type]?.barColor || 'bg-gray-500';
        },

        async loadPortfolio() {
            this.portfolio.loading = true;
            try {
                var results = await Promise.all([
                    API.getPortfolioItems(this.auth.userId),
                    API.getPortfolioAllocation(this.auth.userId)
                ]);
                this.portfolio.items = results[0] || [];
                this.portfolio.allocation = results[1] || [];

                await this.loadStockPrices();

                var sections = {};
                this.portfolio.items.forEach(function(item) {
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
                var self = this;
                this.$nextTick(function() {
                    self.renderDonutChart();
                });
            }
        },

        async loadStockPrices() {
            var stockItems = this.portfolio.items.filter(function(item) {
                return item.assetType === 'STOCK' && item.stockDetail;
            });
            if (stockItems.length === 0) {
                this.portfolio.stockPrices = {};
                return;
            }

            var stocks = stockItems.map(function(item) {
                return {
                    stockCode: item.stockDetail.stockCode,
                    marketType: item.stockDetail.market,
                    exchangeCode: item.stockDetail.exchangeCode
                };
            });

            try {
                var result = await API.getStockPrices(stocks);
                this.portfolio.stockPrices = result.prices || {};
            } catch (e) {
                console.error('현재가 조회 실패:', e);
                this.portfolio.stockPrices = {};
            }
        },

        getEvalAmount(item) {
            if (item.assetType === 'STOCK' && item.stockDetail) {
                var priceData = this.portfolio.stockPrices[item.stockDetail.stockCode];
                if (priceData && priceData.currentPriceKrw) {
                    return parseFloat(priceData.currentPriceKrw) * item.stockDetail.quantity;
                }
            }
            return item.investedAmount;
        },

        getExchangeRate(item) {
            if (item.assetType !== 'STOCK' || !item.stockDetail) return 1;
            // priceCurrency가 KRW이면 이미 원화이므로 환율 적용 불필요
            var priceCurrency = item.stockDetail.priceCurrency;
            if (!priceCurrency || priceCurrency === 'KRW') return 1;
            var priceData = this.portfolio.stockPrices[item.stockDetail.stockCode];
            if (!priceData || !priceData.exchangeRateValue) return 1;
            return parseFloat(priceData.exchangeRateValue);
        },

        getInvestedAmountKrw(item) {
            // 해외 주식: 사용자가 입력한 원화 투자금액 우선 사용
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
            var priceData = this.portfolio.stockPrices[item.stockDetail.stockCode];
            if (!priceData || !priceData.currentPrice) return null;
            var currentPrice = parseFloat(priceData.currentPrice);
            var avgPrice = parseFloat(item.stockDetail.avgBuyPrice);
            if (avgPrice === 0) return null;
            return ((currentPrice - avgPrice) / avgPrice * 100);
        },

        getTotalEvalAmount() {
            return this.portfolio.items.reduce(function(sum, item) {
                return sum + this.getEvalAmount(item);
            }.bind(this), 0);
        },

        getEvalAllocation() {
            var totalEval = this.getTotalEvalAmount();
            if (totalEval === 0) return this.portfolio.allocation;

            var typeMap = {};
            this.portfolio.items.forEach(function(item) {
                if (!typeMap[item.assetType]) {
                    typeMap[item.assetType] = { assetType: item.assetType, assetTypeName: this.getAssetTypeLabel(item.assetType), totalAmount: 0 };
                }
                typeMap[item.assetType].totalAmount += this.getEvalAmount(item);
            }.bind(this));

            var assetTypeOrder = ['STOCK', 'BOND', 'REAL_ESTATE', 'FUND', 'OTHER', 'CRYPTO', 'GOLD', 'COMMODITY', 'CASH'];
            return Object.values(typeMap).map(function(alloc) {
                alloc.percentage = Math.round(alloc.totalAmount / totalEval * 1000) / 10;
                return alloc;
            }).sort(function(a, b) {
                return assetTypeOrder.indexOf(a.assetType) - assetTypeOrder.indexOf(b.assetType);
            });
        },

        getItemsByType(type) {
            return this.portfolio.items.filter(function(item) { return item.assetType === type; });
        },

        getDomesticStocks() {
            return this.portfolio.items.filter(function(item) {
                return item.assetType === 'STOCK' && item.stockDetail?.country === 'KR';
            }).sort(function(a, b) {
                var aIsEtf = a.stockDetail?.subType === 'ETF' ? 1 : 0;
                var bIsEtf = b.stockDetail?.subType === 'ETF' ? 1 : 0;
                return aIsEtf - bIsEtf;
            });
        },

        getOverseasStocks() {
            return this.portfolio.items.filter(function(item) {
                return item.assetType === 'STOCK' && item.stockDetail?.country !== 'KR';
            });
        },

        getTotalInvested() {
            return this.portfolio.items.reduce(function(sum, item) {
                return sum + this.getInvestedAmountKrw(item);
            }.bind(this), 0);
        },

        getSubTotalInvested(assetType) {
            return this.portfolio.items
                .filter(function(item) { return item.assetType === assetType; })
                .reduce(function(sum, item) { return sum + this.getInvestedAmountKrw(item); }.bind(this), 0);
        },

        getSubTotalEvalAmount(assetType) {
            return this.portfolio.items
                .filter(function(item) { return item.assetType === assetType; })
                .reduce(function(sum, item) { return sum + this.getEvalAmount(item); }.bind(this), 0);
        },

        getSubTotalProfitRate(assetType) {
            var invested = this.getSubTotalInvested(assetType);
            if (invested === 0) return null;
            var evalAmount = this.getSubTotalEvalAmount(assetType);
            return ((evalAmount - invested) / invested * 100);
        },

        getNewsEnabledCount() {
            return this.portfolio.items.filter(function(item) { return item.newsEnabled; }).length;
        },

        toggleSection(assetType) {
            this.portfolio.expandedSections[assetType] = !this.portfolio.expandedSections[assetType];
        },

        renderDonutChart() {
            var canvas = document.getElementById('portfolioDonutChart');
            if (!canvas) return;

            if (this.portfolio.chartInstance) {
                this.portfolio.chartInstance.destroy();
                this.portfolio.chartInstance = null;
            }

            var allocation = this.getEvalAllocation();
            if (allocation.length === 0) return;

            var labels = [];
            var data = [];
            var colors = [];
            var self = this;

            allocation.forEach(function(alloc) {
                labels.push(alloc.assetTypeName);
                data.push(alloc.totalAmount);
                var config = self.assetTypeConfig[alloc.assetType];
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
                                generateLabels: function(chart) {
                                    var dataset = chart.data.datasets[0];
                                    var total = dataset.data.reduce(function(sum, val) { return sum + val; }, 0);
                                    return chart.data.labels.map(function(label, i) {
                                        var pct = total > 0 ? Math.round(dataset.data[i] / total * 1000) / 10 : 0;
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
                                label: function(context) {
                                    var value = context.parsed;
                                    var total = context.dataset.data.reduce(function(sum, val) { return sum + val; }, 0);
                                    var pct = total > 0 ? Math.round(value / total * 1000) / 10 : 0;
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
            var priceData = this.portfolio.stockPrices[item.stockDetail.stockCode];
            if (!priceData || !priceData.currentPrice) return '';
            var currency = priceData.currency || 'KRW';
            var priceDisplay = currency === 'KRW'
                ? '현재가 ' + Format.number(priceData.currentPrice) + '원'
                : '현재가 ' + Format.number(priceData.currentPrice, 2) + ' ' + currency;
            var parts = [priceDisplay];
            var rate = this.getProfitRate(item);
            if (rate !== null) {
                var sign = rate >= 0 ? '+' : '';
                parts.push(sign + rate.toFixed(2) + '%');
            }
            parts.push('총 ' + Format.number(this.getEvalAmount(item), 0) + '원');
            return parts.join(' · ');
        },

        getItemSummary(item) {
            switch (item.assetType) {
                case 'STOCK':
                    if (!item.stockDetail) return '';
                    var parts = [item.stockDetail.market + ':' + item.stockDetail.stockCode];
                    if (item.stockDetail.subType === 'ETF') parts.push('ETF');
                    if (item.stockDetail.quantity) parts.push(item.stockDetail.quantity + '주');
                    if (item.stockDetail.avgBuyPrice) {
                        var cur = item.stockDetail.priceCurrency;
                        var suffix = (!cur || cur === 'KRW') ? '원' : ' ' + cur;
                        var decimals = (!cur || cur === 'KRW') ? 0 : 2;
                        parts.push('평균 ' + Format.number(item.stockDetail.avgBuyPrice, decimals) + suffix);
                    }
                    return parts.join(' · ');
                case 'BOND':
                    if (!item.bondDetail) return '';
                    var bondParts = [];
                    if (item.bondDetail.subType === 'GOVERNMENT') bondParts.push('국채');
                    else if (item.bondDetail.subType === 'CORPORATE') bondParts.push('회사채');
                    if (item.bondDetail.maturityDate) bondParts.push('만기 ' + item.bondDetail.maturityDate);
                    if (item.bondDetail.couponRate) bondParts.push(item.bondDetail.couponRate + '%');
                    return bondParts.join(' · ');
                case 'REAL_ESTATE':
                    if (!item.realEstateDetail) return '';
                    var reParts = [];
                    var reSubTypes = { APARTMENT: '아파트', OFFICETEL: '오피스텔', LAND: '토지', COMMERCIAL: '상가' };
                    if (item.realEstateDetail.subType) reParts.push(reSubTypes[item.realEstateDetail.subType] || item.realEstateDetail.subType);
                    if (item.realEstateDetail.address) reParts.push(item.realEstateDetail.address);
                    return reParts.join(' · ');
                case 'FUND':
                    if (!item.fundDetail) return '';
                    var fundParts = [];
                    var fundSubTypes = { EQUITY_FUND: '주식형', BOND_FUND: '채권형', MIXED_FUND: '혼합형' };
                    if (item.fundDetail.subType) fundParts.push(fundSubTypes[item.fundDetail.subType] || item.fundDetail.subType);
                    if (item.fundDetail.managementFee) fundParts.push('보수 ' + item.fundDetail.managementFee + '%');
                    return fundParts.join(' · ');
                default:
                    return item.memo || '';
            }
        },

        async deleteItem(itemId) {
            var item = this.portfolio.items.find(function(i) { return i.id === itemId; });
            var hasLinkedCash = item && item.assetType === 'STOCK' && item.linkedCashItemId;

            if (hasLinkedCash) {
                // 연결된 CASH가 있는 주식: 복원 옵션 제공
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
            var dc = this.portfolio.deleteConfirm;
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

        // ==================== 추가 모달 ====================

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

            if (type === 'GENERAL') {
                this.portfolio.addForm.assetType = 'CRYPTO';
            }
        },

        async searchStock() {
            var query = this.portfolio.stockSearch.query.trim();
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
            return this.portfolio.items.filter(function(item) {
                return item.assetType === 'CASH';
            });
        },

        getCurrencyByExchangeCode(exchangeCode) {
            var mapping = {
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
            var mapping = {
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
            var type = this.portfolio.selectedAssetType;
            var form = this.portfolio.addForm;
            var userId = this.auth.userId;

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
                            itemName: form.itemName,
                            region: form.region,
                            memo: form.memo || null,
                            subType: form.subType || 'INDIVIDUAL',
                            stockCode: form.ticker,
                            market: form.exchange,
                            exchangeCode: form.exchangeCode,
                            country: this.getCountryByExchangeCode(form.exchangeCode),
                            quantity: Number(form.quantity),
                            purchasePrice: Number(form.purchasePrice),
                            dividendYield: form.dividendYield ? Number(form.dividendYield) : null,
                            priceCurrency: form.priceCurrency || 'KRW',
                            investedAmountKrw: form.investedAmountKrw ? Number(form.investedAmountKrw) : null,
                            cashItemId: form.cashItemId ? Number(form.cashItemId) : null
                        });
                        break;
                    case 'BOND':
                        await API.addBondItem(userId, {
                            itemName: form.itemName,
                            investedAmount: Number(form.investedAmount),
                            region: form.region,
                            memo: form.memo || null,
                            subType: form.subType || 'GOVERNMENT',
                            maturityDate: form.maturityDate || null,
                            couponRate: form.couponRate ? Number(form.couponRate) : null,
                            creditRating: form.creditRating || null
                        });
                        break;
                    case 'REAL_ESTATE':
                        await API.addRealEstateItem(userId, {
                            itemName: form.itemName,
                            investedAmount: Number(form.investedAmount),
                            region: form.region,
                            memo: form.memo || null,
                            subType: form.subType || 'APARTMENT',
                            address: form.address || null,
                            area: form.area ? Number(form.area) : null
                        });
                        break;
                    case 'FUND':
                        await API.addFundItem(userId, {
                            itemName: form.itemName,
                            investedAmount: Number(form.investedAmount),
                            region: form.region,
                            memo: form.memo || null,
                            subType: form.subType || 'EQUITY_FUND',
                            managementFee: form.managementFee ? Number(form.managementFee) : null
                        });
                        break;
                    case 'GENERAL':
                        await API.addGeneralItem(userId, {
                            assetType: form.assetType,
                            itemName: form.itemName,
                            investedAmount: Number(form.investedAmount),
                            region: form.region,
                            memo: form.memo || null
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

        // ==================== 포트폴리오 뉴스 ====================

        async findKeywordIdByItemName(itemName, region) {
            var keywords = await API.getKeywords(this.auth.userId) || [];
            var matched = keywords.find(function(k) { return k.keyword === itemName && k.region === region; });
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
                var result = await API.getNewsByKeyword(this.portfolio.selectedNewsKeywordId, page, this.portfolio.news.size);
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
                var keywordId = await this.findKeywordIdByItemName(item.itemName, item.region);
                if (!keywordId) {
                    alert('키워드를 찾을 수 없습니다. 뉴스를 먼저 활성화해주세요.');
                    return;
                }
                var result = await API.collectNewsByKeyword(keywordId, item.itemName, item.region);
                var msg = '수집 완료: ' + result.successCount + '건 저장';
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

        // ==================== 추가 매수 모달 ====================

        async openPurchaseModal(item) {
            this.portfolio.purchaseItem = item;
            this.portfolio.purchaseForm = { quantity: '', purchasePrice: '' };
            this.portfolio.purchaseHistories = [];
            this.portfolio.editingHistory = null;
            this.portfolio.showPurchaseModal = true;
            await this.loadPurchaseHistories(item.id);
        },

        async submitPurchase() {
            var form = this.portfolio.purchaseForm;
            var item = this.portfolio.purchaseItem;

            if (!form.quantity || Number(form.quantity) <= 0) {
                alert('매수 수량을 입력해주세요.');
                return;
            }
            if (!form.purchasePrice || Number(form.purchasePrice) <= 0) {
                alert('매수 단가를 입력해주세요.');
                return;
            }

            try {
                var response = await API.addStockPurchase(this.auth.userId, item.id, {
                    quantity: Number(form.quantity),
                    purchasePrice: Number(form.purchasePrice)
                });
                this.portfolio.purchaseForm = { quantity: '', purchasePrice: '' };
                this.portfolio.purchaseItem = { ...this.portfolio.purchaseItem, ...response };
                await this.loadPurchaseHistories(item.id);
                this.loadPortfolio();
            } catch (e) {
                console.error('추가 매수 실패:', e);
                alert('추가 매수에 실패했습니다.');
            }
        },

        // ==================== 매수이력 ====================

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
            var form = this.portfolio.editHistoryForm;
            var item = this.portfolio.purchaseItem;

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
            var item = this.portfolio.purchaseItem;

            try {
                await API.deletePurchaseHistory(this.auth.userId, item.id, historyId);
                await this.loadPurchaseHistories(item.id);
                await this.loadPortfolio();
            } catch (e) {
                console.error('매수이력 삭제 실패:', e);
                alert(e.message || '매수이력 삭제에 실패했습니다.');
            }
        },

        // ==================== 수정 모달 ====================

        openEditModal(item) {
            this.portfolio.editingItem = item;
            this.portfolio.stockSearch = { query: '', results: [], loading: false, selected: null, debounceTimer: null };

            var form = {
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
                    form.cashItemId = item.linkedCashItemId || '';
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
                    }
                    break;
            }

            this.portfolio.editForm = form;
            this.portfolio.showEditModal = true;
        },

        async searchStockForEdit() {
            var query = this.portfolio.stockSearch.query.trim();
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
            var item = this.portfolio.editingItem;
            var form = this.portfolio.editForm;
            var userId = this.auth.userId;

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
                        var stockDetail = item.stockDetail || {};
                        var newCashItemId = form.cashItemId ? Number(form.cashItemId) : null;
                        var oldCashItemId = item.linkedCashItemId || null;
                        var isNewLink = newCashItemId && !oldCashItemId;
                        var deductOnLink = false;

                        if (isNewLink) {
                            deductOnLink = confirm(
                                '이미 보유 중인 주식에 원화를 연결합니다.\n' +
                                '현재 투자금(' + Number(item.investedAmount).toLocaleString() + '원)을 원화에서 차감할까요?\n\n' +
                                '확인: 차감함 (신규 매수와 동일)\n' +
                                '취소: 연결만 (이후 추가 매수부터 차감)'
                            );
                        }

                        await API.updateStockItem(userId, item.id, {
                            itemName: form.itemName,
                            memo: form.memo || null,
                            subType: form.subType || 'INDIVIDUAL',
                            stockCode: stockDetail.stockCode,
                            market: stockDetail.market,
                            exchangeCode: stockDetail.exchangeCode,
                            country: stockDetail.country,
                            quantity: Number(form.quantity),
                            purchasePrice: Number(form.purchasePrice),
                            dividendYield: form.dividendYield ? Number(form.dividendYield) : null,
                            priceCurrency: form.priceCurrency || 'KRW',
                            investedAmountKrw: form.investedAmountKrw ? Number(form.investedAmountKrw) : null,
                            cashItemId: newCashItemId,
                            deductOnLink: deductOnLink
                        });
                        break;
                    case 'BOND':
                        await API.updateBondItem(userId, item.id, {
                            itemName: form.itemName,
                            investedAmount: Number(form.investedAmount),
                            memo: form.memo || null,
                            subType: form.subType || 'GOVERNMENT',
                            maturityDate: form.maturityDate || null,
                            couponRate: form.couponRate ? Number(form.couponRate) : null,
                            creditRating: form.creditRating || null
                        });
                        break;
                    case 'REAL_ESTATE':
                        await API.updateRealEstateItem(userId, item.id, {
                            itemName: form.itemName,
                            investedAmount: Number(form.investedAmount),
                            memo: form.memo || null,
                            subType: form.subType || 'APARTMENT',
                            address: form.address || null,
                            area: form.area ? Number(form.area) : null
                        });
                        break;
                    case 'FUND':
                        await API.updateFundItem(userId, item.id, {
                            itemName: form.itemName,
                            investedAmount: Number(form.investedAmount),
                            memo: form.memo || null,
                            subType: form.subType || 'EQUITY_FUND',
                            managementFee: form.managementFee ? Number(form.managementFee) : null
                        });
                        break;
                    default:
                        await API.updateGeneralItem(userId, item.id, {
                            itemName: form.itemName,
                            investedAmount: Number(form.investedAmount),
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

        // ==================== 재무 상세 ====================

        getYearOptions() {
            var currentYear = new Date().getFullYear();
            return Array.from({ length: 6 }, function(_, i) { return String(currentYear - i); });
        },

        getDefaultYear() {
            return String(new Date().getFullYear());
        },

        async loadFinancialOptions() {
            if (this.portfolio.financialOptions) return;
            try {
                this.portfolio.financialOptions = await API.getFinancialOptions();
            } catch (e) {
                console.error('재무 옵션 로드 실패:', e);
            }
        },

        async openStockDetail(item) {
            var stockCode = item.stockDetail?.stockCode;
            if (!stockCode || item.stockDetail?.country !== 'KR' || item.stockDetail?.subType === 'ETF') return;

            this.portfolio.selectedStockItem = item;
            this.portfolio.financialYear = this.getDefaultYear();
            this.portfolio.financialReportCode = 'ANNUAL';
            this.portfolio.selectedFinancialMenu = null;
            this.portfolio.financialResult = null;

            await this.loadFinancialOptions();
        },

        closeStockDetail() {
            if (this.portfolio.financialChartInstance) {
                this.portfolio.financialChartInstance.destroy();
                this.portfolio.financialChartInstance = null;
            }
            this.portfolio.selectedStockItem = null;
            this.portfolio.selectedFinancialMenu = null;
            this.portfolio.financialResult = null;
        },

        getFinancialColumns() {
            var menu = this.portfolio.selectedFinancialMenu;
            return this.financialColumns[menu] || null;
        },

        getFilteredFinancialResult() {
            var result = this.portfolio.financialResult;
            if (!result || result.length === 0) return [];
            var menu = this.portfolio.selectedFinancialMenu;

            if (menu === 'accounts' && this.portfolio.financialAccountFsFilter) {
                var fsFilter = this.portfolio.financialAccountFsFilter;
                return result.filter(function(row) { return row.fsName === fsFilter; });
            }
            if (menu === 'full-statements' && this.portfolio.financialStatementFilter) {
                var stFilter = this.portfolio.financialStatementFilter;
                return result.filter(function(row) { return row.statementName === stFilter; });
            }
            return result;
        },

        getFilterOptions(fieldName) {
            var result = this.portfolio.financialResult;
            if (!result || result.length === 0) return [];
            var seen = {};
            var options = [];
            for (var i = 0; i < result.length; i++) {
                var name = result[i][fieldName];
                if (name && !seen[name]) {
                    seen[name] = true;
                    options.push(name);
                }
            }
            return options;
        },

        getFinancialSummaryCards() {
            var menu = this.portfolio.selectedFinancialMenu;
            var result = this.getFilteredFinancialResult();
            if (!result || result.length === 0) return [];

            var config = this.financialSummaryConfig[menu];
            if (!config) return [];

            var cards = [];
            for (var i = 0; i < config.length; i++) {
                var cfg = config[i];
                for (var j = 0; j < result.length; j++) {
                    var row = result[j];
                    var name = row.accountName || row.category || '';
                    if (name.indexOf(cfg.match) !== -1) {
                        var current = row.currentTermAmount || row.currentTerm || '';
                        var previous = row.previousTermAmount || row.previousTerm || '';
                        var currentNum = parseFloat(String(current).replace(/,/g, '')) || 0;
                        var previousNum = parseFloat(String(previous).replace(/,/g, '')) || 0;
                        var changeRate = previousNum !== 0 ? ((currentNum - previousNum) / Math.abs(previousNum) * 100) : null;
                        cards.push({
                            label: cfg.label,
                            value: Format.compactNumber(current),
                            changeRate: changeRate
                        });
                        break;
                    }
                }
            }
            return cards;
        },

        formatFinancialCell(value, type) {
            if (value == null || value === '') return '-';
            if (type === 'amount') {
                return Format.compactNumber(value);
            }
            if (type === 'number') {
                var n = parseFloat(String(value).replace(/,/g, ''));
                return isNaN(n) ? value : Format.number(n);
            }
            return value;
        },

        isAmountColumn(type) {
            return type === 'amount' || type === 'number';
        },

        async selectFinancialMenu(menuKey) {
            this.portfolio.selectedFinancialMenu = menuKey;
            this.portfolio.financialResult = null;
            this.portfolio.financialAccountFsFilter = '';
            this.portfolio.financialStatementFilter = '';
            await this.loadSelectedFinancial();
        },

        async onFinancialFilterChange() {
            if (!this.portfolio.selectedFinancialMenu) return;
            await this.loadSelectedFinancial();
        },

        async loadSelectedFinancial() {
            var stockCode = this.portfolio.selectedStockItem?.stockDetail?.stockCode;
            var menu = this.portfolio.selectedFinancialMenu;
            if (!stockCode || !menu) return;

            var year = this.portfolio.financialYear;
            var reportCode = this.portfolio.financialReportCode;

            this.portfolio.financialLoading = true;
            try {
                var result;
                switch (menu) {
                    case 'accounts':
                        result = await API.getFinancialAccounts(stockCode, year, reportCode);
                        break;
                    case 'indices':
                        result = await API.getFinancialIndices(stockCode, year, reportCode, this.portfolio.financialIndexClass);
                        break;
                    case 'full-statements':
                        result = await API.getFullFinancialStatements(stockCode, year, reportCode, this.portfolio.financialFsDiv);
                        break;
                    case 'stock-quantities':
                        result = await API.getFinancialStockQuantities(stockCode, year, reportCode);
                        break;
                    case 'dividends':
                        result = await API.getFinancialDividends(stockCode, year, reportCode);
                        break;
                    case 'lawsuits':
                        result = await API.getLawsuits(stockCode, year + '0101', year + '1231');
                        break;
                    case 'private-fund':
                        result = await API.getPrivateFundUsages(stockCode, year, reportCode);
                        break;
                    case 'public-fund':
                        result = await API.getPublicFundUsages(stockCode, year, reportCode);
                        break;
                }
                this.portfolio.financialResult = result || [];
                if (menu === 'accounts' && this.portfolio.financialResult.length > 0) {
                    var self = this;
                    this.$nextTick(function() {
                        self.renderFinancialBarChart();
                    });
                }
            } catch (e) {
                console.error('재무정보 조회 실패:', e);
                this.portfolio.financialResult = [];
            } finally {
                this.portfolio.financialLoading = false;
            }
        },

        parseAmount(value) {
            if (!value) return 0;
            var num = parseFloat(String(value).replace(/,/g, ''));
            return isNaN(num) ? 0 : num;
        },

        renderFinancialBarChart() {
            var canvas = document.getElementById('financialBarChart');
            if (!canvas) return;

            if (this.portfolio.financialChartInstance) {
                this.portfolio.financialChartInstance.destroy();
                this.portfolio.financialChartInstance = null;
            }

            var result = this.getFilteredFinancialResult();
            var config = this.financialSummaryConfig.accounts;
            if (!result || result.length === 0 || !config) return;

            var labels = [];
            var currentData = [];
            var previousData = [];
            var beforePreviousData = [];

            for (var i = 0; i < config.length; i++) {
                var cfg = config[i];
                for (var j = 0; j < result.length; j++) {
                    var row = result[j];
                    var name = row.accountName || '';
                    if (name.indexOf(cfg.match) !== -1) {
                        labels.push(cfg.label);
                        currentData.push(this.parseAmount(row.currentTermAmount));
                        previousData.push(this.parseAmount(row.previousTermAmount));
                        beforePreviousData.push(this.parseAmount(row.beforePreviousTermAmount));
                        break;
                    }
                }
            }

            if (labels.length === 0) return;

            this.portfolio.financialChartInstance = new Chart(canvas, {
                type: 'bar',
                data: {
                    labels: labels,
                    datasets: [
                        {
                            label: '당기',
                            data: currentData,
                            backgroundColor: '#3B82F6',
                            borderRadius: 4
                        },
                        {
                            label: '전기',
                            data: previousData,
                            backgroundColor: '#93C5FD',
                            borderRadius: 4
                        },
                        {
                            label: '전전기',
                            data: beforePreviousData,
                            backgroundColor: '#DBEAFE',
                            borderRadius: 4
                        }
                    ]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    plugins: {
                        legend: {
                            position: 'top',
                            labels: {
                                usePointStyle: true,
                                pointStyle: 'rect',
                                font: { size: 11 }
                            }
                        },
                        tooltip: {
                            callbacks: {
                                label: function(context) {
                                    return context.dataset.label + ': ' + Format.compactNumber(context.parsed.y);
                                }
                            }
                        }
                    },
                    scales: {
                        y: {
                            beginAtZero: true,
                            ticks: {
                                callback: function(value) {
                                    return Format.compactNumber(value);
                                },
                                font: { size: 11 }
                            },
                            grid: { color: '#F3F4F6' }
                        },
                        x: {
                            ticks: { font: { size: 11 } },
                            grid: { display: false }
                        }
                    }
                }
            });
        }
    };
}
