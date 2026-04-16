/**
 * FavoriteComponent - 관심 지표 상태 관리
 * 소유 프로퍼티: favorites
 */
const FavoriteComponent = {
    favorites: {
        list: [],
        loading: false,
    },

    initFavorites() {
        Object.defineProperty(this.favorites, '_set', {
            value: new Set(), writable: true, enumerable: false
        });
        Object.defineProperty(this.favorites, '_togglePending', {
            value: false, writable: true, enumerable: false
        });
    },

    async loadFavorites() {
        if (!this.checkLoggedIn()) return;
        try {
            const result = await API.getFavorites();
            this.favorites.list = result || [];
            if (!this.favorites._set) this.initFavorites();
            this.favorites._set.clear();
            this.favorites.list.forEach(f => {
                this.favorites._set.add(f.sourceType + '::' + f.indicatorCode);
            });
        } catch (e) {
            console.error('관심 지표 로드 실패:', e);
        }
    },

    isFavorited(sourceType, indicatorCode) {
        if (!this.favorites._set) return false;
        return this.favorites._set.has(sourceType + '::' + indicatorCode);
    },

    async toggleFavorite(sourceType, indicatorCode) {
        if (!this.checkLoggedIn()) return;
        if (!this.favorites._set) this.initFavorites();
        if (this.favorites._togglePending) return;
        this.favorites._togglePending = true;
        setTimeout(() => { this.favorites._togglePending = false; }, 300);

        const key = sourceType + '::' + indicatorCode;
        const wasFavorited = this.favorites._set.has(key);

        // Optimistic UI
        if (wasFavorited) {
            this.favorites._set.delete(key);
            this.favorites.list = this.favorites.list.filter(f =>
                !(f.sourceType === sourceType && f.indicatorCode === indicatorCode));
        } else {
            this.favorites._set.add(key);
            this.favorites.list.push({ sourceType, indicatorCode });
        }

        try {
            if (wasFavorited) {
                await API.deleteFavorite(sourceType, indicatorCode);
            } else {
                await API.addFavorite(sourceType, indicatorCode);
            }
        } catch (e) {
            // Rollback
            if (wasFavorited) {
                this.favorites._set.add(key);
                this.favorites.list.push({ sourceType, indicatorCode });
            } else {
                this.favorites._set.delete(key);
                this.favorites.list = this.favorites.list.filter(f =>
                    !(f.sourceType === sourceType && f.indicatorCode === indicatorCode));
            }
            console.error('관심 지표 토글 실패:', e);
        }
    },

    /**
     * ECOS 지표의 indicatorCode 생성 (className::keystatName)
     */
    ecosIndicatorCode(indicator) {
        return indicator.className + '::' + indicator.keystatName;
    },

    /**
     * Global 지표의 indicatorCode 생성 (countryName::indicatorType)
     */
    globalIndicatorCode(countryName, indicatorType) {
        return countryName + '::' + indicatorType;
    },

    /**
     * 수평 스크롤
     */
    scrollFavorites(direction, containerId) {
        const container = document.getElementById(containerId);
        if (!container) return;
        container.scrollBy({ left: direction * 320, behavior: 'smooth' });
    }
};