---
title: "Tailwind CSS + Alpine.js SPA 반응형 디자인 구현"
category: ui-bugs
date: 2026-03-29
tags:
  - tailwind-css
  - alpine-js
  - responsive-design
  - mobile-ui
  - matchMedia
  - dvh
module: frontend
symptom: "데스크탑 전용 고정 너비 레이아웃이 모바일/태블릿에서 사용 불가"
root_cause: "고정 픽셀 너비(w-56, w-[480px], w-[65%], w-96)와 viewport 인식 상태 관리 부재"
---

# Tailwind CSS + Alpine.js SPA 반응형 디자인 구현

## Problem

Stock Market Dashboard SPA (Tailwind CSS CDN + Alpine.js v3, 단일 index.html ~2500줄)가 데스크탑 전용으로 설계되어 모바일/태블릿에서 레이아웃이 깨짐.

- 사이드바: 고정 `w-56` — 모바일에서 콘텐츠를 밀어냄
- 모달: 고정 `w-[480px]` — 모바일 화면 넘침
- 재무 패널: `w-[65%]` — 375px 화면에서 244px로 사용 불가
- 챗봇: `w-96` (384px) — 모바일 뷰포트 초과
- 메인 콘텐츠: `ml-56` 고정 마진

## Root Cause

고정 픽셀/퍼센트 너비와 viewport 인식 JavaScript 상태 관리 부재. `sidebarCollapsed`만 localStorage에 저장하고, 화면 크기에 따른 자동 전환 로직 없음.

## Solution

### 1. matchMedia 기반 viewport 상태 (resize 대신)

```javascript
// app.js — matchMedia는 breakpoint 교차 시에만 이벤트 발생 (resize는 매 픽셀)
isMobile: false,
mobileDrawerOpen: false,
_mqlCleanup: null,

async init() {
    if (this._mqlCleanup) return; // 중복 초기화 방지

    const mql = window.matchMedia('(max-width: 1023px)');
    const handleChange = (e) => {
        this.isMobile = e.matches;
        if (!e.matches) this.closeMobileDrawer();
    };
    mql.addEventListener('change', handleChange);
    this._mqlCleanup = () => mql.removeEventListener('change', handleChange);
    this.isMobile = mql.matches;
}
```

### 2. 모바일 드로어 (x-show + x-transition)

```html
<!-- 백드롭 -->
<div x-show="mobileDrawerOpen && isMobile"
     x-transition:enter="transition-opacity ease-out duration-200"
     x-transition:enter-start="opacity-0"
     x-transition:enter-end="opacity-100"
     @click="closeMobileDrawer()"
     class="fixed inset-0 z-40 bg-black/50 lg:hidden"></div>

<!-- 드로어 패널 -->
<aside x-show="mobileDrawerOpen"
       x-transition:enter="transition-transform ease-out duration-200"
       x-transition:enter-start="-translate-x-full"
       x-transition:enter-end="translate-x-0"
       @keydown.escape.window="closeMobileDrawer()"
       class="fixed inset-y-0 left-0 z-50 w-64 bg-white shadow-xl overflow-y-auto overscroll-contain lg:hidden">
```

### 3. 스크롤 잠금 (단순 토글)

```javascript
// 단일 소비자에는 참조 카운터 불필요 — 직접 토글
toggleMobileDrawer() {
    this.mobileDrawerOpen = !this.mobileDrawerOpen;
    document.body.style.overflow = this.mobileDrawerOpen ? 'hidden' : '';
},
closeMobileDrawer() {
    if (this.mobileDrawerOpen) {
        this.mobileDrawerOpen = false;
        document.body.style.overflow = '';
    }
}
```

### 4. 모달 반응형 너비

```html
<!-- 고정 w-[480px] → 모바일 전체 너비 + 데스크탑 고정 -->
<div class="w-[calc(100vw-1.5rem)] sm:w-[480px] max-h-[80dvh] overflow-y-auto">
```

### 5. 챗봇 클래스 메서드 추출

```javascript
// chat.js — 중첩 삼항 대신 메서드로 추출
chatPanelClasses() {
    const base = 'bg-white shadow-xl border border-gray-200 flex flex-col transition-all duration-200';
    if (this.isMobile) {
        return `fixed inset-0 w-full h-dvh rounded-none z-[60] ${base}`;
    }
    const size = this.chat.expanded ? 'w-[600px] h-[700px]' : 'w-96 h-[500px]';
    const position = this.chat.dragPos.x !== null ? 'fixed' : 'fixed bottom-24 right-6';
    return `${size} ${position} rounded-xl z-50 ${base}`;
}
```

## Key Patterns

| 패턴 | 사용 | 피하기 |
|------|------|--------|
| Breakpoint 감지 | `matchMedia` (교차 시 1회) | `resize` (매 픽셀 발생) |
| 드로어 애니메이션 | `x-show` + `x-transition` | `x-if` (transition 미지원) |
| 외부 클릭 닫기 | `@click.outside` (v3) | `@click.away` (deprecated) |
| Viewport 높이 | `100dvh` (iOS Safari 대응) | `100vh` (주소창 포함) |
| 반응형 CSS | Tailwind 유틸리티 전용 | custom.css media query 혼용 |
| 스크롤 잠금 | 직접 overflow 토글 (단일 소비자) | 참조 카운터 (과잉 설계) |

## Prevention

1. **YAGNI 적용**: 모든 boolean 플래그/카운터에 대해 "어떤 구체적 버그를 방지하는가?" 질문. 답할 수 없으면 제거
2. **프레임워크 우선**: Alpine.js `x-transition`이 애니메이션 상태를 관리 — 외부 setTimeout 디바운싱 불필요
3. **HTML 중복 최소화**: 모바일/데스크탑 nav 중복 시 동기화 주석 (`<!-- NAV_ITEMS: 동기화 필요 -->`) 필수
4. **실제 디바이스 테스트**: DevTools 에뮬레이션만으로는 iOS Safari dvh, 가상 키보드 등 미검증

## Related

- Issue: #16 (반응형 웹 디자인)
- Brainstorm: `docs/brainstorms/2026-03-29-responsive-web-design-brainstorm.md`
- Plan: `docs/plans/2026-03-29-001-feat-responsive-web-design-plan.md`
- Frontend architecture: `.serena/memories/frontend/architecture-exploration.md`