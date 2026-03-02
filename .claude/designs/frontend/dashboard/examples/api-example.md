# api.js 예시

```javascript
// js/api.js
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
            window.location.href = '/kakao-login.html';
            return;
        }

        if (response.status === 204) return null;
        if (!response.ok) throw new Error(`API Error: ${response.status}`);
        return response.json();
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

    // ECOS
    getEcosCategories() {
        return this.request('GET', '/api/economics/indicators/categories');
    },

    getEcosIndicators(category) {
        return this.request('GET', `/api/economics/indicators?category=${category}`);
    },

    // Global
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
```