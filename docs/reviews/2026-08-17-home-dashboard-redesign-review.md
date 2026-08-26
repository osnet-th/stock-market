# 홈 대시보드 경제 대시보드형 리디자인 Review (#114)

gate: docs/gates/2026-08-17-home-dashboard-redesign-gates.md
plan: docs/plans/2026-08-17-001-feat-home-dashboard-redesign-plan.md
work: docs/works/2026-08-17-home-dashboard-redesign-work.md

리뷰 대상: `feat/issue-114-home-dashboard-redesign` 전체 diff (24 파일 수정 + 8 파일 신규, +945/−1,051)

---

## Findings

### H1. `GET /api/news/feed` 가 클라이언트가 보낸 `userId` 를 검증 없이 신뢰한다

`news/presentation/NewsController.java:44`

```java
@GetMapping("/feed")
public ResponseEntity<KeywordNewsFeedResponse> getKeywordNewsFeed(
        @RequestParam Long userId,      // ← 클라이언트가 보낸 값을 그대로 사용
        @RequestParam(defaultValue = "5") int size) {
    return ResponseEntity.ok(newsQueryService.getKeywordNewsFeed(userId, size));
}
```

`ProdSecurityConfig:87` 의 `anyRequest().authenticated()` 에 걸리므로 **로그인은 필요**하지만,
로그인한 사용자가 `userId` 만 바꾸면 **다른 사용자의 키워드 목록과 수집 기사를 그대로 조회**할 수 있다.
키워드는 그 사람이 무엇에 투자하는지를 드러내는 정보라 노출 영향이 작지 않다.

같은 코드베이스에 안전한 패턴이 이미 있다 — `FavoriteIndicatorController:113`

```java
private Long getCurrentUserId() {
    return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
}
```

**참고**: `KeywordController` · `PortfolioController` 등 기존 엔드포인트도 `userId` 를 파라미터로 받는
같은 구조다. 즉 이번 변경이 새로 만든 문제는 아니고 **기존 관행을 따라간 것**이다.
다만 이번에 새로 여는 엔드포인트이므로, 관행을 따를지 안전한 쪽으로 갈지는 지금 정하는 게 싸다.

**제안**: `getKeywordNewsFeed` 를 `SecurityContextHolder` 기반으로 바꾸고 `userId` 파라미터를 없앤다.
프론트 `API.getKeywordNewsFeed(userId, size)` 도 인자에서 userId 를 뺀다.
기존 엔드포인트 일괄 정리는 범위가 크므로 별도 이슈로 분리한다.

---

### M1. 홈 뷰모델 헬퍼가 상태 변경 1회마다 여러 번 재계산된다

`js/components/home.js` · `partials/home-indicators.html` · `partials/home-side.html`

Alpine 은 `x-show` / `x-if` / `x-for` / `x-text` 표현식을 리렌더마다 다시 평가한다.
같은 헬퍼가 템플릿 여러 곳에 쓰여 **한 번의 상태 변경에 아래만큼 반복 호출**된다(실측).

| 헬퍼 | 상태 변경 1회당 호출 수 | 1회 비용 |
|---|---|---|
| `getHomeIndicatorCards` | 4회 | 카드 전원 뷰모델 재생성 + **스파크라인 path 문자열 재생성**(최대 252 포인트) |
| `getHomeAlerts` | 4회 | 관심 지표 전체 history 파싱(추세 판정) + 포트폴리오 항목 순회 |
| `getHomeSalaryBreakdown` | 4회 | 정렬 + 매핑 |
| `getHomeCompareSeries` | 3회 | 선택 지표 history 슬라이스·파싱 |

관심 지표가 5개인 하네스에서는 체감되지 않지만, 20~30개로 늘면 기간 버튼 한 번에
수천 포인트를 여러 번 다시 훑게 된다. #110 리뷰 L4(그룹 메모이제이션)와 같은 성격의 문제다.

**제안**: `homeSummary.enrichedFavorites` · `homeDashboard.range/filter/selected` 를 키로 한
캐시를 두고, 키가 바뀔 때만 재계산한다. 최소한 `getHomeIndicatorCards` 와 `getHomeAlerts` 두 개만
memoize 해도 대부분 해소된다.

---

### M2. `/api/favorites/enriched` 의 시계열 조회가 관심 지표 수에 비례한다

`favorite/application/FavoriteIndicatorService.java` — `attachHistoryToEcos` / `attachHistoryToGlobal`

표시 모드 폐지로 history 를 **전 항목에** 붙이게 되면서, 이전에는 GRAPH 모드 항목만 돌던 루프가
이제 모든 관심 지표를 돈다. 항목당 `findHistory` 1회(각 30행)라 관심 지표 20개면 조회 20회다.

work 문서에 "수용"으로 기록해 두긴 했으나, 홈 진입 경로의 주 API 라 리뷰에서는 남는 리스크로 본다.
`HISTORY_LIMIT=30` 이라 절대 비용은 작고, 배치화하려면 ECOS `(statCode, itemCode)` ·
GLOBAL `(country, type)` 두 도메인에 per-key limit 쿼리를 새로 넣어야 한다.

**제안**: 이번 범위에서는 그대로 두되 **validation 단계에서 실측**(관심 지표 N개일 때 응답 시간)하고,
느리면 후속 이슈로 분리한다.

---

### L1. 미사용 import 잔존

`favorite/application/FavoriteIndicatorService.java:32` — `import java.util.Iterator;`

인터리브 헬퍼(`drain` · `interleavePreservingOthersOrder`)를 제거하면서 유일한 사용처가 사라졌다.

### L2. `loadHomeNewsFeed()` 는 죽은 코드다

`js/components/home.js:489`

키워드 피드는 `loadHomeSummary()` 의 `Promise.allSettled` 배열 안에서 로드하도록 바꿨는데,
초기에 만든 단독 로더가 남아 있다. 호출처 0건(템플릿·JS 전체 grep 확인).

**제안**: 제거하거나, 피드만 갱신하는 새로고침 동작을 붙일 거면 그때 살린다.

### L3. 필터가 걸린 채 그 분류의 마지막 지표를 해제하면 빈 목록에 갇힌 것처럼 보인다

`js/components/home.js` `getHomeCategories()` 는 **현재 관심 지표에 존재하는 분류만** 칩으로 만드는데,
`homeDashboard.filter` 는 독립 상태다. 예: `주가` 필터 상태에서 코스피를 해제하면
`주가` 칩이 사라지고 목록은 0건이 된다. `전체` 칩이 남아 있어 복구는 되지만 안내가 없다.

**제안**: 관심 해제 시 `filter` 가 더 이상 유효하지 않으면 `전체` 로 되돌린다(한 줄).

### L4. 홈이 `PortfolioComponent` 의 `formatKrwCompact` 에 의존한다

`partials/home.html` · `js/components/home.js` 여러 곳

`app.js` 가 전 컴포넌트를 한 객체로 spread 해 동작은 하지만, 홈 → 포트폴리오 컴포넌트 방향의
암묵적 결합이 생겼다. 포트폴리오에서 이 헬퍼를 지우거나 이름을 바꾸면 홈이 조용히 깨진다.

**제안**: 공용 포맷 헬퍼는 `utils/format.js` 로 올리는 게 맞다. 다만 포트폴리오 쪽 호출부까지
건드려야 해서 이번 범위는 아니다 — 후속 정리 후보로만 남긴다.

### L5. 도달 불가능한 방어 코드

`news/application/dto/KeywordNewsFeedResponse.java` — `Item.keyword` 가 null 일 수 있다고 문서화했지만,
피드는 **그 사용자의 활성 키워드 id 로만** 조회하므로 `nameById.get()` 이 항상 값을 찾는다.
동작에는 문제 없고 방어로서 해롭지도 않으나, 주석이 실제보다 넓게 읽힌다.

---

### L6. 피드 조회 실패와 키워드 0개가 같은 문구로 보인다 (H1 조치 중 발견)

`partials/home-side.html`

`x-if="!homeSummary.newsFeed || keywordCount === 0"` 한 조건에 두 상태가 묶여 있어
**403·네트워크 실패도 "등록한 키워드가 없습니다"로 표시**됐다. 사용자가 서버 문제를 자기 상태 문제로 오해한다.
H1 조치로 403 이 실제로 발생할 수 있게 되면서 드러났다.

---

## 조치 결과 (2026-08-17, 태형님 "수정안대로 바꿔줘")

| # | 조치 | 내용 |
|---|---|---|
| H1 | **수정** | 대안 채택 — `userId` 파라미터는 유지하고 인증 주체와 일치하는지 확인. `NewsSecurityContext.matchesCurrentUser()` 신설, 불일치·미인증이면 **403** |
| M1 | **조치 불필요로 종결** | validation 실측 — 관심 지표 30건 · history 250포인트 시뮬레이션에서도 상태 변경당 18ms. 버튼 클릭 시에만 발생해 체감 없음. 캐시 무효화 위험이 이득보다 큼 |
| M2 | **조치 불필요로 종결** | validation 실측 — 관심 지표 30건에서 `/enriched` **29ms**. `HISTORY_LIMIT=30` 이라 쿼리가 싸서 우려가 성립하지 않음 |
| L1 | **수정** | `java.util.Iterator` 미사용 import 제거 |
| L2 | **수정** | 죽은 `loadHomeNewsFeed()` 제거 (13줄) |
| L3 | **수정** | 관심 해제 후 필터가 유효하지 않으면 `전체` 로 복구 |
| L4 | **보류** | `formatKrwCompact` 공용화는 포트폴리오 호출부까지 영향 → 후속 정리 후보 |
| L5 | **미조치** | 도달 불가 방어 코드. 동작·안전에 영향 없어 그대로 둠 |
| L6 | **수정** | 조회 실패(null)와 키워드 0개를 분리해 각각 다른 문구 표시 |

### H1 을 "파라미터 유지 + 일치 검증"으로 간 이유

principal 만 쓰는 방식(파라미터 제거)이 더 깔끔하지만,
- 기존 뉴스·키워드·포트폴리오 엔드포인트가 전부 `userId` 파라미터 형태라 **혼자만 다른 규약**이 된다
- 나중에 전체를 일괄 정리할 때 오히려 두 번 고치게 된다

예외를 던지지 않고 컨트롤러가 403 을 직접 반환하도록 한 것은,
`GlobalExceptionHandler` 에 `AccessDeniedException` / `InsufficientAuthenticationException` 핸들러가 없어
**던지면 `handleGeneral` 로 떨어져 500** 이 되기 때문이다. 공용 핸들러를 건드리면
company-report·newsjournal·glossary 등 다른 도메인의 에러 코드까지 바뀌므로 이번 범위 밖으로 뒀다.

→ **후속 후보**: `GlobalExceptionHandler` 에 인증/인가 예외 핸들러 추가(401/403), 기존 엔드포인트의 userId 검증 일괄 적용.

### M1 종결 근거와, 혹시 나중에 넣게 될 때의 주의

`removeDashboardFavorite` 는 `enrichedFavorites.ecos` 를 **새 배열로 교체**하지만
`enrichedFavorites` 객체 참조는 유지한다. 캐시 키를 객체 참조로만 잡으면
**해제한 카드가 화면에 남는 회귀**가 생긴다. 버킷 배열 참조까지 키에 넣거나 명시적으로 무효화해야 한다.
또 캐시 저장소는 `Object.defineProperty` 로 non-enumerable 로 두어야 Alpine 반응형 프록시에 들어가지 않는다.

### 조치 검증

| # | 항목 | 결과 |
|---|---|---|
| 1 | `./gradlew compileJava` · `compileTestJava` · `test` | PASS |
| 2 | L1 — `Iterator` 잔여 참조 | 0건 |
| 3 | L2 — `loadHomeNewsFeed` 잔여 참조 | 0건 (`typeof === 'undefined'` 확인) |
| 4 | L3 — 필터 복구 | PASS (`주가` 필터에서 유일 지표 해제 → 필터 `전체` 복구, 0건 → 4건) |
| 5 | L6 — 실패 문구 | PASS (`newsFeed` null → "뉴스를 불러오지 못했습니다", 키워드 0개 → "등록한 키워드가 없습니다", 기사 0건 → "아직 수집된 기사가 없습니다") |
| 6 | 정상 상태 회귀 | PASS (기사 목록·알림·월급 바 정상) |

H1 은 인증 컨텍스트가 필요해 목 하네스로는 검증할 수 없다 — **validation 단계에서 실서버로 확인**한다
(본인 userId 200 / 타인 userId 403).

---

## Open Questions / Assumptions

1. **H1 을 이번 범위에서 고칠지** — 기존 엔드포인트 관행과 다르게 가는 선택이라 태형님 판단이 필요하다.
   고친다면 신규 엔드포인트 1개만 안전 패턴으로 가고, 기존 정리는 별도 이슈로 분리할 것을 제안한다.
2. **M1 메모이제이션을 지금 넣을지, validation 실측 후 판단할지.**
3. **가정**: 관심 지표 개수는 통상 10~20개 수준이다. 이보다 훨씬 많다면 M1·M2 의 우선순위가 올라간다.
4. **가정**: `display_mode` 컬럼을 남겨 둔 상태로 운영에 나가도 된다(롤백 여지 확보 목적).
   컬럼 DROP 은 후속 이슈로 미뤄져 있다.
5. **미검증 이월**: 5개 Phase 전부 목 하네스까지만 확인했다. 실서버 확인 필요 항목은
   `GET /api/news/feed` 실호출 · 기존 데이터로 순서 편집·저장 · `/enriched` 응답 시간 ·
   모바일에서 카드·패널이 채워진 상태다.

---

## Change Summary

목업(`경제 대시보드 리디자인`)대로 홈을 "오늘의 시장" 단일 화면으로 재구성했다.

- **화면**: 2단 레이아웃(메인 + 320px 우측 패널), `오늘의 시장` 헤더(기간·정규화 모드·포트폴리오 카드),
  오늘 브리핑, 지표 비교 보기(최대 3개 오버레이), 단일 지표 카드 그리드(스파크라인·카테고리 필터·순서 편집),
  우측 패널 3종(알림·키워드 뉴스·월급 스택 바)
- **API 변경**: `PUT /api/favorites/display-mode` 제거, `PUT /api/favorites/order` 에서 `displayMode` 제거,
  `GET /api/news/feed` 신설
- **정리**: 표시 모드 폐지로 reorder 인터리브 로직 6개 헬퍼 제거, 기존 홈 섹션(기능 요약·요약 카드·최근 업데이트) 제거,
  월급 도넛 차트·`renderFavoriteChart` 등 죽은 코드 제거. 홈 진입 API 호출 2건 감소
- **검증**: `compileJava`·`compileTestJava`·`test` 전체 PASS, 목 하네스 실측 Phase 합계 64항목 PASS

`display_mode` 컬럼은 DB 기본값이 있어 매핑만 제거했고 마이그레이션은 없다.
