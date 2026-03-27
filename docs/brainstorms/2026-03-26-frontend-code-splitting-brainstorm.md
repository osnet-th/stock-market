# 프론트엔드 코드 기능별 분리

## What We're Building

app.js(2,383줄, 148개 메서드)와 index.html(2,383줄)을 기능별로 분리하여 유지보수성을 향상시킨다.

**현재 상태:**
- `app.js`: 단일 `dashboard()` 함수에 모든 기능(라우팅, 인증, 키워드, ECOS, 글로벌지표, 뉴스, 포트폴리오, 재무분석, 챗봇, 홈 대시보드)이 포함
- `index.html`: 모든 페이지 HTML이 단일 파일에 포함
- `components/ecos.js`, `components/keyword.js`, `components/global.js` 파일이 존재하지만 **실제로 로드/사용되지 않음** (app.js에 동일 코드 중복)
- `api.js`, `utils/format.js`는 이미 적절히 분리됨

## Why This Approach (방법 3: 하이브리드)

**기존 패턴 확장 + 점진적 Alpine.store 도입**을 선택한 이유:

1. **빌드 도구 없이 진행** — 현재 CDN + 순수 JS 구조를 유지. webpack/Vite 도입 비용 없음
2. **단계적 리스크 분산** — 먼저 파일 분리(안전), 이후 공유 상태만 Alpine.store로 전환(점진적)
3. **유지보수성 향상이 핵심 목적** — 기능별로 파일을 찾고 수정하기 쉽게

## Key Decisions

### 1. JS 분리 구조

app.js의 기능을 다음과 같이 분리:

| 파일 | 책임 | 예상 규모 |
|------|------|-----------|
| `app.js` | 코어(라우팅, 초기화, 메뉴, 사이드바) + 컴포넌트 통합 | ~100줄 |
| `components/auth.js` | 인증 상태, 로그인 체크 | ~30줄 |
| `components/keyword.js` | 키워드 CRUD, 필터링 | ~80줄 |
| `components/ecos.js` | ECOS 카테고리/지표, 스프레드 계산 | ~300줄 |
| `components/global.js` | 글로벌 경제지표 | ~60줄 |
| `components/news.js` | 뉴스 수집/조회/페이징 | ~80줄 |
| `components/portfolio.js` | 포트폴리오 CRUD, 주가검색, 자산배분 | ~400줄 |
| `components/financial.js` | 재무제표, 배당, 소송 | ~200줄 |
| `components/chat.js` | 챗봇 SSE 스트리밍, 드래그, 마크다운 렌더링 | ~200줄 |
| `components/home.js` | 홈 대시보드 요약 | ~50줄 |

### 2. 컴포넌트 통합 패턴

```javascript
// app.js - 코어 + 컴포넌트 통합
function dashboard() {
    return {
        // 코어 상태 (라우팅, 메뉴, 사이드바)
        currentPage: 'home',
        menus: [...],

        // 각 컴포넌트의 상태와 메서드를 스프레드로 통합
        ...AuthComponent,
        ...KeywordComponent,
        ...EcosComponent,
        ...GlobalComponent,
        ...NewsComponent,
        ...PortfolioComponent,
        ...FinancialComponent,
        ...ChatComponent,
        ...HomeComponent,

        // init, navigateTo 등 코어 메서드
        init() { ... },
        navigateTo(page) { ... }
    };
}
```

### 3. HTML 분리 구조

index.html에서 각 페이지 섹션을 별도 HTML 파일로 분리:

```
static/
├── index.html              (코어 레이아웃: 사이드바 + 페이지 컨테이너)
├── pages/
│   ├── home.html           (대시보드 요약)
│   ├── keywords.html       (키워드 관리)
│   ├── ecos.html           (국내 경제지표)
│   ├── global.html         (글로벌 경제지표)
│   ├── portfolio.html      (포트폴리오)
│   └── financial.html      (재무분석 - 포트폴리오 하위)
└── partials/
    └── chat.html           (챗봇 패널 - 모든 페이지에서 사용)
```

**HTML 로딩 방식**: `fetch()`로 HTML 조각을 동적 로드하여 `x-html`로 렌더링, 또는 Alpine.js `x-if`로 조건부 렌더링 유지하되 파일만 분리 후 빌드 시 합침

### 4. Alpine.store 도입 범위 (2단계)

1단계 완료 후, 여러 컴포넌트가 공유하는 상태만 `Alpine.store`로 전환:

- `Alpine.store('auth')` — 인증 토큰, 사용자 정보 (모든 컴포넌트에서 참조)
- `Alpine.store('router')` — 현재 페이지, 네비게이션 (모든 컴포넌트에서 참조)

기능별 상태(keywords, ecos, portfolio 등)는 각 컴포넌트 내부에 유지.

### 5. script 태그 로딩 순서

```html
<!-- 외부 라이브러리 -->
<script src="https://cdn.tailwindcss.com"></script>
<script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
<script src="https://cdn.jsdelivr.net/npm/marked/marked.min.js"></script>

<!-- 내부 유틸/API -->
<script src="/js/api.js"></script>
<script src="/js/utils/format.js"></script>

<!-- 컴포넌트 (순서 무관 - 객체 리터럴 선언만) -->
<script src="/js/components/auth.js"></script>
<script src="/js/components/keyword.js"></script>
<script src="/js/components/ecos.js"></script>
<script src="/js/components/global.js"></script>
<script src="/js/components/news.js"></script>
<script src="/js/components/portfolio.js"></script>
<script src="/js/components/financial.js"></script>
<script src="/js/components/chat.js"></script>
<script src="/js/components/home.js"></script>

<!-- 메인 앱 (컴포넌트 통합) -->
<script src="/js/app.js"></script>

<!-- Alpine.js (마지막) -->
<script src="https://cdn.jsdelivr.net/npm/alpinejs@3/dist/cdn.min.js"></script>
```

## Resolved Questions

1. **HTML 동적 로딩 vs 정적 합침** → `fetch()`로 런타임 동적 로드. 파일 분리 효과가 명확하고 별도 빌드 스크립트 불필요.
2. **미사용 components 파일 처리** → 기존 코드를 기반으로 활용하되 app.js의 최신 코드와 동기화하여 업데이트.