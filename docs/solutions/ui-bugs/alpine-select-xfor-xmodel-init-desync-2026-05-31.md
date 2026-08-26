---
title: Alpine select x-model/x-for 초기 바인딩 desync로 인한 표시값/조회값 불일치
date: 2026-05-31
category: ui-bugs
module: portfolio-financial-detail
problem_type: ui_bug
component: frontend_stimulus
symptoms:
  - "KR 재무상세 연도 selectbox가 첫 option(현재연도)을 표시하지만 Alpine 모델(portfolio.financialYear)은 다른 값을 보유"
  - "화면에 보이는 연도와 실제 API 요청 year 파라미터가 불일치 (예: 화면 2026, 요청 year=2025)"
  - "초기 렌더 시점에만 desync, 사용자 change 이벤트는 정상 동작"
root_cause: async_timing
resolution_type: code_fix
severity: medium
tags: [alpine-js, x-model, x-for, select-binding, init-race, nexttick, portfolio, financial-detail]
---

# Alpine select x-model/x-for 초기 바인딩 desync로 인한 표시값/조회값 불일치

## Problem
Alpine `<select x-model>`의 `<option>`을 `<template x-for>`로 생성할 때, selectbox에 보이는 값(첫 option)과 모델 값이 어긋난다. KR 재무상세 연도 드롭다운이 "2026"을 표시하는데 재무 API 요청은 `year=2025`로 나갔다 — 사용자가 보는 값과 조회되는 값이 불일치.

파일: `src/main/resources/static/partials/portfolio-deposit-financial.html`

## Symptoms
- 드롭다운이 모델 값과 무관하게 **첫 option**을 표시
- 네트워크 요청은 화면 표시값이 아닌 **모델 값**으로 전송
- 형제 보고서타입 select(reportCode=ANNUAL)는 정상 — 부모가 `x-show="... && portfolio.financialOptions"`로 비동기 옵션 로드 후에 렌더되어 영향 없음
- `change` 이벤트는 정상, **초기 표시값만** 어긋남

## What Didn't Work
1. **브라우저/JS 캐시 의심** — 서버 재시작 + `Cache-Control: no-store` 확인, 직전 실행 서버가 다른 worktree(`wt-feat-issue-58-reb-adapter-redesign`)의 옛 코드였음도 확인. 전체 새로고침 후에도 증상 지속 → 캐시 원인 아님.
2. **잔존 폴백 로직 의심** — 과거 `shouldFallbackFinancialYear`(현재연도 결과 비면 전년도 무음 재조회)가 원인인지 점검. 제공 JS에 이미 제거됨 확인 → 원인 아님.
3. **모델 write 지점 감사** — `financialYear`를 쓰는 곳은 `openStockDetail()`(getDefaultYear 대입)뿐, 조회는 모델을 정확히 읽음. 즉 모델은 정상이고 **표시값만** 어긋남.

## Solution
`x-model`이 `x-for`가 `<option>`을 렌더하기 전에 평가되어, 일치하는 option이 없는 시점에 브라우저가 첫 option을 표시하고 이후 Alpine이 재동기화하지 않는다. 옵션 렌더 이후 강제 재동기화:

```html
<!-- before -->
<select x-model="portfolio.financialYear">
  <template x-for="year in getYearOptions()" :key="year">
    <option :value="year" x-text="year + '년'"></option>
  </template>
</select>

<!-- after -->
<select x-model="portfolio.financialYear"
        x-init="$nextTick(() => { $el.value = portfolio.financialYear })">
  <template x-for="year in getYearOptions()" :key="year">
    <option :value="year" x-text="year + '년'"></option>
  </template>
</select>
```

`$nextTick`이 x-for 옵션이 DOM에 들어온 뒤로 실행을 미루고, 네이티브 select의 value를 모델 값으로 강제해 표시값 == 모델을 보장한다.

## Why This Works
**초기화 race**다. `x-model`은 init 중 동기적으로 평가되지만 `<template x-for>`는 다음 tick에 `<option>`을 채운다. 바인딩 시점엔 모델과 일치하는 option이 없어 `<select>`가 첫 option을 표시하고, 옵션 생성 후 Alpine은 select 표시값을 재조정하지 않는다. `$nextTick` 내부의 `$el.value = model`은 옵션 렌더 **이후** 실행되어 네이티브 선택 상태를 복원한다. reportCode select는 부모 `x-show` 게이트가 비동기 옵션 도착 전까지 렌더를 막아 x-model이 옵션 존재 이후 바인딩되었기에 우연히 race를 피했다.

## Prevention
- **렌더 후 재동기화**: 옵션이 init 이후 렌더되는(`x-for`/비동기) 모든 Alpine `<select x-model>`에 `x-init="$nextTick(() => $el.value = <model>)"` 추가.
- **렌더 게이트**: select를 옵션 준비 조건(`x-if`/`x-show="options"`)으로 감싸 옵션이 DOM에 있은 뒤 x-model이 바인딩되게 한다.
- **진단 신호**: "모델과 무관하게 첫 option 표시" + "비동기 옵션 select는 정상" 조합이면 이 race다. 콘솔에서 `$el.value`와 Alpine 모델을 비교 — 초기 로드 시 다르고 `change`는 정상이면 확정.

## Related Issues
- [docs/solutions/architecture-patterns/alpine-sortablejs-umd-slot-based-reorder-2026-05-10.md](../architecture-patterns/alpine-sortablejs-umd-slot-based-reorder-2026-05-10.md) — 동일 root-cause 계열(Alpine `x-for` 렌더 타이밍 vs 반응형 상태 race).
- [docs/solutions/ui-bugs/sale-history-hidden-edit-action.md](sale-history-hidden-edit-action.md) — 동일 portfolio/Alpine UI 영역의 형제 버그(같은 파일 이웃).
- [docs/solutions/ui-bugs/responsive-design-tailwind-alpine.md](responsive-design-tailwind-alpine.md) — 앱 전반 Alpine v3 + Tailwind CDN 컨벤션 및 재무 패널 레이아웃.
- 구분 주의: [docs/solutions/logic-errors/gemini-empty-contents-and-financial-year-resolution-2026-04-19.md](../logic-errors/gemini-empty-contents-and-financial-year-resolution-2026-04-19.md) — "financialYear" 용어가 겹치지만 그쪽은 **백엔드 연도 resolution**(DART 미공시 폴백), 본 문서는 **프론트 연도 표시 동기화**로 별개.
