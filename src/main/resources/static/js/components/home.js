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
        dashboardLoading: false,
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
                API.getRecentUpdates()
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
        } catch (e) {
            console.error('홈 요약 로드 실패:', e);
        } finally {
            this.homeSummary.dashboardLoading = false;
        }
    },

    /**
     * 대시보드: 국내 영역 표시 여부
     */
    hasEcosDashboardContent() {
        const hasUpdates = this.homeSummary.recentUpdates?.ecos?.length > 0;
        const hasFavorites = this.homeSummary.enrichedFavorites?.ecos?.length > 0;
        return hasUpdates || hasFavorites;
    },

    /**
     * 대시보드: 글로벌 영역 표시 여부
     */
    hasGlobalDashboardContent() {
        const hasUpdates = this.homeSummary.recentUpdates?.global?.length > 0;
        const hasFavorites = this.homeSummary.enrichedFavorites?.global?.length > 0;
        return hasUpdates || hasFavorites;
    },

    /**
     * 관심 지표 해제 후 대시보드 카드 즉시 제거
     */
    async removeDashboardFavorite(sourceType, indicatorCode) {
        await this.toggleFavorite(sourceType, indicatorCode);
        // 차트 인스턴스 메모리 정리 (GRAPH 모드 카드였을 수 있음)
        this.destroyFavoriteChart(indicatorCode);
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
    }
};