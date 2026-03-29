---
title: "feat: 반응형 웹 디자인 (태블릿 + 모바일)"
type: feat
status: completed
date: 2026-03-29
origin: docs/brainstorms/2026-03-29-responsive-web-design-brainstorm.md
---

# feat: 반응형 웹 디자인 (태블릿 + 모바일)

## Enhancement Summary

**Deepened on:** 2026-03-29
**Research agents used:** Best Practices, Framework Docs, Performance Oracle, Security Sentinel, Frontend Race Conditions, Code Simplicity, Architecture Strategist (7개)

### Key Improvements (심화 리서치 반영)
1. **Phase 6→3 통합** — YAGNI 원칙 적용, 불필요한 복잡도 제거
2. **테이블: `overflow-x-auto` 유지** — 카드형 전환 대신 가로 스크롤 (HTML 중복 제거)
3. **포트폴리오 액션 버튼: `flex-wrap` 사용** — ⋮ 드롭다운 대신 단순 줄바꿈
4. **`matchMedia` 전용** — `resize` 이벤트 제거, threshold 교차 시에만 이벤트 발생
5. **스크롤 잠금 참조 카운터** — 다중 오버레이 동시 열림 시 잠금 해제 버그 방지
6. **`navigateTo` 동시 호출 가드** — 더블 탭 방지
7. **`@click.away` → `@click.outside`** — Alpine.js v3 정식 API 사용
8. **`100vh` → `100dvh`** — iOS Safari 주소창 문제 해결

### Simplification Decisions (YAGNI 적용)
- ~~safe-area-inset 시스템~~ → 필요 시 개별 요소에 추가
- ~~툴팁 모바일 변환~~ → hover 없으면 안 보이는 것이 자연스러움
- ~~5단계 z-index 추상화~~ → 3단계로 단순화
- ~~custom.css media query~~ → Tailwind 유틸리티 클래스만 사용
- ~~테이블→카드 HTML 이중 렌더링~~ → overflow-x-auto로 충분

---

## Overview

데스크탑 위주로 설계된 Stock Market Dashboard를 태블릿(768~1024px)과 모바일(<768px) 브라우저에서도 모든 기능이 정상 동작하도록 반응형 웹 디자인을 적용한다.

현재 상태: Tailwind CSS + Alpine.js 기반 SPA. viewport 메타태그는 있으나, 사이드바/모달/슬라이드 패널 등에 고정 너비가 적용되어 있어 모바일에서 레이아웃이 깨짐. 기존 그리드 카드에만 `md:grid-cols-*` 반응형이 부분 적용됨.

## Problem Statement / Motivation

- 이슈 #16: "핸드폰 브라우저에서도 볼 수 있도록 반영 필요"
- 외출 시에도 포트폴리오 조회, 키워드 등록, 경제지표 확인 등 모든 기능을 모바일에서 사용하고자 함
- 현재 고정 사이드바(`w-56`), 고정 너비 모달(`w-[480px]`), 슬라이드 패널(`w-[65%]`) 등이 모바일에서 사용 불가

## Proposed Solution

Tailwind CSS 반응형 유틸리티 클래스(`sm:`, `md:`, `lg:`)를 기존 HTML에 추가하는 방식. 빌드 도구 변경 없이 CDN 환경 유지. **custom.css에 media query를 추가하지 않고** Tailwind 유틸리티만으로 모든 반응형을 처리한다.

(see brainstorm: docs/brainstorms/2026-03-29-responsive-web-design-brainstorm.md)

## Key Decisions

| 결정 항목 | 선택 | 근거 |
|-----------|------|------|
| 브레이크포인트 | Tailwind 기본값 (sm:640, md:768, lg:1024) | 표준 + CDN 지원 |
| 드로어 전환 기준 | `lg:1024px` 미만은 모두 드로어 | 태블릿 세로 모드에서도 넓은 콘텐츠 영역 확보 |
| 사이드바 (모바일/태블릿) | 햄버거 메뉴 + 오버레이 드로어 | 일반적 모바일 네비게이션 패턴 |
| 챗봇 (모바일) | 전체 화면 모달 | 좁은 화면에서 대화 UX 최적화 |
| 데이터 테이블 (모바일) | **가로 스크롤 유지** (`overflow-x-auto`) | HTML 중복 제거, DRY 원칙 (Architecture/Simplicity 리뷰 반영) |
| 포트폴리오 액션 버튼 | **`flex-wrap` 줄바꿈** | ⋮ 드롭다운보다 단순, 버튼이 자연스럽게 다음 줄로 이동 (Simplicity 리뷰 반영) |
| 상태 관리 | `isMobile` + `mobileDrawerOpen` (app.js) | `matchMedia` 전용, `sidebarCollapsed`와 분리 |
| 스크롤 잠금 | **참조 카운터 패턴** | 다중 오버레이 동시 열림 대응 (Race Condition 리뷰 반영) |
| CSS 전략 | **Tailwind 유틸리티 전용** (custom.css에 media query 추가 안 함) | 두 시스템 혼용 방지 (Architecture 리뷰 반영) |
| viewport 단위 | `dvh`/`svh` 사용 | iOS Safari 주소창 문제 해결, Tailwind CDN에서 지원 |

## Breakpoint별 레이아웃 매핑

| 요소 | 모바일 (<640px) | 태블릿 세로 (640~1023px) | 데스크탑 (1024px+) |
|------|-----------------|--------------------------|---------------------|
| **헤더** | 햄버거 버튼 + 로고, px-3 | 햄버거 버튼 + 로고 + 사용자명, px-4 | 현재 유지 (로고 + 사용자 정보, px-6) |
| **사이드바** | 숨김 → 오버레이 드로어 | 숨김 → 오버레이 드로어 | 고정 사이드바 (w-56/w-14) |
| **메인 콘텐츠** | ml-0, p-3 | ml-0, p-4 | ml-56/ml-14, p-6 |
| **요약 카드** | grid-cols-1 | sm:grid-cols-2 | lg:grid-cols-4 |
| **데이터 테이블** | 가로 스크롤 (overflow-x-auto) | 가로 스크롤 | 테이블 |
| **포트폴리오 항목** | 세로 스택 + flex-wrap 버튼 | 가로 배치 + flex-wrap | 가로 배치 + 버튼 나열 |
| **모달** | w-full mx-3 | w-full max-w-[480px] mx-4 | 현재 유지 |
| **재무 패널** | w-full | w-full | w-[65%] |
| **챗봇** | 전체 화면 모달 | 전체 화면 모달 | 플로팅 패널 (w-96/w-[600px]) |

## Technical Considerations

### 상태 관리 변경 (app.js)

**`matchMedia` 전용 — `resize` 이벤트 사용하지 않음** (Performance/Race Condition 리뷰 반영)

```javascript
// app.js init()에 추가
isMobile: false,
mobileDrawerOpen: false,
_scrollLockCount: 0,
_navigating: false,

init() {
    // matchMedia는 threshold 교차 시에만 이벤트 발생 (resize는 매 픽셀마다 발생)
    const mql = window.matchMedia('(max-width: 1023px)');
    const handleChange = (e) => {
        this.isMobile = e.matches;
        if (!e.matches) {
            this.mobileDrawerOpen = false; // 데스크탑 전환 시 드로어 닫기
        }
    };
    mql.addEventListener('change', handleChange);
    this.isMobile = mql.matches;

    // 기존 sidebarCollapsed 로직 유지 (데스크탑 전용)
    // ...
}
```

**computed 헬퍼:**
```javascript
get effectiveSidebarVisible() {
    if (this.isMobile) return this.mobileDrawerOpen;
    return true; // 데스크탑: 항상 표시 (접힌/펼친 상태만 다름)
}
```

### 스크롤 잠금 참조 카운터 (Race Condition 리뷰 반영)

다중 오버레이(드로어 + 모달 + 재무패널)가 동시에 열릴 때, 하나만 닫아도 스크롤 잠금이 풀리는 버그 방지:

```javascript
lockScroll() {
    this._scrollLockCount++;
    if (this._scrollLockCount === 1) {
        document.body.style.overflow = 'hidden';
    }
},
unlockScroll() {
    this._scrollLockCount = Math.max(0, this._scrollLockCount - 1);
    if (this._scrollLockCount === 0) {
        document.body.style.overflow = '';
    }
}
```

### navigateTo 동시 호출 가드 (Race Condition 리뷰 반영)

모바일에서 드로어 메뉴 항목을 빠르게 두 번 탭하면 두 개의 비동기 데이터 로딩이 경쟁:

```javascript
async navigateTo(page) {
    if (this._navigating) return;
    this._navigating = true;
    this.mobileDrawerOpen = false; // 즉시 닫기 (애니메이션 없이)
    try {
        this.currentPage = page;
        // ... 기존 데이터 로딩 로직 ...
    } finally {
        this._navigating = false;
    }
}
```

### 드로어 토글 디바운싱 (Race Condition 리뷰 반영)

```javascript
_drawerTransitioning: false,
toggleMobileDrawer() {
    if (this._drawerTransitioning) return;
    this._drawerTransitioning = true;
    this.mobileDrawerOpen = !this.mobileDrawerOpen;
    if (this.mobileDrawerOpen) this.lockScroll();
    else this.unlockScroll();
    setTimeout(() => { this._drawerTransitioning = false; }, 200);
}
```

### z-index 계층 (단순화)

```
z-40: 드로어 백드롭 + 드로어 패널
z-50: 모달, 재무 슬라이드 패널
z-[60]: 챗봇 전체화면 (모바일)
```

### Alpine.js v3 주의사항 (Framework Docs 리뷰 반영)

- **`@click.away` → `@click.outside`**: Alpine.js v3에서 `.away`는 deprecated. `.outside` 사용
- **`x-transition`은 `x-show`에서만 작동**: `x-if`와 함께 사용 불가. 드로어 애니메이션에 `x-show` + `x-transition` 사용
- **`100vh` → `100dvh`**: `min-h-[calc(100vh-3.5rem)]` → `min-h-[calc(100dvh-3.5rem)]`로 변경 (iOS Safari 대응)

## System-Wide Impact

- **파일 변경 범위**: index.html (전체 레이아웃), app.js (상태 관리), chat.js (드래그 조건부)
- **custom.css 변경 없음**: Tailwind 유틸리티 클래스만 사용 (Architecture 리뷰 반영)
- **기존 기능 영향**: 데스크탑 레이아웃은 변경 없음 (lg: 이상에서 기존 클래스 유지)
- **성능**: Tailwind CDN 자체는 ~300KB JS 런타임이지만 기존 결정이므로 범위 밖. 후속 과제로 Tailwind CLI 정적 빌드 검토 가능

## Acceptance Criteria

- [ ] 모바일(375px)에서 모든 페이지가 가로 스크롤 없이 표시됨
- [ ] 태블릿 세로(768px)에서 모든 페이지가 정상 표시됨
- [ ] 햄버거 메뉴로 사이드바 드로어 열림/닫힘 동작
- [ ] 드로어 열림 시 배경 스크롤 잠금, 다중 오버레이에서도 올바르게 동작
- [ ] 모든 모달이 모바일 화면 내에서 표시됨 (오버플로우 없음)
- [ ] 재무 슬라이드 패널이 모바일에서 전체 너비로 표시됨
- [ ] 챗봇이 모바일에서 전체 화면 모달로 표시됨
- [ ] 포트폴리오 액션 버튼이 모바일에서 자연스럽게 줄바꿈됨
- [ ] 데이터 테이블이 모바일에서 가로 스크롤로 사용 가능
- [ ] 데스크탑(1024px 이상) 레이아웃이 기존과 동일하게 유지됨
- [ ] viewport 크기 변경 시 레이아웃 자동 전환 (matchMedia 기반)
- [ ] 드로어 메뉴에서 빠른 더블 탭 시에도 정상 동작

## Implementation Phases

### Phase 1: 코어 레이아웃 + 모달 반응형

사이드바 드로어, 헤더 햄버거, 메인 콘텐츠 margin, 모달 너비를 한 번에 전환. 이것만으로 모바일에서 앱이 "사용 가능"해짐.

**작업 목록:**

- [x] **app.js — 반응형 상태 관리 추가**
  - `isMobile`: `window.matchMedia('(max-width: 1023px)')` 기반 (`resize` 이벤트 사용 안 함)
  - `mobileDrawerOpen` 상태 추가
  - `toggleMobileDrawer()`: 디바운싱 포함
  - `lockScroll()` / `unlockScroll()`: 참조 카운터 패턴
  - `navigateTo()`: `_navigating` 가드 추가 + 드로어 즉시 닫기
  - `effectiveSidebarVisible` computed 프로퍼티

- [x] **index.html — viewport 메타 태그 업데이트**
  - `viewport-fit=cover` 추가
  - `min-h-[calc(100vh-3.5rem)]` → `min-h-[calc(100dvh-3.5rem)]` 변경

- [x] **index.html — 헤더 반응형**
  - 햄버거 버튼 추가 (`lg:hidden`으로 데스크탑에서 숨김)
  - 패딩: `px-3 lg:px-6`
  - 사용자 표시명: `hidden sm:inline`

- [x] **index.html — 사이드바를 오버레이 드로어로 전환**
  - `lg:` 이상: 기존 고정 사이드바 유지
  - `lg:` 미만: `x-show="mobileDrawerOpen"` + `x-transition` 슬라이드
  - 백드롭: `bg-black/50`, `@click="toggleMobileDrawer()"`, `x-transition.opacity`
  - 드로어: `-translate-x-full` ↔ `translate-x-0` 애니메이션
  - `@keydown.escape.window="mobileDrawerOpen && toggleMobileDrawer()"`
  - `overscroll-contain` 클래스로 드로어 내부 스크롤 격리

- [x] **index.html — 메인 콘텐츠 영역 반응형**
  - margin: `ml-0 lg:ml-56` (또는 `lg:ml-14` when collapsed)
  - padding: `p-3 sm:p-4 lg:p-6`

- [x] **index.html — 모든 모달 반응형** (키워드:~293, 자산추가:~1248, 자산수정:~1566, 삭제:~1795)
  - `w-96` / `w-[480px]` / `w-[400px]` → `w-[calc(100vw-1.5rem)] sm:w-96` / `sm:w-[480px]` / `sm:w-[400px]`
  - `max-h-[80vh]` → `max-h-[80dvh]`

- [x] **index.html — 재무 슬라이드 패널 반응형** (~1987)
  - `w-[65%]` → `w-full lg:w-[65%]`

### Phase 2: 포트폴리오 + 챗봇 반응형

포트폴리오 항목 레이아웃과 챗봇 패널 모바일 최적화.

**작업 목록:**

- [x] **index.html — 포트폴리오 항목 카드 레이아웃 반응형**
  - 데스크탑: 기존 `flex items-center justify-between` 유지
  - 모바일: `flex-col` 세로 스택

- [x] **index.html — 포트폴리오 액션 버튼 `flex-wrap` 적용**
  - `flex items-center gap-2` → `flex flex-wrap items-center gap-2`
  - 모바일에서 버튼이 자연스럽게 다음 줄로 이동
  - 삭제 버튼 터치 영역 충분히 확보 (min-h-[44px])

- [x] **index.html — 포트폴리오 헤더 영역 반응형**
  - 알림 토글 + 추가 버튼: `flex-wrap` 또는 `flex-col sm:flex-row`

- [x] **index.html — 요약 카드 그리드 중간 단계 추가**
  - `grid-cols-1 md:grid-cols-4` → `grid-cols-1 sm:grid-cols-2 lg:grid-cols-4`

- [x] **index.html — 챗봇 패널 반응형**
  - `lg:` 이상: 기존 플로팅 패널 유지
  - `lg:` 미만: `fixed inset-0 w-full h-dvh` 전체 화면 모달
  - 전체 화면 시 확장/축소 버튼 숨김 (`lg:inline hidden`)
  - 닫기 버튼 터치 영역 확대 (min-w-[44px] min-h-[44px])

- [x] **chat.js — 모바일 드래그 비활성화**
  - `isMobile` 확인하여 드래그 이벤트 조건부 바인딩
  - `isMobile` 전환 시 진행 중인 드래그 강제 취소

### Phase 3: 테이블 + 마무리

데이터 테이블 가로 스크롤 확인 및 터치 타겟 최적화.

**작업 목록:**

- [x] **index.html — 데이터 테이블 가로 스크롤 확인**
  - ECOS/글로벌/재무 테이블에 `overflow-x-auto` 래퍼 확인
  - 이미 적용되어 있으면 변경 없음

- [x] **index.html — 터치 타겟 최소 크기 확인**
  - 주요 클릭 가능 요소에 `min-h-[44px]` 확보
  - 특히: 드로어 메뉴 항목, 모달 버튼, 포트폴리오 액션 버튼

- [x] **index.html — 키워드 필터 버튼 모바일 처리**
  - `overflow-x-auto` + `flex-nowrap` 확인 (이미 적용 시 변경 없음)

## Dependencies & Risks

| 리스크 | 영향 | 대응 |
|--------|------|------|
| index.html이 2400줄로 변경량이 많음 | 머지 충돌 가능성 | 다른 기능 브랜치와 병행 주의 |
| iOS Safari `overflow:hidden`이 스크롤 차단 못 할 수 있음 | 드로어 뒤에서 페이지 스크롤 | `overscroll-contain` + 필요 시 `position:fixed` 패턴으로 전환 |
| matchMedia 전환 시 드래그 중이면 mouseup 안 됨 | 챗봇 패널 드래그 상태 고착 | isMobile 전환 시 드래그 강제 취소 코드 추가 |
| 현재 feature/portfolio-email-notification 브랜치에서 작업 중 | 브랜치 충돌 | 새 브랜치 생성 전 현재 작업 정리 필요 |

### Performance Notes (Performance 리뷰 반영)

- **Chart.js 전환 중 리사이즈**: 재무 패널 open/close 애니메이션 중 Chart.js가 매 프레임 리드로우. `transitionend` 이벤트 후 1회 resize 호출로 최적화 가능
- **Tailwind CDN 성능**: 런타임 ~300KB JS. 후속 과제로 Tailwind CLI 정적 빌드 검토 (빌드 도구 없이 단독 바이너리 사용 가능)

### Security Notes (Security 리뷰 반영)

반응형 변경 자체는 보안 위험 없음. 별도 이슈로 다룰 기존 취약점:
- `X-Frame-Options` 헤더 누락 (ProdSecurityConfig.java)
- `marked.parse()` + `x-html` XSS 가능성 (DOMPurify 적용 필요)

## Sources & References

### Origin

- **Brainstorm document:** [docs/brainstorms/2026-03-29-responsive-web-design-brainstorm.md](docs/brainstorms/2026-03-29-responsive-web-design-brainstorm.md)
- Key decisions carried forward: 드로어 패턴, 전체화면 챗봇, Tailwind 유틸리티 방식
- Simplification: 카드형 테이블 → overflow-x-auto, ⋮ 드롭다운 → flex-wrap

### Internal References

- Layout structure: `src/main/resources/static/index.html` (헤더:14-35, 사이드바:39-82, 메인:85-86)
- Sidebar state: `src/main/resources/static/js/app.js:15`
- Chat drag: `src/main/resources/static/js/components/chat.js:121-148`
- Modals: index.html (키워드:293, 자산추가:1248, 자산수정:1566, 삭제:1795, 매수:1828)
- Slide panel: index.html:1987-2249
- Chat panel: index.html:2251-2412
- Custom CSS: `src/main/resources/static/css/custom.css` (136줄, 변경 없음)

### External References

- [Tailwind CSS v3 Responsive Design](https://v3.tailwindcss.com/docs/responsive-design)
- [Alpine.js x-transition](https://alpinejs.dev/directives/transition)
- [Alpine.js @click.outside](https://alpinejs.dev/directives/on#outside)
- [CSS dvh/svh viewport units](https://caniuse.com/viewport-unit-variants)
- [matchMedia API](https://developer.mozilla.org/en-US/docs/Web/API/Window/matchMedia)

### Related

- Issue: #16 (반응형 웹 디자인)