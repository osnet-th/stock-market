const GlobalComponent = {
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

        const cat = this.globalData.categories.find(c => c.key === categoryKey);
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
        const cat = this.globalData.categories.find(c => c.key === this.globalData.selectedCategory);
        return cat ? cat.indicators : [];
    },

    getGlobalCategoryLabel(key) {
        const cat = this.globalData.categories.find(c => c.key === key);
        return cat ? cat.displayName : key;
    },

    getChangeInfo(current, previous) {
        return Format.change(current, previous);
    }
};
