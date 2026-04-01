# app.js 변경 예시

## 1. currentPage 초기값 (라인 5)

```javascript
// before
currentPage: 'home',

// after
currentPage: (() => {
    const hash = location.hash.replace('#', '');
    const validPages = ['home', 'keywords', 'ecos', 'global', 'portfolio'];
    return validPages.includes(hash) ? hash : 'home';
})(),
```

## 2. navigateTo() - hash 업데이트 추가 (라인 100)

```javascript
async navigateTo(page) {
    this.closeMobileDrawer();

    // 포트폴리오에서 떠날 때 Chart.js 인스턴스 정리
    if (this.currentPage === 'portfolio' && page !== 'portfolio') {
        if (this.portfolio.chartInstance) {
            this.portfolio.chartInstance.destroy();
            this.portfolio.chartInstance = null;
        }
        if (this.portfolio.financialChartInstance) {
            this.portfolio.financialChartInstance.destroy();
            this.portfolio.financialChartInstance = null;
        }
    }

    this.currentPage = page;
    history.pushState(null, '', '#' + page);  // 추가

    switch (page) {
        case 'home':
            await this.loadHomeSummary();
            break;
        case 'keywords':
            if (this.checkLoggedIn()) {
                await this.loadKeywords();
                this.news.selectedKeywordId = null;
                this.news.selectedKeywordText = null;
                this.news.list = [];
            }
            break;
        case 'ecos':
            if (this.ecos.categories.length === 0) await this.loadEcosCategories();
            break;
        case 'global':
            if (this.globalData.categories.length === 0) await this.loadGlobalCategories();
            break;
        case 'portfolio':
            await this.loadPortfolio();
            break;
    }
}
```

## 3. init() - hash 기반 초기 로드 + popstate 리스너 (라인 52-83)

```javascript
async init() {
    if (this._mqlCleanup) return;

    // 반응형 breakpoint 감지
    const mql = window.matchMedia('(max-width: 1023px)');
    const handleChange = (e) => {
        this.isMobile = e.matches;
        if (!e.matches) {
            this.closeMobileDrawer();
        }
    };
    mql.addEventListener('change', handleChange);
    this._mqlCleanup = () => mql.removeEventListener('change', handleChange);
    this.isMobile = mql.matches;

    // 브라우저 뒤로가기/앞으로가기 대응
    window.addEventListener('popstate', () => {
        const hash = location.hash.replace('#', '');
        const validPages = this.menus.map(m => m.key);
        const page = validPages.includes(hash) ? hash : 'home';
        if (this.currentPage !== page) {
            this.navigateTo(page);
        }
    });

    this.handleOAuthCallback();

    if (!this.checkLoggedIn()) {
        window.location.href = '/login.html';
        return;
    }

    await this.loadMyProfile();

    if (this.auth.role === 'SIGNING_USER') {
        window.location.href = '/signup.html';
        return;
    }

    // hash 기반 초기 페이지 로드 (home이 아닌 경우 해당 페이지 데이터 로드)
    if (this.currentPage !== 'home') {
        await this.navigateTo(this.currentPage);
    } else {
        await this.loadHomeSummary();
    }
}
```

### 주의사항

- `popstate` 에서 `navigateTo()` 호출 시 내부에서 다시 `pushState`가 호출되므로, 무한 루프 방지를 위해 `currentPage !== page` 조건 필수
- `navigateTo()` 내부 `pushState`가 `popstate` 시에도 실행되면 히스토리가 꼬이므로, popstate 핸들러에서는 `replaceState` 방식의 별도 메서드를 사용하는 것도 고려 가능. 다만 `pushState`는 `popstate`를 트리거하지 않으므로 현재 구조에서는 문제 없음.
