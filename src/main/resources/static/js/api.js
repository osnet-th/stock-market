const API = {
    baseUrl: '',

    getHeaders() {
        const token = localStorage.getItem('accessToken');
        return {
            'Content-Type': 'application/json',
            ...(token && { 'Authorization': `Bearer ${token}` })
        };
    },

    async request(method, url, body = null) {
        const options = {
            method,
            headers: this.getHeaders(),
        };
        if (body) options.body = JSON.stringify(body);

        const response = await fetch(`${this.baseUrl}${url}`, options);

        if (response.status === 401) {
            localStorage.removeItem('accessToken');
            localStorage.removeItem('userId');
            window.location.href = '/login.html';
            return;
        }

        if (response.status === 204) return null;

        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(`API Error ${response.status}: ${errorText}`);
        }

        return response.json();
    },

    // Users
    getMyProfile() {
        return this.request('GET', '/api/users/me');
    },

    // Auth
    signup(userId, name, nickname, phoneNumber) {
        return this.request('POST', '/signup', { userId, name, nickname, phoneNumber });
    },

    // Keywords
    getKeywords(userId, active = null) {
        let url = `/api/keywords?userId=${userId}`;
        if (active !== null) url += `&active=${active}`;
        return this.request('GET', url);
    },

    registerKeyword(keyword, userId, region) {
        return this.request('POST', '/api/keywords', { keyword, userId, region });
    },

    activateKeyword(id) {
        return this.request('PATCH', `/api/keywords/${id}/activate`);
    },

    deactivateKeyword(id) {
        return this.request('PATCH', `/api/keywords/${id}/deactivate`);
    },

    deleteKeyword(id) {
        return this.request('DELETE', `/api/keywords/${id}`);
    },

    // ECOS Indicators
    getEcosCategories() {
        return this.request('GET', '/api/economics/indicators/categories');
    },

    getEcosIndicators(category) {
        return this.request('GET', `/api/economics/indicators?category=${category}`);
    },

    // News
    getNewsByKeyword(keywordId, page = 0, size = 20) {
        return this.request('GET', `/api/news?purpose=KEYWORD&sourceId=${keywordId}&page=${page}&size=${size}`);
    },

    getNewsByPortfolioItem(portfolioItemId, page = 0, size = 20) {
        return this.request('GET', `/api/news?purpose=PORTFOLIO&sourceId=${portfolioItemId}&page=${page}&size=${size}`);
    },

    collectNewsByKeyword(keywordId, keyword, userId, region) {
        return this.request('POST', '/api/news/collect', { purpose: 'KEYWORD', sourceId: keywordId, keyword, userId, region });
    },

    collectNewsByPortfolioItem(portfolioItemId, keyword, userId, region) {
        return this.request('POST', '/api/news/collect', { purpose: 'PORTFOLIO', sourceId: portfolioItemId, keyword, userId, region });
    },

    // Global Indicators
    getGlobalCategories() {
        return this.request('GET', '/api/economics/global-indicators/categories');
    },

    getGlobalIndicator(indicatorType) {
        return this.request('GET', `/api/economics/global-indicators/${indicatorType}`);
    },

    getGlobalCategoryIndicators(category) {
        return this.request('GET', `/api/economics/global-indicators/categories/${category}`);
    },

    // ==================== Stock Search ====================
    searchStocks(name) {
        return this.request('GET', `/api/stocks/search?name=${encodeURIComponent(name)}`);
    },

    // ==================== Portfolio ====================
    getPortfolioItems(userId) {
        return this.request('GET', `/api/portfolio/items?userId=${userId}`);
    },

    getPortfolioAllocation(userId) {
        return this.request('GET', `/api/portfolio/allocation?userId=${userId}`);
    },

    // 등록 (타입별)
    addStockItem(userId, body) {
        return this.request('POST', `/api/portfolio/items/stock?userId=${userId}`, body);
    },

    addBondItem(userId, body) {
        return this.request('POST', `/api/portfolio/items/bond?userId=${userId}`, body);
    },

    addRealEstateItem(userId, body) {
        return this.request('POST', `/api/portfolio/items/real-estate?userId=${userId}`, body);
    },

    addFundItem(userId, body) {
        return this.request('POST', `/api/portfolio/items/fund?userId=${userId}`, body);
    },

    addGeneralItem(userId, body) {
        return this.request('POST', `/api/portfolio/items/general?userId=${userId}`, body);
    },

    // 추가 매수
    addStockPurchase(userId, itemId, body) {
        return this.request('POST', `/api/portfolio/items/stock/${itemId}/purchase?userId=${userId}`, body);
    },

    // 수정 (타입별)
    updateStockItem(userId, itemId, body) {
        return this.request('PUT', `/api/portfolio/items/stock/${itemId}?userId=${userId}`, body);
    },

    updateBondItem(userId, itemId, body) {
        return this.request('PUT', `/api/portfolio/items/bond/${itemId}?userId=${userId}`, body);
    },

    updateRealEstateItem(userId, itemId, body) {
        return this.request('PUT', `/api/portfolio/items/real-estate/${itemId}?userId=${userId}`, body);
    },

    updateFundItem(userId, itemId, body) {
        return this.request('PUT', `/api/portfolio/items/fund/${itemId}?userId=${userId}`, body);
    },

    updateGeneralItem(userId, itemId, body) {
        return this.request('PUT', `/api/portfolio/items/general/${itemId}?userId=${userId}`, body);
    },

    // 삭제
    deletePortfolioItem(userId, itemId) {
        return this.request('DELETE', `/api/portfolio/items/${itemId}?userId=${userId}`);
    },

    // 뉴스 토글
    togglePortfolioNews(userId, itemId, enabled) {
        return this.request('PATCH', `/api/portfolio/items/${itemId}/news?userId=${userId}`, { enabled });
    }
};
