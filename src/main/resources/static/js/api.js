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
    getNewsByKeyword(keyword, page = 0, size = 20) {
        return this.request('GET', `/api/news?keyword=${encodeURIComponent(keyword)}&page=${page}&size=${size}`);
    },

    collectNewsByKeyword(keyword, userId, region) {
        return this.request('POST', '/api/news/collect', { keyword, userId, region });
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
    }
};
