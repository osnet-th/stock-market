/**
 * NewsComponent - 뉴스 수집/조회/페이징
 *
 * 소유 프로퍼티: news
 * 메서드: collectNews, selectNewsKeyword, loadNews
 */
const NewsComponent = {
    news: {
        selectedKeywordId: null,
        selectedKeywordText: null,
        collectingKeywordId: null,
        list: [],
        page: 0,
        size: 20,
        totalPages: 0,
        totalElements: 0,
        loading: false
    },

    async collectNews(kw) {
        if (this.news.collectingKeywordId) return;
        this.news.collectingKeywordId = kw.id;
        try {
            const result = await API.collectNewsByKeyword(kw.id, kw.keyword, kw.region);
            let msg = '수집 완료: ' + result.successCount + '건 저장';
            if (result.ignoredCount > 0) msg += ', ' + result.ignoredCount + '건 중복';
            alert(msg);

            if (this.news.selectedKeywordId === kw.id) {
                await this.loadNews(0);
            }
        } catch (e) {
            console.error('뉴스 수집 실패:', e);
            alert('뉴스 수집에 실패했습니다.');
        } finally {
            this.news.collectingKeywordId = null;
        }
    },

    async selectNewsKeyword(kw) {
        if (this.news.selectedKeywordId === kw.id) {
            this.news.selectedKeywordId = null;
            this.news.selectedKeywordText = null;
            this.news.list = [];
            return;
        }
        this.news.selectedKeywordId = kw.id;
        this.news.selectedKeywordText = kw.keyword;
        this.news.page = 0;
        await this.loadNews(0);
    },

    async loadNews(page) {
        if (!this.news.selectedKeywordId) return;
        this.news.loading = true;
        try {
            const result = await API.getNewsByKeyword(this.news.selectedKeywordId, page, this.news.size);
            this.news.list = result.content || [];
            this.news.page = result.page;
            this.news.totalPages = result.totalPages;
            this.news.totalElements = result.totalElements;
        } catch (e) {
            console.error('뉴스 로드 실패:', e);
            this.news.list = [];
        } finally {
            this.news.loading = false;
        }
    }
};