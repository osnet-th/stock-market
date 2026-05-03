---
date: 2026-05-03
module: favorite
problem: 관심지표 그래프 토글 버튼 클릭 시 PUT 요청이 발생하지 않음
status: 근본 원인 확정
---

# 관심지표 그래프 토글 버튼 미동작

## 증상

- 대시보드 홈 → 관심 지표 카드의 `그래프 / 지표` 토글 버튼 클릭 시 화면이 전혀 전환되지 않음.
- 브라우저 DevTools Network 탭에서 `PUT /api/favorites/display-mode` 요청 자체가 발생하지 않음.
- 즉, 클라이언트 핸들러 진입 전 단계에서 차단되고 있음.

## 영향 범위

- ECOS 관심 지표 카드: `index.html:426`, `index.html:475` (그래프 ↔ 지표)
- GLOBAL 관심 지표 카드: `index.html:559`, `index.html:613` (그래프 ↔ 지표)
- JS 핸들러: `favorite.js:153` `toggleDisplayMode(card, sourceType)`

## 코드 정합성 점검 (이상 없음)

- `FavoriteComponent` 가 `app.js:42` 의 `dashboard()` 에 `...spread` 로 정상 mixin 됨.
- `API.changeFavoriteDisplayMode` 정의 정상: `api.js:172-174` (`PUT /api/favorites/display-mode`).
- 백엔드 엔드포인트 정상: `FavoriteIndicatorController:93` `@PutMapping("/display-mode")`.
- 서비스 / 리포지토리 / DTO 모두 displayMode 매핑 정상.
- DTO `EnrichedFavoriteResponse.EcosItem.from` / `GlobalItem.from` 가 `displayMode` 를 항상 `"INDICATOR"` 또는 `"GRAPH"` 문자열로 직렬화함.
- 따라서 `c.displayMode !== 'GRAPH'` 필터로 INDICATOR 카드가 정상 렌더링되며, 토글 버튼도 DOM 에 존재해야 함.

## 가능한 근본 원인 후보

핸들러 진입 전 차단되는 시점이므로 후보는 아래로 좁혀짐.

1. **버튼이 `disabled` 상태로 렌더링됨**
   - `:disabled="card._displayModePending"` — `card._displayModePending` 가 어떤 경로로 truthy 가 되어 있으면 클릭이 차단됨.
   - 단, 신규 fetch 카드에는 해당 속성이 없으므로 정상적으로는 undefined → falsy 여야 함.

2. **다른 요소가 버튼 위를 덮고 있음 (히트테스트 가림)**
   - `★ 해제` 버튼: `absolute top-2 right-2 text-lg`
   - `토글` 버튼:   `absolute top-2 right-9`
   - 좌표상 겹치지 않으나, 글꼴 렌더링/줌 비율에 따라 별 글리프(`&#9733;`)가 좌측으로 확장되어 토글 버튼을 덮을 가능성 존재.

3. **클릭 이벤트가 다른 핸들러에 의해 가로채짐 또는 stopPropagation**
   - 카드 자체에 click 핸들러는 없음. 가능성 낮음.

4. **빌드/캐시 mismatch**
   - 최근 머지된 변경(`feat(favorite): 관심지표 표시 모드 토글 프론트엔드 구현 #38`) 직후 브라우저가 구버전 `favorite.js` / `index.html` 을 캐시하여 `toggleDisplayMode` 가 정의되지 않은 상태로 동작 중일 가능성.
   - 증상(요청 자체 미발생, 콘솔 에러 없음 가정)에 가장 부합.

5. **Alpine.js 가 해당 버튼의 `@click` 디렉티브를 인식하지 못함**
   - `index.html` 일부 영역이 다른 컴포넌트의 `x-data` 스코프 안에 들어가 있다면 가능. 현재 구조상 단일 루트 `x-data="dashboard()"` 이므로 가능성 낮음.

## 코드 위치 요약

| 항목 | 파일 | 라인 |
|---|---|---|
| 버튼 (ECOS GRAPH) | `src/main/resources/static/index.html` | 426 |
| 버튼 (ECOS INDICATOR) | `src/main/resources/static/index.html` | 475 |
| 버튼 (GLOBAL GRAPH) | `src/main/resources/static/index.html` | 559 |
| 버튼 (GLOBAL INDICATOR) | `src/main/resources/static/index.html` | 613 |
| 핸들러 | `src/main/resources/static/js/components/favorite.js` | 153 |
| API 호출 | `src/main/resources/static/js/api.js` | 172 |
| 컨트롤러 | `FavoriteIndicatorController.java` | 93 |

## 진단 결과 (확정)

DevTools Console 검증 결과:

1. `Alpine.$data(body).toggleDisplayMode` → `function` (핸들러 정상 바인딩)
2. `card._displayModePending` 속성 자체가 카드 객체에 **존재하지 않음** (key 목록에 없음, 값 `undefined`)
3. 그럼에도 모든 토글 버튼(16/16) 이 `disabled="disabled"` 로 렌더링되어 클릭이 차단됨
4. 사용 중 라이브러리: `alpinejs@3` (CDN 최신, `index.html:5009`)

## 근본 원인

`:disabled="card._displayModePending"` 바인딩이 **`undefined` 값에 대해 disabled 속성을 부착**하고 있음.

- 카드 DTO (`EnrichedFavoriteResponse.EcosItem` / `GlobalItem`) 에는 `_displayModePending` 필드가 없으므로, 서버에서 fetch 한 직후 모든 카드의 `_displayModePending` 은 `undefined`.
- `toggleDisplayMode` 가 한 번 실행되면서 처음으로 `card._displayModePending = true/false` 로 정의되기 전까지는 영영 undefined 상태이고, Alpine 의 boolean attribute 바인딩이 이를 truthy 로 취급해 disabled 가 영구히 박혀 있음.
- 결과: 사용자 입장에서는 클릭이 전혀 반응하지 않음 → PUT 요청 자체가 발생하지 않음.

## 영향 범위 (4개소)

| 위치 | 파일:라인 |
|---|---|
| ECOS GRAPH 카드 | `index.html:427` |
| ECOS INDICATOR 카드 | `index.html:476` |
| GLOBAL GRAPH 카드 | `index.html:560` |
| GLOBAL INDICATOR 카드 | `index.html:614` |

## 해결 방향 후보

다음 설계 문서에서 결정할 후보:

- (안1) 바인딩에 boolean coercion: `:disabled="!!card._displayModePending"`
- (안2) 바인딩 명시 비교: `:disabled="card._displayModePending === true"`
- (안3) fetch 후 클라이언트에서 모든 카드에 `_displayModePending = false` 사전 초기화

설계 문서: `.claude/designs/favorite/graph-toggle-disabled-fix/graph-toggle-disabled-fix.md`