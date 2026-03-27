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

        async init() {
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
        }
    };
}