# app.js 추가 예시

## 상태 추가

```javascript
// ==================== News State ====================
news: {
    keywords: [],
    selectedKeyword: null,
    list: [],
    page: 0,
    size: 20,
    totalPages: 0,
    totalElements: 0,
    loading: false
},
```

## 메뉴 추가

```javascript
menus: [
    { key: 'home', label: '대시보드', icon: 'home' },
    { key: 'keywords', label: '키워드 관리', icon: 'tag' },
    { key: 'news', label: '키워드 뉴스', icon: 'news' },
    { key: 'ecos', label: '국내 경제지표', icon: 'chart' },
    { key: 'global', label: '글로벌 경제지표', icon: 'globe' }
],
```

## navigateTo 케이스 추가

```javascript
case 'news':
    if (this.checkLoggedIn() && this.news.keywords.length === 0) await this.loadNewsKeywords();
    break;
```

## 메서드 추가

```javascript
// ==================== News Methods ====================
async loadNewsKeywords() {
    try {
        var allKeywords = await API.getKeywords(this.auth.userId, true) || [];
        this.news.keywords = allKeywords;
        if (allKeywords.length > 0 && !this.news.selectedKeyword) {
            await this.selectNewsKeyword(allKeywords[0].keyword);
        }
    } catch (e) {
        console.error('뉴스 키워드 로드 실패:', e);
        this.news.keywords = [];
    }
},

async selectNewsKeyword(keyword) {
    this.news.selectedKeyword = keyword;
    this.news.page = 0;
    await this.loadNews(0);
},

async loadNews(page) {
    if (!this.news.selectedKeyword) return;
    this.news.loading = true;
    try {
        var result = await API.getNewsByKeyword(this.news.selectedKeyword, page, this.news.size);
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
},
```