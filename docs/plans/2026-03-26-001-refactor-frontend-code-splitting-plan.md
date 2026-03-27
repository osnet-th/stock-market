---
title: "refactor: app.js 기능별 컴포넌트 파일 분리"
type: refactor
status: active
date: 2026-03-26
origin: docs/brainstorms/2026-03-26-frontend-code-splitting-brainstorm.md
---

# refactor: app.js 기능별 컴포넌트 파일 분리

## 리서치 결과 반영 사항

1. **Phase 2(HTML 분리), Phase 3(Alpine.store) 제거** — 모든 리뷰어가 과도한 복잡성으로 판단. Phase 1만으로 핵심 목표 달성 가능.
2. **portfolio/financial 상태 충돌 해결** — financial.js는 메서드만 제공, portfolio.js가 전체 portfolio 상태 소유.
3. **financial-config.js를 financial.js에 통합** — config의 유일한 소비자가 financial이므로 별도 파일 불필요.
4. **보안 이슈 발견** — marked.parse() DOMPurify 미적용 (기존 문제, 별도 대응 필요).

### 제거된 Phase 및 근거

| 제거된 Phase | 근거 |
|-------------|------|
| Phase 2: HTML 분리 (fetch + initTree) | `x-html`은 Alpine 디렉티브를 초기화하지 않음. `Alpine.initTree()`는 비공식 내부 API. race condition, 로딩 상태, 에러 처리, 캐싱 등 새로운 복잡성 유입. 목표는 개발자 경험(DX)인데 런타임 SPA 아키텍처 문제를 해결하려 함. |
| Phase 3: Alpine.store | spread 패턴에서 모든 컴포넌트가 `this`로 상태를 공유하므로 불필요. `this.auth.userId` → `$store.auth.userId` 변경의 파급 범위가 모든 API 호출/HTML 템플릿에 영향. |

---

## Overview

app.js(2,383줄, 148개 메서드)를 기능별 component 파일로 분리하여 유지보수성을 향상시킨다. 빌드 도구 없이 기존 CDN + 순수 JS 구조를 유지하면서, 기존 orphaned component 패턴(스프레드 연산자)을 확장 적용한다.

**index.html은 현재 상태를 유지한다.** 2,330줄의 HTML은 IDE의 코드 폴딩/검색으로 관리 가능하며, 동적 HTML 로드의 복잡성 대비 실익이 낮다.

(see brainstorm: docs/brainstorms/2026-03-26-frontend-code-splitting-brainstorm.md)

## Problem Statement

- `app.js`: 단일 `dashboard()` 함수에 10개 기능 도메인의 모든 상태/메서드가 혼재
- `components/ecos.js`, `keyword.js`, `global.js`: 분리 시도했으나 **로드되지 않는 orphaned 파일** (app.js에 동일 코드 중복, keyword.js에 `KOREA` vs `DOMESTIC` 불일치 버그 존재)
- 기능 수정 시 2,300줄 파일에서 관련 코드를 찾아야 하므로 유지보수 비용 높음

## Proposed Solution

**단일 Phase**: JS 메서드/상태를 기능별 component 파일로 분리 (리스크 낮음, 동작 변경 없음)

## Technical Approach

### 패턴: 스프레드 연산자를 통한 컴포넌트 통합

각 기능을 `const XxxComponent = { ... }` 객체로 선언하고, `dashboard()`에서 `...XxxComponent`로 스프레드.

```javascript
// app.js - 코어 + 컴포넌트 통합
function dashboard() {
    return {
        // 코어 상태
        currentPage: 'home',
        menus: [...],
        sidebarCollapsed: localStorage.getItem('sidebarCollapsed') === 'true',

        // 컴포넌트 스프레드 (순서 무관)
        ...AuthComponent,
        ...KeywordComponent,
        ...EcosComponent,
        ...GlobalComponent,
        ...NewsComponent,
        ...PortfolioComponent,    // portfolio 전체 상태 포함
        ...FinancialComponent,    // 메서드만 (상태 없음)
        ...ChatComponent,
        ...HomeComponent,

        // 코어 메서드
        toggleSidebar() { ... },
        init() { ... },
        navigateTo(page) { ... }
    };
}
```

### Research Insights

**Alpine.js 반응성 호환성 (확인됨)**:
- spread는 Alpine 초기화 이전에 실행 → 최종 객체를 Alpine이 Proxy로 감싸므로 반응성 정상 유지
- **화살표 함수 제약**: 메서드 자체를 화살표 함수로 정의하면 안 됨 (`toggle: () => {}` ❌). 단, 메서드 내부의 콜백 화살표 함수(`array.filter(x => ...)`)는 `this`를 참조하지 않으면 안전
- Alpine.js 공식 문서에서도 `alpine:init` 이벤트를 통한 컴포넌트 분리를 권장하나, 현재 패턴(`function dashboard()`)에서는 스프레드가 더 간단

**init() 충돌 방지 (중요)**:
- 여러 컴포넌트가 `init()` 메서드를 가지면 마지막 spread만 남음
- **해결**: 각 컴포넌트는 `initXxx()`로 명명 (예: `initAuth()`, `initChat()`)
- app.js의 `init()`에서 필요한 초기화 메서드를 순서대로 호출

**Performance (확인됨)**:
- HTTP/2 환경에서 3 → ~12 JS 파일 증가: **+10-30ms** (무시 가능)
- 10개 객체 spread 비용: **0.01-0.05ms** (무시 가능)
- 캐시 효율성: 단일 파일 수정 시 **2-10KB만 재다운로드** (기존 80KB 전체 재다운로드 대비 큰 개선)

### 분리 구조

| 파일 | 책임 | 포함 상태 | 비고 |
|------|------|-----------|------|
| `app.js` | 코어 (라우팅, 초기화, 메뉴, 사이드바) + 컴포넌트 통합 | `currentPage`, `menus`, `sidebarCollapsed` | ~100줄 |
| `components/auth.js` | 인증, OAuth 콜백, 프로필 | `auth` | 신규 생성 |
| `components/keyword.js` | 키워드 CRUD, 필터링 | `keywords` | orphaned 파일 동기화 |
| `components/ecos.js` | ECOS 카테고리/지표, 스프레드 계산 (11종) | `ecos` | orphaned 파일 동기화 |
| `components/global.js` | 글로벌 경제지표 | `globalData` | orphaned 파일 동기화 |
| `components/news.js` | 뉴스 수집/조회/페이징 | `news` | 신규 생성 |
| `components/portfolio.js` | 포트폴리오 CRUD, 주가검색, 자산배분 | `portfolio` (**financial 포함 전체**) | 신규 생성 |
| `components/financial.js` | 재무제표, 배당, 소송, config 데이터 | **없음 (메서드만)** | 신규 생성 |
| `components/chat.js` | 챗봇 SSE, 드래그, 마크다운 | `chat` | 신규 생성 |
| `components/home.js` | 홈 대시보드 요약 | `homeSummary` | 신규 생성 |

### portfolio/financial 분리 전략 (리서치 반영)

**핵심 제약**: financial의 모든 상태(`financialOptions`, `financialResult`, `financialLoading`, `selectedStockItem`, `financialChartInstance` 등)가 `portfolio` 객체 안에 중첩되어 있음. 두 컴포넌트에서 각각 `portfolio` 상태를 정의하면 **spread 시 후자가 전자를 덮어씌움**.

**해결**: portfolio.js가 `portfolio` 상태 전체를 소유하고, financial.js는 메서드만 제공:

```javascript
// components/portfolio.js
const PortfolioComponent = {
    portfolio: {
        items: [], allocation: [], loading: false,
        // ... 포트폴리오 + 재무 관련 상태 모두 포함
        financialOptions: null, financialResult: null,
        financialLoading: false, selectedStockItem: null,
        financialChartInstance: null,
        // ...
    },
    // 포트폴리오 메서드만
    async loadPortfolio() { ... },
    async addStockItem(form) { ... },
    // ...
};

// components/financial.js - 메서드만, 상태 없음
const FinancialComponent = {
    // config 데이터 (top-level 프로퍼티로 spread)
    financialColumns: { ... },
    financialSummaryConfig: { ... },
    financialDescriptions: { ... },
    financialMenus: [ ... ],

    // 재무 메서드 (this.portfolio.* 참조)
    async openStockDetail(item) { ... },
    async loadFinancialOptions(stockCode) { ... },
    getFinancialColumns(menu) { ... },
    // ...
};
```

### Script 로딩 순서

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

<!-- Alpine.js (마지막 - 자동 start) -->
<script src="https://cdn.jsdelivr.net/npm/alpinejs@3/dist/cdn.min.js"></script>
```

**주의사항**:
- `async`/`defer` 사용 금지 (의존성 순서 보장을 위해)
- Alpine CDN은 반드시 마지막 (자동 `Alpine.start()` 실행)
- 컴포넌트 간 순서는 무관 (각각 독립적인 객체 리터럴 선언)

### Orphaned 파일 동기화 절차

기존 orphaned 파일(ecos.js, keyword.js, global.js)을 활용하되 **app.js를 source of truth**로 동기화:

1. app.js에서 해당 기능의 메서드/상태를 식별
2. orphaned 파일과 diff 비교
3. 불일치 수정 (keyword.js `KOREA` → `DOMESTIC`, ecos.js에 `getInterestRateSpreads()` 추가 등)
4. 상태 객체를 component에 추가

## Acceptance Criteria

- [ ] app.js에서 각 기능의 상태 + 메서드를 component 파일로 추출
- [ ] app.js는 코어(라우팅, 초기화, 메뉴, 사이드바) + `...XxxComponent` 스프레드만 포함 (~100줄)
- [ ] 기존 orphaned component 파일을 app.js 최신 코드 기준으로 동기화 (diff 검증)
- [ ] financial.js는 메서드 + config만 포함, 상태 없음 (portfolio.js가 전체 portfolio 상태 소유)
- [ ] 각 component의 init 메서드는 `initXxx()` 명명 규칙 준수
- [ ] index.html의 script 태그 순서 업데이트 (모든 component 로드)
- [ ] 모든 기존 기능이 분리 후에도 동일하게 동작 (회귀 없음)
- [ ] 컴포넌트 간 property name 충돌 없음 (교차 검증 완료)

## 작업 리스트

- [x] 1. `components/auth.js` 생성 — auth 상태 + handleOAuthCallback, loadMyProfile, logout, checkLoggedIn
- [x] 2. `components/home.js` 생성 — homeSummary 상태 + loadHomeSummary
- [x] 3. `components/keyword.js` 동기화 — app.js 최신 코드 기준으로 업데이트 (KOREA→DOMESTIC 수정), keywords 상태 포함
- [x] 4. `components/news.js` 생성 — news 상태 + collectNews, selectNewsKeyword, loadNews
- [x] 5. `components/ecos.js` 동기화 — app.js 기준 업데이트, getInterestRateSpreads 등 누락 메서드 추가, ecos 상태 포함
- [x] 6. `components/global.js` 동기화 — app.js 기준 업데이트, globalData 상태 포함
- [x] 7. `components/portfolio.js` 생성 — **portfolio 전체 상태**(financial 관련 포함) + 포트폴리오 CRUD, 주가검색, 자산배분 메서드
- [x] 8. `components/financial.js` 생성 — config 데이터(financialColumns, financialDescriptions, financialSummaryConfig, financialMenus, assetTypeConfig) + 재무제표, 배당, 소송 **메서드만** (상태 없음)
- [x] 9. `components/chat.js` 생성 — chat 상태 + 챗봇 SSE, 드래그, 마크다운 메서드
- [x] 10. property name 교차 검증 — 모든 component의 top-level 프로퍼티명 충돌 없음 확인 완료
- [x] 11. `app.js` 리팩토링 — 코어만 남기고 `...XxxComponent` 스프레드로 통합
- [x] 12. `index.html` script 태그 업데이트 — 모든 component 파일 로드
- [ ] 13. 전체 기능 동작 검증 — 각 페이지 정상 작동, 챗봇, OAuth 흐름 확인

## Dependencies & Risks

| 리스크 | 영향 | 대응 |
|--------|------|------|
| Component 스프레드 시 property name 충돌 | 런타임 에러/데이터 손실 | **작업 10**에서 교차 검증. 각 component 상단에 소유 프로퍼티 주석 명시 |
| portfolio/financial 상태 중첩 충돌 | portfolio 상태 덮어씌움 | financial.js는 메서드만 제공, portfolio.js가 전체 상태 소유 |
| init() 메서드 충돌 | 마지막 spread만 남음 | 각 component는 initXxx()로 명명, app.js의 init()에서 순서대로 호출 |
| Orphaned 파일과 app.js 불일치 | 기능 오작동 | app.js를 기준(source of truth)으로 동기화, diff 비교 절차 |
| Script 로딩 순서 오류 | ReferenceError | `async`/`defer` 사용 금지, 의존성 순서 준수 |

## 발견된 보안 이슈 (기존, 별도 대응 필요)

| 이슈 | 심각도 | 위치 | 설명 |
|------|--------|------|------|
| `marked.parse()` XSS | **중간-높음** | `app.js:2349-2354`, `index.html:2286` | LLM 응답이 `marked.parse()` → `x-html`로 sanitization 없이 렌더링됨. DOMPurify 적용 필요. |
| CDN 스크립트 SRI 미적용 | 중간 | `index.html:7,8,2325,2328` | 4개 CDN 스크립트에 Subresource Integrity hash 없음. 공급망 공격 위험. |

## Sources & References

- **Origin:** [docs/brainstorms/2026-03-26-frontend-code-splitting-brainstorm.md](docs/brainstorms/2026-03-26-frontend-code-splitting-brainstorm.md)
- `src/main/resources/static/js/app.js` — 현재 모놀리식 프론트엔드 (2,383줄)
- `src/main/resources/static/js/components/ecos.js` — orphaned, EcosComponent 스프레드 패턴 참조
- `src/main/resources/static/js/components/keyword.js:30` — `KOREA` 버그 (app.js는 `DOMESTIC`)
- [Alpine.js GitHub Discussion #3025](https://github.com/alpinejs/alpine/discussions/3025) — Spread init() 충돌 이슈
- [Ryan Chandler - Writing Reusable Alpine Components](https://ryangjchandler.co.uk/posts/writing-reusable-alpine-components) — 스프레드 패턴 커뮤니티 사례