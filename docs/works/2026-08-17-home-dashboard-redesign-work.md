# 홈 대시보드 경제 대시보드형 리디자인 Work (#114)

gate: docs/gates/2026-08-17-home-dashboard-redesign-gates.md
plan: docs/plans/2026-08-17-001-feat-home-dashboard-redesign-plan.md

---

## Phase 1 — 레이아웃 셸 · 헤더 · 브리핑 (완료, 2026-08-17)

### 변경 파일

| 파일 | 변경 |
|---|---|
| `static/partials/home.html` | 제목 → `오늘의 시장` 헤더(기준시각·기간·정규화 모드·포트폴리오 카드)로 교체. 지표 영역 301줄을 분리해 496 → 256줄 |
| `static/partials/home-indicators.html` | **신규 306줄** — 기존 국내·글로벌 지표 마크업을 그대로 이관(Phase 2~3에서 재구성) |
| `static/partials/home-side.html` | **신규 3줄** — 우측 패널 빈 셸(Phase 4에서 채움) |
| `static/index.html` | home 본문 2단 그리드 래퍼 + host 2종 추가 |
| `static/js/app.js` | `partialNames` 에 `home-indicators` · `home-side` 추가 |
| `static/js/components/home.js` | `homeDashboard` 상태 + 헬퍼 10종 추가 |

### 실행 중 확정된 사항

**1) 2단 그리드 래퍼를 `index.html` 에 둔 이유**

처음에는 `home.html` 안에 `data-partial` host 를 중첩하려 했으나 동작하지 않는다.
`PartialLoader.mountAllPartials()` 가 `names.map()` 시점에 **모든 host 를 한 번에 `querySelector`** 하는데,
이때 `home.html` 은 아직 마운트 전이라 내부 host 가 DOM 에 없다 → `placeholder missing` 으로 스킵된다.

→ 그리드 래퍼와 host 2종을 `index.html` 에 두고, 각 host 를 grid item 으로 만들었다.
`portfolio` 셸 + 탭 partial 이 형제로 놓인 #110 구조와 같은 방식이다.

```html
<div data-partial="home"></div>
<div x-show="currentPage === 'home'" x-cloak
     class="grid grid-cols-1 xl:grid-cols-[minmax(0,1fr)_320px] gap-4 items-start">
    <div data-partial="home-indicators" class="min-w-0"></div>
    <div data-partial="home-side" class="min-w-0"></div>
</div>
```

**2) 브리핑 소수 자릿수는 원본 값을 따라간다**

지표마다 정밀도가 달라 일괄 고정하면 기준금리(2.75)와 KORIBOR(2.984)가 뭉개진다.
`_homeDeltaDecimals()` 가 원본 문자열의 소수부 길이를 읽어 최대 3자리로 맞춘다.

**3) 포트폴리오 요약 카드는 조건부 노출**

목업에 "목표 배분 미설정" 상태가 없다. `hasHomePortfolioSummary()` 로
`configured && totalEvaluated > 0` 일 때만 카드를 띄우고, 아니면 숨긴다(사이드바 메뉴로 유도).

**4) 기존 섹션은 유지**

기능 요약 3카드·요약 카드 4장·최근 업데이트 박스는 **Phase 5 에서 제거**한다.
Phase 1 에서 지우면 Phase 2~4 동안 홈이 반쪽이 되므로 그대로 뒀다.

**5) `formatKrwCompact` 는 `PortfolioComponent` 것을 재사용**

`app.js` 가 전 컴포넌트를 한 객체로 spread 하므로 `this.formatKrwCompact()` 로 접근된다(#110 에서 추가된 헬퍼).
홈 전용으로 중복 정의하지 않았다.

### 검증 (목 하네스)

`static/` 전체를 스크래치패드에 복사 + `harness-mock.js` 주입(정적 서버 :8897).
`auth.js` 가 **파싱 시점에** `localStorage` 를 읽으므로 토큰 시드는 `api.js` 앞 인라인 스크립트로 넣어야 했다.
목이 없는 엔드포인트가 정적 서버로 새지 않도록 `API.request` catch-all 도 두었다.

| # | 항목 | 결과 |
|---|---|---|
| 1 | partial 3종 마운트 | PASS (home · home-indicators · home-side 모두 children > 0) |
| 2 | 헤더 · 기준시각 | PASS (`2026.08.17 (월) 16:19 기준`) |
| 3 | 브리핑 3종 | PASS (`+0.02` / `+0.00` / `+7.43` — 원본 정밀도 유지) |
| 4 | 기간 버튼 | PASS (`6M` → `1Y` 전환, `getHomeRangePoints()` 252) |
| 5 | 정규화 모드 토글 | PASS (`변화율(%) 기준` ↔ `자체 스케일 기준`) |
| 6 | 포트폴리오 카드 | PASS (9,395만원 · 배분 밴드 초과 · 안전 77% · 투자 23%) |
| 7 | 2단 그리드 (1600px) | PASS (986px + 320px, 같은 행) |
| 8 | 1단 전환 (375px) | PASS (세로 스택, 가로 스크롤 없음) |
| 9 | 빈 상태 — 관심 지표 0개 | PASS (브리핑 0건 → 줄 숨김) |
| 10 | 빈 상태 — `enrichedFavorites` null | PASS (브리핑 0건) |
| 11 | 빈 상태 — 목표 배분 미설정 | PASS (카드 숨김) |
| 12 | 빈 상태 — `allocationStatus` null | PASS (카드 숨김) |

콘솔 잔여 경고는 하네스 산물이다 — `realestate.tab.nextScheduledAt` 은 목이 없는 부동산 API 가
`null` 을 반환해서 나는 것으로 이번 변경과 무관하다(실서버에서는 데이터가 온다).

### 미검증

- 실서버 화면 — 목 하네스까지만 확인
- `getHomeAsOfLabel()` 은 브라우저 현재 시각 기준이다. 목업의 "08:45 기준"처럼 **데이터 기준시각**을 쓰려면
  지표 응답의 최신 스냅샷 시각을 써야 한다 → Phase 3 에서 카드 기준일을 다루면서 재검토

---

## Phase 2 — 지표 비교 보기 (완료, 2026-08-17)

### 변경 파일

| 파일 | 변경 |
|---|---|
| `static/js/components/home.js` | 비교 상태·헬퍼 9종 + Chart.js 렌더 (`getHomeCompareCandidates` · `toggleHomeCompare` · `getHomeCompareSeries` · `normalizeHomeSeries` · `renderHomeCompareChart` · `destroyHomeCompareChart` 등) |
| `static/partials/home-indicators.html` | 비교 보기 카드(칩 + canvas + 빈 상태) 신규, 기존 카드 4블록에 `비교` 토글 버튼 추가 |
| `static/js/app.js` | `cleanupRegistry` 에 `home-indicators` 추가 — 리마운트 시 비교 차트 destroy |

### 실행 중 확정된 사항

**1) 주기가 다른 지표는 전 구간으로 편다 (목업 방식)**

일별 지표(126포인트)와 월별 지표(60포인트)를 Chart.js 카테고리 축에 그냥 얹으면
짧은 쪽이 **왼쪽 60칸에만 그려져** 시점이 어긋난다. 최신값이 차트 중간에 찍히는 셈이다.

목업은 각 시리즈를 `px(i, d.vals.length)` 로 **자기 길이 기준 전 폭에 펴서** 그린다.
같은 방식으로 `stretch()` 를 넣어 모든 시리즈를 가장 긴 길이에 맞춰 리샘플링했다.

대신 공유 x축 라벨이 짧은 지표에는 거짓이 되므로, **툴팁 제목을 없애고 각 줄에 자기 날짜를 붙였다.**

**2) 기준값이 0이면 변화율이 발산한다**

`pct` 모드는 `((v - base) / |base|) * 100` 인데 `base === 0` 이면 Infinity 가 된다.
이 경우만 `raw` 정규화로 떨어뜨린다. 실측으로 `[0, 5, -5]` → `[0, 50, -50]` 확인.

**3) 시계열 없는 지표는 비교 버튼을 비활성화**

`history` 가 빈 카드를 선택하면 `getHomeCompareSeries()` 가 걸러내서 **칩도 선도 안 생긴다** —
버튼만 "비교 중"으로 바뀌어 무반응처럼 보였다. `card.history` 가 비면 `:disabled` + 사유 title 로 막았다.

현재 백엔드는 **GRAPH 모드 항목에만 history 를 붙이므로** 지표 모드 카드는 비교가 막힌다.
Phase 3 에서 history 를 무조건 첨부하면 전 카드가 비교 가능해진다.

**4) 비교 버튼은 Phase 3 에서 별표로 대체된다**

목업은 카드 우상단 별표가 비교 토글이다. 지금 카드는 별표가 이미 "관심 지표 해제"라
충돌하므로, Phase 2 에서는 카드 하단에 임시 `비교` 버튼을 뒀다. Phase 3 카드 재설계 때 정리한다.

**5) 관심 지표 해제 시 비교 선택도 정리**

`removeDashboardFavorite()` 가 선택 배열을 안 건드리면 stale 키가 3칸 중 하나를 계속 차지한다.
해제 시 `removeHomeCompare()` 를 같이 호출하도록 했다.

### 검증 (목 하네스)

단위·스케일이 다른 지표를 섞어 목 데이터를 확장했다(일별 금리 180 · 일별 지수 180 · 월별 경상수지 24 · 월별 미국 CPI 60).

| # | 항목 | 결과 |
|---|---|---|
| 1 | 최대 3개 제한 | PASS (4개째 선택 시 가장 오래된 항목이 밀려남) |
| 2 | 색 순서 | PASS (BLUE `#1f4f9e` → RED `#c02a22` → AMBER `#b5854a`) |
| 3 | 차트 렌더 | PASS (3 데이터셋, 라벨 126, 색 일치) |
| 4 | 길이 다른 시리즈 정렬 | PASS (60포인트 시리즈가 126칸 전 폭으로 리샘플링됨) |
| 5 | `pct` 정규화 | PASS (`[100,110,90]` → `[0,10,-10]`, 모든 시리즈 시작점 0) |
| 6 | `raw` 정규화 | PASS (`[100,110,90]` → `[0,50,-50]`) |
| 7 | base 0 폴백 | PASS (`[0,5,-5]` → `[0,50,-50]`, Infinity 없음) |
| 8 | null 포함 시리즈 | PASS (null 유지, 첫 유효값이 기준) |
| 9 | 기간 반영 | PASS (1M 22 · 1Y 180 포인트) |
| 10 | 차트 누수 | PASS (모드 5회 + 기간 4회 전환에도 인스턴스 6개 고정, 전체 해제 시 5개) |
| 11 | 빈 상태 | PASS (선택 0개 → 차트 destroy + 안내 문구) |
| 12 | 카드 버튼 토글 | PASS (클릭 시 선택 변경, "비교 중" 3개 표시) |
| 13 | history 없는 카드 | PASS (버튼 비활성 1개, 클릭 무반응, title "시계열이 없어 비교할 수 없습니다") |
| 14 | 칩 개별 해제 | PASS (✕ 클릭 → 해당 시리즈만 제거) |

콘솔의 `realestate.tab.nextScheduledAt` 오류는 부동산 API 목을 만들지 않아 `null` 이 반환된 하네스 산물이다
(`d.realestate.tab === null` 확인). 이번 변경과 무관하다.

### 미검증

- 실서버 화면 — 목 하네스까지만
- 실데이터에서 주기가 다른 지표를 섞었을 때의 가독성 — 리샘플링이 실제로 읽을 만한지는 실데이터로 봐야 한다

## Phase 3 — 표시 모드 폐지 + 지표 카드 그리드 (완료, 2026-08-17)

승인: 태형님 "진행해" (2026-08-17) — public API 변경 게이트 통과

### 백엔드 변경

| 파일 | 변경 |
|---|---|
| `favorite/domain/model/FavoriteDisplayMode.java` | **삭제** |
| `favorite/presentation/dto/FavoriteDisplayModeRequest.java` | **삭제** |
| `FavoriteIndicatorController` | `PUT /api/favorites/display-mode` 제거, `reorder` 호출에서 displayMode 제거 |
| `FavoriteOrderRequest` | `displayMode` 필드 제거 + 문서 주석 정정 |
| `FavoriteIndicatorService` | `changeDisplayMode()` 제거, `reorder()` 3인자화, `computeNewOrder` 단순화, history 무조건 첨부 |
| `FavoriteIndicator` (도메인) | `displayMode` 필드·`changeDisplayMode()` 제거 |
| `UserFavoriteIndicatorEntity` | `displayMode` 매핑 제거 (컬럼은 유지) |
| `FavoriteIndicatorMapper` | displayMode 매핑 제거 |
| `FavoriteIndicatorRepository` / `Impl` / `JpaRepository` | `updateDisplayMode` 제거 |
| `EnrichedFavoriteResponse` | 응답에서 `displayMode` 필드 제거 |

**reorder 알고리즘이 크게 단순해졌다.** 컨테이너가 `(sourceType × displayMode)` 에서 `(sourceType)` 하나가 되면서
다른 모드 항목과의 인터리브가 불필요해졌다 — `filterByMode` · `interleavePreservingOthersOrder` ·
`resolveEditedMode` · `fillByPattern` · `takeNextSlot` · `drain` 6개 헬퍼가 사라지고
`computeNewOrder` 는 "페이로드 순서 + 나머지 append" 로 끝난다. 서비스 파일이 31.5KB → 27.4KB.

**마이그레이션 없음.** `display_mode` 는 `NOT NULL DEFAULT 'INDICATOR'` 라 매핑만 지워도 INSERT 가 통과한다.
컬럼 DROP 은 롤백 여지를 남기려고 후속으로 미뤘다.

**history 무조건 첨부의 비용 (수용)**: 항목당 `findHistory` 1회(HISTORY_LIMIT=30행)라 관심 지표 수에 비례한다.
이전에는 GRAPH 모드 항목만 조회했으므로 순증이다. 배치 조회는 조회 키가 ECOS `(statCode, itemCode)` /
GLOBAL `(country, type)` 로 도메인이 달라 두 리포지토리에 per-key limit 쿼리를 새로 넣어야 해서
이번 범위에서는 두고 주석으로 남겼다. HISTORY_LIMIT 이 30이라 절대 비용은 작다.

### 프론트 변경

| 파일 | 변경 |
|---|---|
| `partials/home-indicators.html` | 그래프/지표 2분할 → **단일 카드 그리드** 2종(국내·글로벌)으로 재작성 (376 → 267줄) |
| `js/components/favorite.js` | 컨테이너 스코프 sourceType 단일화, `attemptToggleDisplayMode`·`toggleDisplayMode` 제거, 죽은 차트 헬퍼 2종 제거 (84줄) |
| `js/api.js` | `changeFavoriteDisplayMode` 제거, `reorderFavorites(sourceType, codes)` 2인자화 |
| `js/components/home.js` | 카드 뷰모델·카테고리 필터·스파크라인 빌더 추가 |

### 실행 중 확정된 사항

**1) 별표는 비교 선택, 관심 해제는 ✕**

목업 카드에는 별표 하나뿐인데 별표가 "비교 선택"이다. 기존 카드에서 별표는 "관심 해제"였다.
목업을 따라 별표를 비교 선택으로 쓰고, 관심 해제는 옆에 작은 ✕ 를 뒀다.
Phase 2 에서 임시로 넣었던 카드 하단 `비교` 버튼은 제거했다.

**2) 편집 중에는 카테고리 필터를 숨긴다**

필터로 카드가 숨겨진 채 순서를 저장하면 **숨은 카드가 페이로드에서 빠진다.**
서버는 페이로드에 없는 항목을 뒤로 append 하므로 순서가 조용히 망가진다.
편집 진입 시 필터 UI 를 숨기고 `getHomeIndicatorCards` 가 편집 중에는 필터를 무시하도록 했다.
실측으로 필터 `주가`(1건) 상태에서 편집 진입 시 카드 5건·페이로드 5건 확인.

**3) 스파크라인은 인라인 SVG**

카드마다 1개씩 다수 렌더되므로 Chart.js 인스턴스를 카드 수만큼 만들지 않는다.
`buildHomeSparkline()` 이 viewBox 200x44 기준 path 문자열을 만들고 템플릿은 그리기만 한다.
그 결과 `renderFavoriteChart`/`destroyFavoriteChart`/`favorites._charts` 가 죽은 코드가 되어 함께 제거했다.

**4) 미선택 카드의 스파크라인 색은 일간 방향을 따른다**

목업과 동일하게 선택 시 비교 색(BLUE/RED/AMBER), 미선택 시 상승 RED · 하락 BLUE.

### 검증

| # | 항목 | 결과 |
|---|---|---|
| 1 | `./gradlew compileJava` · `compileTestJava` | PASS |
| 2 | `./gradlew test` (전체) | PASS (BUILD SUCCESSFUL) |
| 3 | `displayMode` 잔여 참조 (Java) | 0건 |
| 4 | `displayMode` 잔여 참조 (static) | 0건 |
| 5 | 카테고리 필터 칩 | PASS (`전체 · 시장금리 · 주가 · 국제수지` — 실제 존재 분류만) |
| 6 | 필터 동작 | PASS (`주가` → 1건, `전체` → 5건) |
| 7 | 편집 중 필터 무시 | PASS (필터 `주가` 상태에서도 카드 5건 · 페이로드 5건) |
| 8 | `reorderFavorites` 시그니처 | PASS (2인자, `(ECOS, [5 codes])` 캡처) |
| 9 | 저장 후 편집 종료 | PASS |
| 10 | 별표 = 비교 토글 | PASS (선택 시 카드 테두리·배경·별표가 비교 색) |
| 11 | 스파크라인 렌더 | PASS (path 길이 1426/270/680, 색이 선택 상태 따라 변경) |
| 12 | 시계열 없는 카드 | PASS (SVG 없음 · "시계열 없음" · 별표 비활성) |
| 13 | 조회 실패 카드(글로벌) | PASS (실패 문구 + 다시 조회 버튼 · 별표 비활성) |
| 14 | 관심 해제(✕) | PASS (카드 5 → 4, 비교 선택에서도 제거) |
| 15 | 죽은 코드 제거 후 회귀 | PASS (해제·필터·비교 정상) |

### 미검증

- 실서버 화면 — 목 하네스까지만
- **기존 사용자 데이터로 순서 편집·저장** — 실DB 에 `display_mode` 가 섞여 있는 상태에서의 reorder 는 validation 단계에서 확인 필요
- history 무조건 첨부 후 `/api/favorites/enriched` 응답 시간

## Phase 4 — 키워드 뉴스 통합 엔드포인트 + 우측 패널 (완료, 2026-08-17)

승인: 태형님 "진행해" (2026-08-17) — 신규 공개 API 게이트 통과

### 백엔드 변경

| 파일 | 변경 |
|---|---|
| `NewsJpaRepository` | `findByKeywordIdInOrderByPublishedAtDesc` · `countByKeywordIdInAndCreatedAtGreaterThanEqual` 추가 |
| `NewsRepository` (포트) | `findLatestByKeywordIds` · `countByKeywordIdsSince` 추가 |
| `NewsRepositoryImpl` | 위 2종 구현 (빈 키워드 목록이면 쿼리 없이 즉시 반환) |
| `KeywordNewsFeedResponse` | **신규** — `keywordCount` · `todayCount` · `items[]` |
| `NewsQueryService` | `getKeywordNewsFeed(userId, size)` 추가, `KeywordService` 주입 |
| `NewsController` | `GET /api/news/feed?userId&size` 추가 |
| `js/api.js` | `getKeywordNewsFeed(userId, size)` 추가 |

**응답에서 `source`(언론사)를 뺐다.** `News` 도메인에 없는 값이라 담을 수 없다.
목업 패널도 키워드·시각·제목만 쓰므로 화면에는 영향이 없다.

**`todayCount` 는 `createdAt`(수집 시각) 기준**이다. "오늘 N건"은 오늘 쌓인 양을 뜻하므로
기사 발행일(`publishedAt`)이 아니라 수집 시각이 맞다.

**홈 진입 호출은 늘지 않는다.** `loadHomeSummary` 의 기존 `Promise.allSettled` 배열에 넣어
다른 6개 호출과 함께 병렬로 나가고, 실패해도 패널만 빈 상태가 된다.

### 프론트 변경

| 파일 | 변경 |
|---|---|
| `partials/home-side.html` | 우측 패널 3종 구현 (3줄 → 120줄) |
| `js/components/home.js` | 알림 계산 3종 · 뉴스 시각 포맷 · 월급 스택 바 · `portfolioItems`/`newsFeed` 상태 |

### 실행 중 확정된 사항

**1) 알림 임계값**

- **배분 이탈**: 밴드를 벗어난 항목 중 **편차가 가장 큰 하나만** 알린다. 전부 띄우면 패널이 배분 알림으로 도배된다.
- **지표 추세**: **4회 연속** 같은 방향. 목업은 "3주 연속"이지만 일별 지표에서 3회 연속은 흔해 알림이 시끄러워진다.
- **만기 임박**: **120일**. 처음 90일로 뒀더니 목업이 임박으로 보여주는 D-102 사례가 걸러졌다.
  실측에서 정기예금(2026-11-20, D-95)이 90일 임계에서는 안 뜨고 120일에서 뜨는 것을 확인했다.

**2) 뉴스 시각 포맷 버그 수정**

`Math.floor((startOfToday - publishedAt) / 86400000)` 로 계산하니 **어제 늦은 시각 기사가 '오늘'로 잡혔다**
(어제 10:58 → 오늘 자정까지 13시간 → 0일). 날짜 경계끼리 비교하도록 고쳤다.
실측: 1h → `15:59`, 13h → `03:59`, 30h → `어제`, 50h → `8.15`.

**3) 월급은 도넛 → 스택 바**

`dashboardSummary.salary` 를 그대로 쓰고 표현만 바꿨다. 금액 0인 항목은 제외, 금액 내림차순 정렬.
색은 목업 팔레트(BLUE·RED·청회색·AMBER·회색) 순환.

### 검증

| # | 항목 | 결과 |
|---|---|---|
| 1 | `./gradlew compileJava` · `compileTestJava` | PASS |
| 2 | `./gradlew test` (전체) | PASS |
| 3 | 패널 3종 렌더 | PASS (확인이 필요한 것 · 내 키워드 뉴스 · 월급 사용 비율) |
| 4 | 알림 3종 | PASS (RED 배분 초과 · BLUE 코스피 4회 연속 하락 · AMBER 정기예금 D-95) |
| 5 | 알림 색 바 | PASS (3개, 톤별 클래스 적용) |
| 6 | 뉴스 링크 | PASS (`target=_blank` + `rel=noopener`, 키워드 태그·시각·제목) |
| 7 | `keyword` null 기사 | PASS (태그 숨기고 시각·제목만) |
| 8 | 뉴스 시각 포맷 | PASS (오늘 시:분 / 어제 / 월.일, 빈값·잘못된 값은 빈 문자열) |
| 9 | 월급 스택 바 | PASS (5구간 합계 100.0%, 금액 0 항목 제외) |
| 10 | 월급 수치 | PASS (목업과 동일 — 41/19/16/13/11%) |
| 11 | 빈 상태 — 알림 0건 | PASS |
| 12 | 빈 상태 — 키워드 0개 | PASS (키워드 등록 유도) |
| 13 | 빈 상태 — 기사 0건 | PASS |
| 14 | 빈 상태 — `newsFeed` null | PASS |
| 15 | 빈 상태 — 지출 0 / salary null | PASS |
| 16 | 우측 패널 폭 | PASS (320px) |

### 미검증

- 실서버 화면 — 목 하네스까지만
- **`GET /api/news/feed` 실호출** — 실DB 로 keywordCount·todayCount 가 맞는지는 validation 단계에서 확인 필요
- 키워드가 많을 때(수십 개) `findByKeywordIdIn` 성능

## Phase 5 — 기존 섹션 제거 · 정리 (완료, 2026-08-17)

### 제거한 것

| 대상 | 결과 |
|---|---|
| 기능 요약 3카드 (뉴스 기록·월급·운영자 로그) | `home.html` 에서 제거 |
| 요약 카드 4장 (키워드·국내·글로벌·포트폴리오) | 제거 — 포트폴리오는 헤더 다크 카드로 이미 이동 |
| 최근 업데이트 박스(국내·글로벌) | Phase 3 카드 그리드 재작성 때 이미 사라짐 |
| `dashboardSummary` 뉴스 기록 페치 | 제거 — 홈 진입 호출 1건 감소 |
| `dashboardSummary` 운영자 로그(incident) 페치 | 제거 — 홈 진입 호출 1건 감소(admin 한정이었음) |
| 월급 도넛 차트(`_renderDashboardSalaryDonut`·`_chartInstance`·`destroyDashboardSummaryChart`) | 제거 — 우측 패널 스택 바로 대체 |
| `home.js` `hasEcosDashboardContent`·`hasGlobalDashboardContent` | 제거 (18줄) |
| `favorite.js` `scrollFavorites` | 제거 — 가로 스크롤 UI 자체가 사라짐 |

`API.getTodayIncidentCount` 와 `API.getNewsJournalDashboardSummary` 정의는 `api.js` 에 남겼다.
각각 운영자 로그·뉴스 기록 화면에서 쓰일 수 있는 공개 클라이언트 메서드라 홈에서 안 쓴다고 지울 이유가 없다.

### 파일 규모

| 파일 | 변화 |
|---|---|
| `partials/home.html` | 496 → **67줄** (헤더·브리핑·포트폴리오 카드만) |
| `partials/home-indicators.html` | 신규 267줄 |
| `partials/home-side.html` | 신규 124줄 |
| `js/components/dashboardSummary.js` | 181 → **67줄** |
| `js/components/favorite.js` | 431 → **307줄** |

### `destroyDashboardSummaryChart` 제거에 따른 후속 처리

`app.js` 두 곳에서 이 메서드를 부르고 있었다.
- `cleanupRegistry.home` — 항목 자체를 제거(홈 셸에 이제 차트가 없다)
- `navigateTo` 의 "home 떠날 때" 분기 — `destroyHomeCompareChart()` 호출로 교체

교체하지 않고 지우기만 했으면 **홈을 떠나도 비교 차트가 살아남아** 재진입 시 인스턴스가 누적된다.
실측으로 왕복 확인: 진입 0 → 비교 선택 1 → 홈 이탈 0 → 재진입 1.

### 검증

| # | 항목 | 결과 |
|---|---|---|
| 1 | `./gradlew compileJava` · `compileTestJava` · `test` | PASS |
| 2 | 제거 확인 — 기능 요약·요약 카드·최근 업데이트·도넛 canvas | 전부 DOM 에 없음 |
| 3 | 유지 확인 — 오늘의 시장·브리핑·비교 보기·관심 지표·우측 패널 3종 | 전부 렌더 |
| 4 | 운영자 로그 접근 경로 | PASS (일반 사용자 숨김 / `auth.isAdmin` 시 사이드바 메뉴 노출) |
| 5 | 죽은 심볼 잔여 참조 7종 | 0건 (`hasEcos/GlobalDashboardContent`·`scrollFavorites`·`dashboardSalaryDonut`·`destroyDashboardSummaryChart`·`displayMode`·`renderFavoriteChart`) |
| 6 | 차트 인스턴스 왕복 | PASS (0 → 1 → 0 → 1, 누수 없음) |
| 7 | 월급 데이터 로드 | PASS (도넛 제거 후에도 우측 패널 스택 바 정상) |

### 미검증

- 실서버 화면 — 5개 Phase 전부 목 하네스까지만 확인했다
- 모바일 반응형은 Phase 1 에서 2단 → 1단 전환만 확인했고, 카드 그리드·우측 패널이 채워진 상태는 미확인


---

## Phase 6 — 목업 디자인 반영 (완료, 2026-08-17)

Phase 1~5 로 구조·동작은 목업대로 맞췄지만 **보이는 디자인이 목업과 달랐다.**
태형님 지적("목업에선 디자인을 이렇게 제공했는데 하나도 반영이 안되어있는데?")으로 확인한 결과,
구성 요소는 다 만들었으나 배치·조작 방식·타이포·색이 어긋나 있었다.
구조와 동작만 검증하고 **목업과 나란히 놓고 대조하지 않은 것**이 원인이다.

### 6-1. 배치·조작 (목업 구조 대조)

| 항목 | 목업 | 수정 전 | 조치 |
|---|---|---|---|
| 오늘 브리핑 | 독립 흰 카드 + 우측에 포트폴리오 다크 카드(같은 행) | 제목 아래 인라인 텍스트 | 카드로 분리, 2열 행 구성 |
| 기간·모드 버튼 | 제목 행 맨 우측 최상단 | 포트폴리오 카드와 한 묶음 | 제목 행으로 이동 |
| 카드 푸터 | 좌 `6M +3.6%` / 우 `08.07` | 좌우 반대 | 스왑 |
| 카드 클릭 | **카드 전체 클릭**으로 비교 추가, 별표는 상태 표시 | 별표 버튼만 클릭 가능 | 카드에 클릭 이관, 별표를 `<button>` → `<span>` |
| 안내 문구 | "클릭해 비교에 추가 · 최대 3개" | "별표를 눌러…" | 목업 문구로 |

### 6-2. 디자인 토큰 (목업 computed style 실측)

추측하지 않고 목업 페이지를 띄워 `getComputedStyle` 로 값을 뽑아 넣었다.

| 항목 | 실측값 |
|---|---|
| 본문 폰트 | `IBM Plex Sans KR` |
| 숫자 폰트 | `IBM Plex Mono` (+ `tabular-nums`) |
| 페이지 배경 | `#eef0f3` |
| 제목 | 20px / 600 / `#14161a` / letter-spacing −0.2px |
| 카드 | radius 10px · border `rgba(0,0,0,.08)` · **그림자 없음** |
| 지표 셀 | radius 9px · padding `11px 12px 8px` |
| 지표 값 | Mono 17px / 600 / letter-spacing −0.34px |
| 섹션 제목 | 12.5px / 600 |
| 색 | RED `#c02a22` · BLUE `#1f4f9e` · AMBER `#b5854a` + tint 3종 |
| 사이드바 활성 | 배경 `#eff3fa` (지표 셀 tint 와 **다른 톤**) |

`css/custom.css` 에 `--dc-*` 토큰과 `.dc-card` / `.dc-cell` / `.dc-h1` / `.dc-title` 클래스로 정리했다.

### 6-3. 사이드바

목업은 **아이콘 없이 텍스트만**, 활성 항목에 5px 파란 점 + `#eff3fa` 배경.
실측값(12px · padding 8px 10px · radius 7px · gap 9px · 항목 간격 1px · 행 높이 34px)대로 맞췄다.

**접힘 모드(`w-14`)에는 아이콘을 남겼다.** 목업에 접힘 상태가 없는데 라벨을 감추는 모드라,
아이콘까지 없애면 빈 버튼만 남는다. 펼침 = 목업(점+텍스트), 접힘 = 아이콘으로 갈랐다.
데스크탑·모바일에 중복돼 있던 아이콘 SVG 를 한 벌로 합쳐 157줄 → 123줄.

### 변경 파일

| 파일 | 변경 |
|---|---|
| `css/custom.css` | `--dc-*` 토큰 + 실측 기반 클래스 |
| `index.html` | IBM Plex 폰트 로드, `body` → `app-surface` |
| `partials/home.html` | 제목 행 / 브리핑 카드 / 포트폴리오 다크 카드 재구성 |
| `partials/home-indicators.html` | 카드 클릭 토글, 푸터 스왑, 토큰 적용 |
| `partials/home-side.html` | 알림 색 토큰 |
| `partials/_sidebar.html` | 목업 스타일로 재작성 |
| `js/components/home.js` | 알림 톤 토큰화, 카드 기준일 산출 |

### 검증

렌더된 화면의 computed style 이 목업과 **수치까지 일치**함을 확인했다.

```
body: IBM Plex Sans KR / rgb(238,240,243)
h1:   20px / 600 / rgb(20,22,26)
cell: radius 9px / padding 11px 12px 8px / 1px solid rgba(0,0,0,.08)
value: IBM Plex Mono / 17px / 600
sidebar active: 12px / 600 / rgb(31,79,158) / bg rgb(239,243,250) / radius 7px / 행 34px / 점 5px
```

### 범위 주의

**폰트·페이지 배경·사이드바는 `body` 와 공용 partial 에 걸려 전 화면에 적용된다.**
홈만 적용하면 화면 간 이질감이 생겨 전역으로 갔다. 타 화면 회귀 확인이 validation 항목에 추가됐다.
