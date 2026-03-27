/**
 * GlobalComponent - 글로벌 경제지표
 *
 * 소유 프로퍼티: globalData
 * 메서드: loadGlobalCategories, selectGlobalCategory, selectGlobalIndicator,
 *         getCurrentCategoryIndicators, getChangeInfo, getGlobalCategoryLabel
 */
const GlobalComponent = {
    globalData: {
        categories: [],
        selectedCategory: null,
        selectedIndicator: null,
        indicatorData: null,
        loading: false,
        _requestGeneration: 0
    },

    async loadGlobalCategories() {
        try {
            this.globalData.categories = await API.getGlobalCategories() || [];
            if (this.globalData.categories.length > 0 && !this.globalData.selectedCategory) {
                this.globalData.selectedCategory = this.globalData.categories[0].key;
                const firstIndicators = this.globalData.categories[0].indicators;
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
        const cat = this.globalData.categories.find((c) => c.key === categoryKey);
        if (cat && cat.indicators && cat.indicators.length > 0) {
            await this.selectGlobalIndicator(cat.indicators[0].key);
        }
    },

    async selectGlobalIndicator(indicatorKey) {
        this.globalData.selectedIndicator = indicatorKey;

        const thisGeneration = ++this.globalData._requestGeneration;
        this.globalData.loading = true;

        try {
            const result = await API.getGlobalIndicator(indicatorKey);
            if (thisGeneration !== this.globalData._requestGeneration) return;
            this.globalData.indicatorData = result;
        } catch (e) {
            if (thisGeneration !== this.globalData._requestGeneration) return;
            console.error('글로벌 지표 로드 실패:', e);
            this.globalData.indicatorData = null;
        } finally {
            if (thisGeneration === this.globalData._requestGeneration) {
                this.globalData.loading = false;
            }
        }
    },

    getCurrentCategoryIndicators() {
        if (!this.globalData.selectedCategory) return [];
        const cat = this.globalData.categories.find((c) => c.key === this.globalData.selectedCategory);
        return cat ? cat.indicators || [] : [];
    },

    getChangeInfo(current, previous) {
        return Format.change(current, previous);
    },

    getGlobalCategoryLabel(key) {
        const cat = this.globalData.categories.find((c) => c.key === key);
        return cat ? cat.displayName : key;
    }
};