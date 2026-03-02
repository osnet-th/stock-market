const EcosComponent = {
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

    getEcosCategoryLabel(name) {
        const cat = this.ecos.categories.find(c => c.name === name);
        return cat ? cat.label : name;
    }
};
