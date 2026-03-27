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
        globalCategories: []
    },

    async loadHomeSummary() {
        try {
            const results = await Promise.allSettled([
                this.checkLoggedIn() ? API.getKeywords(this.auth.userId) : Promise.resolve(null),
                API.getEcosCategories(),
                API.getGlobalCategories(),
                this.checkLoggedIn() ? API.getPortfolioItems(this.auth.userId) : Promise.resolve(null)
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
        } catch (e) {
            console.error('홈 요약 로드 실패:', e);
        }
    }
};
