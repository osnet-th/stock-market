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

    getEcosCategoryLabel(name) {
        const cat = this.ecos.categories.find(c => c.name === name);
        return cat ? cat.label : name;
    }
};