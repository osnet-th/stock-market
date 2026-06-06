/** Stock Evaluation - 종목 평가 (KIS 국내주식 종목정보). 보유 여부 무관 매수 전 리서치 */
const StockEvalComponent = {
    stockEval: {
        searchQuery: '',
        searchResults: [],
        searchLoading: false,
        selected: null,          // { stockCode, stockName, ... }
        basicInfo: null,
        basicInfoLoading: false,
        error: '',
        activeTab: 'finance',    // finance | estimate-credit | schedule
        _searchGen: 0,
        _infoGen: 0,
    },

    async stockEvalSearch() {
        const q = this.stockEval.searchQuery.trim();
        if (!q) return;
        const gen = ++this.stockEval._searchGen;
        this.stockEval.searchLoading = true;
        try {
            const results = await API.searchStocks(q) || [];
            if (gen !== this.stockEval._searchGen) return; // 레이스 가드
            // 국내주식(KRX)만 평가 대상
            this.stockEval.searchResults = results.filter(r => r.exchangeCode === 'KRX');
        } catch (e) {
            if (gen !== this.stockEval._searchGen) return;
            console.error('종목 검색 실패:', e);
            this.stockEval.searchResults = [];
        } finally {
            if (gen === this.stockEval._searchGen) this.stockEval.searchLoading = false;
        }
    },

    async stockEvalSelect(stock) {
        this.stockEval.selected = stock;
        this.stockEval.searchResults = [];
        this.stockEval.searchQuery = '';
        this.stockEval.activeTab = 'finance';
        await this.stockEvalLoadBasicInfo();
    },

    async stockEvalLoadBasicInfo() {
        if (!this.stockEval.selected) return;
        const code = this.stockEval.selected.stockCode;
        const gen = ++this.stockEval._infoGen;
        this.stockEval.basicInfoLoading = true;
        this.stockEval.error = '';
        try {
            const info = await API.getStockBasicInfo(code);
            if (gen !== this.stockEval._infoGen) return; // 레이스 가드
            this.stockEval.basicInfo = info;
        } catch (e) {
            if (gen !== this.stockEval._infoGen) return;
            this.stockEval.basicInfo = null;
            this.stockEval.error = '종목 기본정보를 불러올 수 없습니다.';
        } finally {
            if (gen === this.stockEval._infoGen) this.stockEval.basicInfoLoading = false;
        }
    },

    stockEvalSelectTab(tab) {
        this.stockEval.activeTab = tab;
    },

    stockEvalReset() {
        this.stockEval.selected = null;
        this.stockEval.basicInfo = null;
        this.stockEval.error = '';
        this.stockEval.searchResults = [];
        this.stockEval.searchQuery = '';
    },
};
