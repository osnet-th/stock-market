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

    toggleNotification(enabled) {
        return this.request('PATCH', '/api/users/me/notification', { enabled });
    },

    // Auth
    signup(userId, name, nickname, phoneNumber) {
        return this.request('POST', '/signup', { userId, name, nickname, phoneNumber });
    },

    // Keywords
    getKeywords(userId, active = null) {
        var url = '/api/keywords?userId=' + userId;
        if (active !== null) url += '&active=' + active;
        return this.request('GET', url);
    },

    registerKeyword(keyword, userId, region) {
        return this.request('POST', '/api/keywords', { keyword, userId, region });
    },

    activateKeyword(id, userId) {
        return this.request('PATCH', '/api/keywords/' + id + '/activate?userId=' + userId);
    },

    deactivateKeyword(id, userId) {
        return this.request('PATCH', '/api/keywords/' + id + '/deactivate?userId=' + userId);
    },

    deleteKeyword(id, userId) {
        return this.request('DELETE', '/api/keywords/' + id + '?userId=' + userId);
    },

    // ECOS Indicators
    getEcosCategories() {
        return this.request('GET', '/api/economics/indicators/categories');
    },

    getEcosIndicators(category) {
        return this.request('GET', `/api/economics/indicators?category=${category}`);
    },

    getEcosIndicatorHistory(category) {
        return this.request('GET', `/api/economics/indicators/history?category=${category}`);
    },

    // News
    getNewsByKeyword(keywordId, page, size) {
        page = page || 0;
        size = size || 20;
        return this.request('GET', '/api/news?keywordId=' + keywordId + '&page=' + page + '&size=' + size);
    },

    collectNewsByKeyword(keywordId, keyword, region) {
        return this.request('POST', '/api/news/collect', { keywordId: keywordId, keyword: keyword, region: region });
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

    addCashItem(userId, body) {
        return this.request('POST', `/api/portfolio/items/cash?userId=${userId}`, body);
    },

    addGeneralItem(userId, body) {
        return this.request('POST', `/api/portfolio/items/general?userId=${userId}`, body);
    },

    // 추가 매수
    addStockPurchase(userId, itemId, body) {
        return this.request('POST', `/api/portfolio/items/stock/${itemId}/purchase?userId=${userId}`, body);
    },

    // 매수이력
    getPurchaseHistories(userId, itemId) {
        return this.request('GET', `/api/portfolio/items/stock/${itemId}/purchases?userId=${userId}`);
    },

    updatePurchaseHistory(userId, itemId, historyId, body) {
        return this.request('PUT', `/api/portfolio/items/stock/${itemId}/purchases/${historyId}?userId=${userId}`, body);
    },

    deletePurchaseHistory(userId, itemId, historyId) {
        return this.request('DELETE', `/api/portfolio/items/stock/${itemId}/purchases/${historyId}?userId=${userId}`);
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

    updateCashItem(userId, itemId, body) {
        return this.request('PUT', `/api/portfolio/items/cash/${itemId}?userId=${userId}`, body);
    },

    updateGeneralItem(userId, itemId, body) {
        return this.request('PUT', `/api/portfolio/items/general/${itemId}?userId=${userId}`, body);
    },

    // 삭제
    deletePortfolioItem(userId, itemId, restoreCash = false, restoreAmount = null) {
        let url = `/api/portfolio/items/${itemId}?userId=${userId}&restoreCash=${restoreCash}`;
        if (restoreAmount != null) {
            url += `&restoreAmount=${restoreAmount}`;
        }
        return this.request('DELETE', url);
    },

    // 뉴스 토글
    togglePortfolioNews(userId, itemId, enabled) {
        return this.request('PATCH', `/api/portfolio/items/${itemId}/news?userId=${userId}`, { enabled });
    },

    // ==================== Stock Prices ====================
    getStockPrices(stocks) {
        return this.request('POST', '/api/stocks/prices', { stocks });
    },

    // ==================== Stock Financial ====================

    getFinancialOptions() {
        return this.request('GET', '/api/stocks/financial/options');
    },

    getFinancialAccounts(stockCode, year, reportCode) {
        return this.request('GET',
            `/api/stocks/${stockCode}/financial/accounts?year=${year}&reportCode=${reportCode}`);
    },

    getFinancialIndices(stockCode, year, reportCode, indexClassCode) {
        return this.request('GET',
            `/api/stocks/${stockCode}/financial/indices?year=${year}&reportCode=${reportCode}&indexClassCode=${indexClassCode}`);
    },

    getFinancialDividends(stockCode, year, reportCode) {
        return this.request('GET',
            `/api/stocks/${stockCode}/financial/dividends?year=${year}&reportCode=${reportCode}`);
    },

    getFinancialStockQuantities(stockCode, year, reportCode) {
        return this.request('GET',
            `/api/stocks/${stockCode}/financial/stock-quantities?year=${year}&reportCode=${reportCode}`);
    },

    getFullFinancialStatements(stockCode, year, reportCode, fsDiv) {
        return this.request('GET',
            `/api/stocks/${stockCode}/financial/full-statements?year=${year}&reportCode=${reportCode}&fsDiv=${fsDiv}`);
    },

    getLawsuits(stockCode, startDate, endDate) {
        return this.request('GET',
            `/api/stocks/${stockCode}/financial/lawsuits?startDate=${startDate}&endDate=${endDate}`);
    },

    getPrivateFundUsages(stockCode, year, reportCode) {
        return this.request('GET',
            `/api/stocks/${stockCode}/financial/private-fund-usages?year=${year}&reportCode=${reportCode}`);
    },

    getPublicFundUsages(stockCode, year, reportCode) {
        return this.request('GET',
            `/api/stocks/${stockCode}/financial/public-fund-usages?year=${year}&reportCode=${reportCode}`);
    },

    // ==================== SEC Financial (해외주식) ====================
    getSecFinancialStatements(ticker) {
        return this.request('GET', `/api/stocks/${ticker}/sec/financial/statements`);
    },

    getSecQuarterlyStatements(ticker) {
        return this.request('GET', `/api/stocks/${ticker}/sec/financial/statements/quarterly`);
    },

    getSecInvestmentMetrics(ticker) {
        return this.request('GET', `/api/stocks/${ticker}/sec/financial/metrics`);
    },

    // ==================== Chat ====================
    async streamChat(userId, message, chatMode, stockCode, onChunk, onDone, onError, signal) {
        try {
            const response = await fetch(`${this.baseUrl}/api/chat?userId=${userId}`, {
                method: 'POST',
                headers: this.getHeaders(),
                body: JSON.stringify({ message, chatMode, stockCode }),
                signal: signal
            });

            if (response.status === 401) {
                localStorage.removeItem('accessToken');
                localStorage.removeItem('userId');
                window.location.href = '/login.html';
                return;
            }

            if (!response.ok) {
                const errorText = await response.text();
                onError(new Error(`API Error ${response.status}: ${errorText}`));
                return;
            }

            const reader = response.body.getReader();
            const decoder = new TextDecoder();
            let buffer = '';

            while (true) {
                const { done, value } = await reader.read();
                if (done) break;

                buffer += decoder.decode(value, { stream: true });
                const lines = buffer.split('\n');
                buffer = lines.pop();

                for (const line of lines) {
                    if (line.startsWith('data:')) {
                        const text = line.substring(5);
                        if (text.trim()) {
                            onChunk(text);
                        }
                    }
                }
            }

            onDone();
        } catch (error) {
            onError(error);
        }
    }
};
