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
            loading: false
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
            expandedSections: {},
            showPurchaseModal: false,
            purchaseItem: null,
            purchaseForm: { quantity: '', purchasePrice: '' },
            selectedNewsItemId: null,
            news: { list: [], page: 0, size: 20, totalPages: 0, totalElements: 0, loading: false },
            collectingItemId: null
        },

        assetTypeConfig: {
            STOCK:       { label: '주식',     color: 'blue',   barColor: 'bg-blue-500' },
            BOND:        { label: '채권',     color: 'green',  barColor: 'bg-green-500' },
            REAL_ESTATE: { label: '부동산',   color: 'yellow', barColor: 'bg-yellow-500' },
            FUND:        { label: '펀드',     color: 'purple', barColor: 'bg-purple-500' },
            CRYPTO:      { label: '암호화폐', color: 'orange', barColor: 'bg-orange-500' },
            GOLD:        { label: '금',       color: 'amber',  barColor: 'bg-amber-500' },
            COMMODITY:   { label: '원자재',   color: 'red',    barColor: 'bg-red-500' },
            CASH:        { label: '현금',     color: 'gray',   barColor: 'bg-gray-500' },
            OTHER:       { label: '기타',     color: 'slate',  barColor: 'bg-slate-500' }
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
                    this.homeSummary.portfolioTotalAmount = portfolioItems.reduce(function(sum, item) { return sum + item.investedAmount; }, 0);
                    this.homeSummary.portfolioNewsEnabledCount = portfolioItems.filter(function(item) { return item.newsEnabled; }).length;
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
                    await API.deactivateKeyword(kw.id);
                } else {
                    await API.activateKeyword(kw.id);
                }
                await this.loadKeywords();
            } catch (e) {
                console.error('키워드 상태 변경 실패:', e);
            }
        },

        async removeKeyword(id) {
            if (!confirm('키워드를 삭제하시겠습니까?')) return;
            try {
                await API.deleteKeyword(id);
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
                var result = await API.collectNewsByKeyword(kw.id, kw.keyword, this.auth.userId, kw.region);
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
            this.ecos.loading = true;
            try {
                this.ecos.indicators = await API.getEcosIndicators(this.ecos.selectedCategory) || [];
            } catch (e) {
                console.error('ECOS 지표 로드 실패:', e);
                this.ecos.indicators = [];
            } finally {
                this.ecos.loading = false;
            }
        },

        async selectEcosCategory(categoryName) {
            this.ecos.selectedCategory = categoryName;
            await this.loadEcosIndicators();
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
            }
        },

        getItemsByType(type) {
            return this.portfolio.items.filter(function(item) { return item.assetType === type; });
        },

        getTotalInvested() {
            return this.portfolio.items.reduce(function(sum, item) { return sum + item.investedAmount; }, 0);
        },

        getNewsEnabledCount() {
            return this.portfolio.items.filter(function(item) { return item.newsEnabled; }).length;
        },

        toggleSection(assetType) {
            this.portfolio.expandedSections[assetType] = !this.portfolio.expandedSections[assetType];
        },

        getItemSummary(item) {
            switch (item.assetType) {
                case 'STOCK':
                    if (!item.stockDetail) return '';
                    var parts = [item.stockDetail.market + ':' + item.stockDetail.stockCode];
                    if (item.stockDetail.subType === 'ETF') parts.push('ETF');
                    if (item.stockDetail.quantity) parts.push(item.stockDetail.quantity + '주');
                    if (item.stockDetail.avgBuyPrice) parts.push('평균 ' + Format.number(item.stockDetail.avgBuyPrice) + '원');
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
            if (!confirm('자산 항목을 삭제하시겠습니까?\n관련 뉴스도 함께 삭제됩니다.')) return;
            try {
                await API.deletePortfolioItem(this.auth.userId, itemId);
                await this.loadPortfolio();
            } catch (e) {
                console.error('항목 삭제 실패:', e);
                alert('삭제에 실패했습니다.');
            }
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
        },

        clearSelectedStock() {
            this.portfolio.stockSearch.selected = null;
            this.portfolio.addForm.itemName = '';
            this.portfolio.addForm.ticker = '';
            this.portfolio.addForm.exchange = '';
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
                            country: form.region === 'DOMESTIC' ? 'KR' : null,
                            quantity: Number(form.quantity),
                            purchasePrice: Number(form.purchasePrice),
                            dividendYield: form.dividendYield ? Number(form.dividendYield) : null
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

        async selectPortfolioNewsItem(item) {
            if (this.portfolio.selectedNewsItemId === item.id) {
                this.portfolio.selectedNewsItemId = null;
                this.portfolio.news = { list: [], page: 0, size: 20, totalPages: 0, totalElements: 0, loading: false };
                return;
            }
            this.portfolio.selectedNewsItemId = item.id;
            this.portfolio.news.page = 0;
            await this.loadPortfolioNews(0);
        },

        async loadPortfolioNews(page) {
            if (!this.portfolio.selectedNewsItemId) return;
            this.portfolio.news.loading = true;
            try {
                var result = await API.getNewsByPortfolioItem(this.portfolio.selectedNewsItemId, page, this.portfolio.news.size);
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
                var result = await API.collectNewsByPortfolioItem(item.id, item.itemName, this.auth.userId, item.region);
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

        openPurchaseModal(item) {
            this.portfolio.purchaseItem = item;
            this.portfolio.purchaseForm = { quantity: '', purchasePrice: '' };
            this.portfolio.showPurchaseModal = true;
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
                await API.addStockPurchase(this.auth.userId, item.id, {
                    quantity: Number(form.quantity),
                    purchasePrice: Number(form.purchasePrice)
                });
                this.portfolio.showPurchaseModal = false;
                this.portfolio.purchaseItem = null;
                await this.loadPortfolio();
            } catch (e) {
                console.error('추가 매수 실패:', e);
                alert('추가 매수에 실패했습니다.');
            }
        },

        // ==================== 수정 모달 ====================

        openEditModal(item) {
            this.portfolio.editingItem = item;
            this.portfolio.stockSearch = { query: '', results: [], loading: false, selected: null, debounceTimer: null };

            var form = {
                itemName: item.itemName,
                investedAmount: item.investedAmount,
                memo: item.memo || ''
            };

            switch (item.assetType) {
                case 'STOCK':
                    if (item.stockDetail) {
                        form.subType = item.stockDetail.subType;
                        form.ticker = item.stockDetail.stockCode;
                        form.exchange = item.stockDetail.market;
                        form.quantity = item.stockDetail.quantity;
                        form.purchasePrice = item.stockDetail.avgBuyPrice;
                        form.dividendYield = item.stockDetail.dividendYield;
                    }
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
                        await API.updateStockItem(userId, item.id, {
                            itemName: form.itemName,
                            memo: form.memo || null,
                            subType: form.subType || 'INDIVIDUAL',
                            stockCode: form.ticker,
                            market: form.exchange,
                            country: form.region === 'DOMESTIC' ? 'KR' : null,
                            quantity: Number(form.quantity),
                            purchasePrice: Number(form.purchasePrice),
                            dividendYield: form.dividendYield ? Number(form.dividendYield) : null
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
        }
    };
}
