---
title: "Alpine.js x-for + SortableJS UMD — revert-then-splice가 아닌 slot-based 갱신 패턴"
category: architecture-patterns
date: 2026-05-10
module: favorite, frontend
problem_type: best_practice
component: frontend_stimulus
severity: high
tags:
  - alpine-js
  - sortablejs
  - drag-and-drop
  - umd-cdn
  - reactive-array
  - x-for-key
  - revert-then-splice
  - slot-based-reorder
applies_when:
  - "Alpine.js v3 + Tailwind CDN 환경(npm 빌드 파이프라인 없음)에 드래그 앤 드롭 reorder UI를 추가할 때"
  - "한 reactive 배열에 여러 컨테이너(필터링된 view)를 렌더링하면서 한 컨테이너만 reorder해야 할 때"
  - "SortableJS DOM mutation과 Alpine x-for 재렌더가 충돌해 카드가 깜박이거나 잘못된 위치로 splice될 때"
---

# Alpine.js x-for + SortableJS UMD — revert-then-splice가 아닌 slot-based 갱신 패턴

## Context

본 repo는 npm 빌드 파이프라인이 없는 정적 HTML + Tailwind CDN + Alpine.js v3 환경이다. 관심지표 컨테이너에 드래그 앤 드롭 reorder UI를 추가하면서 SortableJS 1.15.x UMD를 CDN script로 도입했다 — 본 repo의 첫 DnD 사용처.

핵심 trap 두 가지가 있다:

1. **Alpine `x-for` reactive 재렌더 vs SortableJS DOM mutation 충돌**: SortableJS는 onEnd 콜백에서 DOM을 *이미 변경*한 상태로 호출하지만, Alpine은 reactive 배열을 source-of-truth로 보고 재렌더한다. 두 흐름이 충돌하면 카드가 깜박이거나 잘못된 위치로 떨어진다. 표준 회피책 "revert-then-splice"는 SortableJS oldIndex/newIndex의 의미와 splice 후 인덱스 보정 수식이 미묘해 dead ternary(`fromAbs < toAbs ? toAbs : toAbs`) 같은 silent bug를 부른다.

2. **단일 reactive 배열 + 다중 컨테이너 렌더**: 같은 `enrichedFavorites.ecos` 배열을 displayMode로 필터링해 두 개의 `<template x-for>`로 렌더링한다. 한 컨테이너만 reorder하더라도 bucket의 절대 인덱스를 직접 splice하면 다른 컨테이너 항목이 의도치 않게 뒤로 밀린다.

## Guidance

### 1. SortableJS UMD 로드 순서

```html
<!-- index.html, Chart.js 다음 / 컴포넌트 스크립트 이전 -->
<script src="https://cdn.tailwindcss.com"></script>
<script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
<script src="https://cdn.jsdelivr.net/npm/sortablejs@1.15.6/Sortable.min.js"></script>
...
<script src="/js/components/favorite.js"></script>
<script src="/js/app.js"></script>
<script src="https://cdn.jsdelivr.net/npm/alpinejs@3/dist/cdn.min.js"></script>
```

Alpine은 마지막에 로드되며 `x-data` 컴포넌트가 마운트된 직후 SortableJS가 사용 가능하도록 컴포넌트 스크립트 이전에 SortableJS를 둔다.

### 2. `:key`는 stable string identifier

```html
<template x-for="card in homeSummary.enrichedFavorites.ecos.filter(c => c.displayMode === 'GRAPH')"
          :key="card.indicatorCode">
  ...
</template>
```

- `:key="index"`는 절대 사용 금지. reorder 시 키가 충돌해 Alpine reconciliation이 노드를 재사용하면서 깜박임 또는 destructive re-render 발생.
- `card.indicatorCode`처럼 **immutable + unique**한 도메인 식별자를 키로 사용하면 SortableJS DOM mutation과 Alpine x-for diff가 무리 없이 동작.

### 3. SortableJS init 옵션 — 모바일 long-press 분리

```javascript
this.favoriteEdit.sortable = Sortable.create(container, {
    animation: 150,
    delay: 200,
    delayOnTouchOnly: true,    // 데스크톱은 즉시, 터치만 long-press
    touchStartThreshold: 5,    // 5px 이내 움직임은 일반 스크롤로 처리
    ghostClass: 'opacity-40',
    onEnd: (evt) => this.handleSortEnd(evt),
});
```

`delay: 200 + delayOnTouchOnly: true`는 모바일에서 일반 페이지 스크롤과 드래그 시작을 명확히 분리한다. 이 설정 없이 터치 디바이스에 SortableJS를 붙이면 사용자가 페이지를 스크롤하려 할 때마다 카드가 잡혀 페이지 스크롤이 깨진다.

### 4. revert-then-splice가 아닌 **slot-based 갱신**

다중 컨테이너 환경에서 절대 인덱스 splice의 인덱스 보정(`fromAbs < toAbs ? toAbs - 1 : toAbs`)은 동일 컨테이너만 있을 때만 동작하며, 다른 displayMode 항목과 인터리브된 bucket에서는 미묘한 off-by-one을 유발한다. 더 안전한 패턴:

```javascript
handleSortEnd(evt) {
    if (evt.oldIndex === evt.newIndex) return;
    const a = this.favoriteEdit.active;
    if (!a) return;

    // (1) SortableJS가 옮긴 DOM을 즉시 되돌린다 — Alpine이 reactive array를 source-of-truth로 다시 그리도록.
    const parent = evt.from;
    const children = Array.from(parent.children);
    if (evt.oldIndex < children.length) {
        parent.insertBefore(
            evt.item,
            children[evt.oldIndex] === evt.item ? children[evt.oldIndex + 1] : children[evt.oldIndex]
        );
    }

    // (2) 컨테이너 카드들의 새 순서 계산 — splice on copy, 인덱스 보정 불필요.
    const enriched = this.homeSummary?.enrichedFavorites;
    if (!enriched) return;
    const bucket = a.sourceType === 'ECOS' ? enriched.ecos : enriched.global;
    const items = this.containerCards(a.sourceType, a.displayMode);
    if (evt.oldIndex >= items.length || evt.newIndex >= items.length) return;
    const reordered = items.slice();
    const [moving] = reordered.splice(evt.oldIndex, 1);
    reordered.splice(evt.newIndex, 0, moving);

    // (3) bucket 내 컨테이너 슬롯 절대 인덱스 수집 후 새 순서로 채워 넣기.
    //     다른 displayMode 항목의 절대 위치는 변경되지 않는다 — 인덱스 보정 불필요.
    const slots = items.map(card => bucket.indexOf(card)).filter(idx => idx >= 0);
    if (slots.length !== items.length) return;
    slots.forEach((slot, i) => { bucket[slot] = reordered[i]; });
    this.favoriteEdit.dirty = true;
},
```

**왜 slot-based가 더 안전한가**:
- (2)에서 *items 배열의 카피* 위에 splice를 적용하므로 bucket 내 다른 항목과 무관하게 컨테이너 순서를 정확히 산출.
- (3)에서 bucket의 컨테이너 슬롯 인덱스(절대 위치)만 수집해 그 슬롯들에 새 순서를 *그대로* 채워 넣는다. 다른 displayMode 항목의 절대 인덱스는 건드리지 않는다 → 인덱스 보정 수식이 필요 없음.
- cancel 시 snapshot 복원도 같은 패턴(`slots.forEach` 슬롯에 snapshot 카드 채워 넣기)으로 일관 처리 가능.

### 5. `x-show + x-transition` 권장, `x-if` 회피

본 repo의 `responsive-design-tailwind-alpine.md` 가이드와 일관 — 편집 모드 토글에서 `x-if`로 DOM을 통째로 추가/제거하면 SortableJS 인스턴스가 detach되어 다음 진입 시 다시 attach 비용이 든다. `x-show`는 DOM을 유지하므로 SortableJS lifecycle 관리가 단순.

## Why This Matters

- **Silent client bug 회피**: dead ternary(`fromAbs < toAbs ? toAbs : toAbs`) 같은 미묘한 인덱스 버그는 컴파일·린트로 잡히지 않고 reorder 시나리오 일부에서만 한 칸 어긋남으로 나타난다. 사용자에게 "내가 옮긴 카드가 한 칸 더 갔다" 같은 모호한 불만으로 환원되어 디버깅이 늦어진다.
- **재사용성**: 본 repo의 다른 화면(포트폴리오, 주식 노트 등)에 reorder UI를 도입할 때 동일 패턴 그대로 적용 가능.
- **모바일 페이지 스크롤 보존**: `delayOnTouchOnly + touchStartThreshold` 조합 미설정 시 모바일 사용성이 즉시 깨진다.

## When to Apply

- Alpine.js v3 환경에 드래그 앤 드롭 reorder UI를 추가할 때
- 한 reactive 배열을 여러 필터로 분기 렌더하면서 한 분기만 reorder해야 할 때
- 모바일 터치 디바이스에서 일반 페이지 스크롤과 드래그 시작을 분리해야 할 때

## Examples

### 함정 시나리오 — revert-then-splice의 dead ternary

```javascript
// 잘못된 패턴
const fromAbs = bucket.findIndex(c => c === moving);
const toAbs = bucket.findIndex(c => c === target);
bucket.splice(fromAbs, 1);
const adjusted = fromAbs < toAbs ? toAbs : toAbs;  // dead ternary
bucket.splice(adjusted, 0, moving);
```

`fromAbs < toAbs`일 때 `toAbs - 1`이 정답이지만 두 분기 모두 `toAbs`를 반환 → 한 칸 어긋남. 다른 displayMode 항목이 fromAbs와 toAbs 사이에 있으면 그 보정이 우연히 정답과 같아져 일부 시나리오에서만 문제 발견.

### 본 작업 적용 사례

- 변경 파일: `src/main/resources/static/js/components/favorite.js`(편집 모드 상태기계 + handleSortEnd + restoreSnapshot 모두 slot-based), `src/main/resources/static/index.html`(SortableJS UMD 추가), `src/main/resources/static/partials/home.html`(편집 진입/저장/취소 버튼)
- containerCards 헬퍼: 같은 `enrichedFavorites.ecos`를 sourceType×displayMode 컨테이너 단위로 필터링해 SortableJS attach 대상 결정
- restoreSnapshot도 동일 slot-based 패턴 — bucket 통째 재구성을 회피해 다른 컨테이너 cosmetic 영향 차단

## Related

- `docs/solutions/ui-bugs/responsive-design-tailwind-alpine.md` — Alpine v3 + Tailwind CDN 컨벤션 (key/x-show/matchMedia)
- `docs/solutions/architecture-patterns/deferred-unique-constraint-retry-requires-new-2026-05-10.md` — 같은 PR의 backend 트랜잭션 격리 패턴 (frontend reorder의 서버 측 페어)
- `docs/plans/2026-05-07-001-feat-watchlist-priority-and-graph-layout-plan.md` — Issue #42 plan, Unit 5 (편집 모드 + SortableJS DnD)
- 외부 참고: alpinejs/alpine#1635 (x-for + SortableJS V3 regression), alpinejs/alpine#3856 (Sortable touch fallback + x-ignore), SortableJS README 1.15.x UMD