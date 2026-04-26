/** Dashboard - 코어(라우팅, 초기화) + 컴포넌트 통합 */
function dashboard() {
    return {
        // ==================== 코어 상태 ====================
        currentPage: (() => {
            const hash = location.hash.replace('#', '');
            const validPages = ['home', 'keywords', 'news-search', 'ecos', 'global', 'portfolio', 'salary', 'stocknote', 'admin-logs'];
            return validPages.includes(hash) ? hash : 'home';
        })(),

        menus: [
            { key: 'home', label: '대시보드', icon: 'home' },
            { key: 'keywords', label: '키워드', icon: 'tag' },
            { key: 'news-search', label: '뉴스 검색', icon: 'search' },
            { key: 'ecos', label: '국내 경제지표', icon: 'chart' },
            { key: 'global', label: '글로벌 경제지표', icon: 'globe' },
            { key: 'portfolio', label: '포트폴리오', icon: 'portfolio' },
            { key: 'salary', label: '월급 사용 비율', icon: 'wallet' },
            { key: 'stocknote', label: '투자 노트', icon: 'note' },
            { key: 'admin-logs', label: '운영자 로그', icon: 'logs' }
        ],

        sidebarCollapsed: localStorage.getItem('sidebarCollapsed') === 'true',

        // ==================== 반응형 상태 ====================
        isMobile: false,
        mobileDrawerOpen: false,
        _mqlCleanup: null,

        // ==================== 컴포넌트 통합 ====================
        ...AuthComponent,
        ...HomeComponent,
        ...KeywordComponent,
        ...NewsComponent,
        ...NewsSearchComponent,
        ...EcosComponent,
        ...GlobalComponent,
        ...PortfolioComponent,
        ...FinancialComponent,
        ...ChatComponent,
        ...FavoriteComponent,
        ...SalaryComponent,
        ...StocknoteComponent,
        ...AdminLogsComponent,

        // ==================== 코어 메서드 ====================
        toggleSidebar() {
            this.sidebarCollapsed = !this.sidebarCollapsed;
            localStorage.setItem('sidebarCollapsed', this.sidebarCollapsed);
        },

        // ==================== 반응형 메서드 ====================
        toggleMobileDrawer() {
            this.mobileDrawerOpen = !this.mobileDrawerOpen;
            document.body.style.overflow = this.mobileDrawerOpen ? 'hidden' : '';
        },

        closeMobileDrawer() {
            if (this.mobileDrawerOpen) {
                this.mobileDrawerOpen = false;
                document.body.style.overflow = '';
            }
        },

        async init() {
            // 중복 초기화 방지
            if (this._mqlCleanup) return;

            // 반응형 breakpoint 감지 (matchMedia 전용 — resize 이벤트 사용 안 함)
            const mql = window.matchMedia('(max-width: 1023px)');
            const handleChange = (e) => {
                this.isMobile = e.matches;
                if (!e.matches) {
                    this.closeMobileDrawer();
                }
            };
            mql.addEventListener('change', handleChange);
            this._mqlCleanup = () => mql.removeEventListener('change', handleChange);
            this.isMobile = mql.matches;

            // 브라우저 뒤로가기/앞으로가기 대응
            window.addEventListener('popstate', () => {
                const hash = location.hash.replace('#', '');
                const validPages = this.menus.map(m => m.key);
                const page = validPages.includes(hash) ? hash : 'home';
                if (this.currentPage !== page) {
                    this.navigateTo(page);
                }
            });

            this.handleOAuthCallback();

            if (!this.checkLoggedIn()) {
                window.location.href = '/login.html';
                return;
            }

            await this.loadMyProfile();
            this.initFavorites();
            this.loadFavorites();

            if (this.auth.role === 'SIGNING_USER') {
                window.location.href = '/signup.html';
                return;
            }

            // hash 기반 초기 페이지 로드
            if (this.currentPage !== 'home') {
                await this.navigateTo(this.currentPage);
            } else {
                await this.loadHomeSummary();
            }
        },

        async navigateTo(page) {
            this.closeMobileDrawer();

            // 페이지 이동 시 채팅 닫기 + 스트리밍 중단
            if (this.chat.isOpen) {
                if (this.chat._abortController) {
                    this.chat._abortController.abort();
                }
                this.chat.isOpen = false;
                this.chat.isLoading = false;
            }

            // 포트폴리오에서 떠날 때 Chart.js 인스턴스 정리
            if (this.currentPage === 'portfolio' && page !== 'portfolio') {
                if (this.portfolio.chartInstance) {
                    this.portfolio.chartInstance.destroy();
                    this.portfolio.chartInstance = null;
                }
                if (this.portfolio.financialChartInstance) {
                    this.portfolio.financialChartInstance.destroy();
                    this.portfolio.financialChartInstance = null;
                }
                if (this.portfolio._secChartInstance) {
                    this.portfolio._secChartInstance.destroy();
                    this.portfolio._secChartInstance = null;
                }
            }

            // 월급 사용 비율에서 떠날 때 Chart.js 인스턴스 정리
            if (this.currentPage === 'salary' && page !== 'salary') {
                this.destroySalaryCharts();
            }

            // 투자 노트에서 떠날 때 Chart.js 인스턴스 정리
            if (this.currentPage === 'stocknote' && page !== 'stocknote') {
                this.destroyStocknoteCharts();
            }

            this.currentPage = page;
            history.pushState(null, '', '#' + page);
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
                case 'news-search':
                    break;
                case 'ecos':
                    this.initEcosCharts();
                    if (this.ecos.categories.length === 0) await this.loadEcosCategories();
                    break;
                case 'global':
                    if (this.globalData.categories.length === 0) await this.loadGlobalCategories();
                    break;
                case 'portfolio':
                    await this.loadPortfolio();
                    break;
                case 'salary':
                    await this.loadSalaryInitial();
                    break;
                case 'stocknote':
                    await this.loadStocknote();
                    break;
                case 'admin-logs':
                    await this.loadAdminLogs();
                    break;
            }
        }
    };
}