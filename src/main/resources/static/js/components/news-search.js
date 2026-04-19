/**
 * NewsSearchComponent - ES 기반 뉴스 전문 검색
 *
 * 소유 프로퍼티: newsSearch
 * 메서드: searchNews, loadNewsSearchPage
 */
const NewsSearchComponent = {
    newsSearch: {
        query: '',
        startDate: '',
        endDate: '',
        region: '',
        list: [],
        page: 0,
        size: 20,
        totalPages: 0,
        totalElements: 0,
        loading: false,
        searched: false
    },

    async searchNews() {
        const query = this.newsSearch.query.trim();
        if (!query) return;

        this.newsSearch.loading = true;
        this.newsSearch.searched = true;
        this.newsSearch.page = 0;

        try {
            const result = await API.searchNews(
                query,
                this.newsSearch.startDate || null,
                this.newsSearch.endDate || null,
                this.newsSearch.region || null,
                0,
                this.newsSearch.size
            );
            this.newsSearch.list = result.content || [];
            this.newsSearch.page = result.page;
            this.newsSearch.totalPages = result.totalPages;
            this.newsSearch.totalElements = result.totalElements;
        } catch (e) {
            console.error('뉴스 검색 실패:', e);
            this.newsSearch.list = [];
            this.newsSearch.totalElements = 0;
            this.newsSearch.totalPages = 0;
        } finally {
            this.newsSearch.loading = false;
        }
    },

    async loadNewsSearchPage(page) {
        if (page < 0 || page >= this.newsSearch.totalPages) return;

        this.newsSearch.loading = true;
        try {
            const result = await API.searchNews(
                this.newsSearch.query.trim(),
                this.newsSearch.startDate || null,
                this.newsSearch.endDate || null,
                this.newsSearch.region || null,
                page,
                this.newsSearch.size
            );
            this.newsSearch.list = result.content || [];
            this.newsSearch.page = result.page;
            this.newsSearch.totalPages = result.totalPages;
            this.newsSearch.totalElements = result.totalElements;
        } catch (e) {
            console.error('뉴스 검색 페이지 로드 실패:', e);
        } finally {
            this.newsSearch.loading = false;
        }
    }
};