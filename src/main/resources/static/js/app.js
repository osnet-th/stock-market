/** Dashboard - 코어(라우팅, 초기화) + 컴포넌트 통합 */
function dashboard() {
    return {
        // ==================== 코어 상태 ====================
        currentPage: 'home',

        menus: [
            { key: 'home', label: '대시보드', icon: 'home' },
            { key: 'keywords', label: '키워드', icon: 'tag' },
            { key: 'ecos', label: '국내 경제지표', icon: 'chart' },
            { key: 'global', label: '글로벌 경제지표', icon: 'globe' },
            { key: 'portfolio', label: '포트폴리오', icon: 'portfolio' }
        ],

        sidebarCollapsed: localStorage.getItem('sidebarCollapsed') === 'true',

        // ==================== 반응형 상태 ====================
        isMobile: false,
        mobileDrawerOpen: false,
        _scrollLockCount: 0,
        _navigating: false,
        _drawerTransitioning: false,

        // ==================== 컴포넌트 통합 ====================
        ...AuthComponent,
        ...HomeComponent,
        ...KeywordComponent,
        ...NewsComponent,
        ...EcosComponent,
        ...GlobalComponent,
        ...PortfolioComponent,
        ...FinancialComponent,
        ...ChatComponent,

        // ==================== 코어 메서드 ====================
        toggleSidebar() {
            this.sidebarCollapsed = !this.sidebarCollapsed;
            localStorage.setItem('sidebarCollapsed', this.sidebarCollapsed);
        },

        // ==================== 반응형 메서드 ====================
        toggleMobileDrawer() {
            if (this._drawerTransitioning) return;
            this._drawerTransitioning = true;
            this.mobileDrawerOpen = !this.mobileDrawerOpen;
            if (this.mobileDrawerOpen) this.lockScroll();
            else this.unlockScroll();
            setTimeout(() => { this._drawerTransitioning = false; }, 200);
        },

        lockScroll() {
            this._scrollLockCount++;
            if (this._scrollLockCount === 1) {
                document.body.style.overflow = 'hidden';
            }
        },

        unlockScroll() {
            this._scrollLockCount = Math.max(0, this._scrollLockCount - 1);
            if (this._scrollLockCount === 0) {
                document.body.style.overflow = '';
            }
        },

        async init() {
            // 반응형 breakpoint 감지 (matchMedia 전용 — resize 이벤트 사용 안 함)
            const mql = window.matchMedia('(max-width: 1023px)');
            const handleChange = (e) => {
                this.isMobile = e.matches;
                if (!e.matches) {
                    this.mobileDrawerOpen = false;
                    // 데스크탑 전환 시 스크롤 잠금 해제
                    if (this._scrollLockCount > 0) {
                        this._scrollLockCount = 0;
                        document.body.style.overflow = '';
                    }
                }
            };
            mql.addEventListener('change', handleChange);
            this.isMobile = mql.matches;

            this.handleOAuthCallback();

            if (!this.checkLoggedIn()) {
                window.location.href = '/login.html';
                return;
            }

            await this.loadMyProfile();

            if (this.auth.role === 'SIGNING_USER') {
                window.location.href = '/signup.html';
                return;
            }

            await this.loadHomeSummary();
        },

        async navigateTo(page) {
            if (this._navigating) return;
            this._navigating = true;

            // 모바일 드로어 즉시 닫기
            if (this.mobileDrawerOpen) {
                this.mobileDrawerOpen = false;
                this.unlockScroll();
            }

            try {
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
                }

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
            } finally {
                this._navigating = false;
            }
        }
    };
}