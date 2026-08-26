# 홈 대시보드 경제 대시보드형 리디자인 Validation (#114)

gate: docs/gates/2026-08-17-home-dashboard-redesign-gates.md
review: docs/reviews/2026-08-17-home-dashboard-redesign-review.md
work: docs/works/2026-08-17-home-dashboard-redesign-work.md

## 실행 환경

- 빌드: `./gradlew bootJar -x test`
- 기동: `set -a; source .env; set +a` → `DATABASE_URL` 의 `host.docker.internal` → `localhost` 치환
  → `KAKAO_REDIRECT_URI=http://localhost:8080/oauth/kakao`
  → `SPRING_PROFILES_ACTIVE=dev SERVER_PORT=8080 nohup java -jar ...`
- DB: 로컬 도커 Postgres 16 (`local-postgres`, `stocks`)
- 기동 결과: `Started StockMarketApplication in 4.38 seconds`, `/` 200
- 인증: 태형님이 카카오 로그인 수행 (JWT `sub: 1`, `role: USER`)

## 실행한 명령과 결과

| 명령 | 결과 |
|---|---|
| `./gradlew compileJava` | PASS |
| `./gradlew compileTestJava` | PASS |
| `./gradlew test` (전체) | PASS |
| `./gradlew bootJar -x test` | PASS |
| 앱 기동 (dev, :8080) | PASS |

---

## V1. 인증 없이는 검증 자체가 불가능했다 (환경 이슈)

브라우저 localStorage 에 이전 세션의 **가짜 토큰 `dev-harness`** 가 남아 있었다.
프론트 게이트는 토큰 존재 여부만 보므로 **로그인된 것처럼 보였지만**(헤더에 표시명까지 노출)
실제 인증은 되지 않아 인증 기반 API 가 전부 실패했고, 화면이 비어 보였다.

- `/api/favorites/enriched` → **500** (`ClassCastException: String → Long`) — 기존 `getCurrentUserId()` 패턴이
  익명 principal(`"anonymousUser"`)을 그대로 캐스팅한다
- `/api/news/feed` → **403** — 이번에 넣은 `NewsSecurityContext` 가 같은 상황을 500 대신 403 으로 처리
- 포트폴리오·배분 API 는 `userId` 파라미터 방식 + dev permitAll 이라 인증 없이도 값이 나왔다
  → **일부만 데이터가 보여 더 헷갈렸다**

`DevSecurityConfig` 도 `JwtAuthenticationFilter` 를 붙이므로 **유효한 JWT 없이는 principal 이 잡히지 않는다.**
테스트용 토큰 발급 경로는 없어서 실제 카카오 로그인이 필요했다.

**교훈**: 하네스용 가짜 토큰은 검증 전에 지워야 한다. 안 지우면 "기능이 깨진 것"으로 오진하기 쉽다.

---

## 신규/변경 API 실호출

| 요청 | 결과 |
|---|---|
| `GET /api/news/feed?userId=1&size=5` | **200** — `keywordCount` · `todayCount` · `items` |
| `GET /api/news/feed?userId=9999&size=5` | **403** — **review H1 조치 동작 확인 (IDOR 차단)** |
| `GET /api/favorites/enriched` | 200 — 응답에 `displayMode` 필드 **없음** (표시 모드 폐지 확인) |
| `PUT /api/favorites/order` (역순 페이로드) | **204** — 서버 재조회 시 순서 그대로 반영 |
| `PUT /api/favorites/display-mode` | `NoResourceFoundException` — **엔드포인트 제거 확인** |
| `POST` / `DELETE /api/favorites` | 200 — 등록·해제 정상 |

### history 무조건 첨부 (Phase 3 핵심 변경)

관심 지표 4건 등록 후 **4건 전부** history 첨부 확인 (14 · 16 · 12 · 2 포인트).
이전에는 GRAPH 모드 항목만 붙었다.

### 순서 편집 — 기존 스키마에서 동작

`display_mode` 컬럼이 남아 있는 상태(매핑만 제거)에서 reorder 가 정상 동작했다.
리뷰에서 미검증으로 남겼던 항목이 해소됐다.

---

## V2. 실데이터에서만 드러난 결함 2건 (수정 완료)

### V2-1. `cycle` 은 주기 라벨이 아니라 날짜였다

카드 우하단 기준일을 `history` 마지막 스냅샷 → 없으면 `cycle` 로 폴백하게 만들었는데,
ECOS 실데이터의 `cycle` 값은 **`20260815`** 형식의 날짜 문자열이었다(목 하네스에서는 `"일"` 로 만들어 뒀다).
폴백 경로에서 `20260815` 가 raw 로 노출될 상황이었다.

**조치**: `_homeFormatRefDate()` 로 분리해 history·cycle 양쪽에 같은 규칙 적용
(`20260815` → `08.15`, `202607` → `2026.07`, 그 외는 원문).

### V2-2. 기간 라벨이 실제 구간을 과장했다

코스피 카드가 `6M −13.7%` 로 표시됐다. 계산은 맞지만(8088.34 → 6977.94)
**실제 history 는 `2026-07-05 ~ 08-17`, 6주치**였다. 수집을 시작한 지 얼마 안 돼 데이터가 짧은데
`6M` 이라고 쓰면 **6개월간 13.7% 하락**으로 읽힌다. 목업은 120포인트 일별 데이터라 이 문제가 없었다.

**조치**: 요청 기간보다 데이터가 짧으면 실제 시작일을 라벨에 쓴다 — `07.05~ −13.7%`,
툴팁에 `07.05 ~ 08.17 기준 · 선택한 6M 보다 짧은 구간입니다`.

---

## V3. review Medium 2건 — 실측 결과 조치 불필요로 닫음

관심 지표 **30건**을 등록해 측정했다.

### M2 — `/enriched` N+1

| 관심 지표 | 응답 시간 |
|---|---|
| 0건 | 12ms |
| 4건 | 52ms (콜드 스타트 포함) |
| **30건** | **29ms** |

`HISTORY_LIMIT=30` 이라 쿼리 하나가 매우 싸다. 30건 전부 history 를 붙여도 29ms —
"관심 지표 수에 비례해 느려진다"는 우려가 실제로는 성립하지 않았다.

### M1 — 뷰모델 재계산

상태 변경 1회(기간 버튼 클릭)당, 관심 지표 30건 기준:

| 헬퍼 | 호출 | 소요 |
|---|---|---|
| `getHomeIndicatorCards` | 5회 | 7.2ms |
| `getHomeAlerts` | 4회 | 4.3ms |
| 나머지 4종 | 9회 | 1.1ms |
| **합계** | | **12.6ms** |

현재 history 가 지표당 8~16건뿐이라, **250포인트(1년치 일별)로 부풀려 미래 상황도 시뮬레이션**했다
→ 추정 **18.4ms**.

**결론: 메모이제이션을 넣지 않는다.**
- 최악(30지표 × 250포인트)에서도 18ms이고 **버튼 클릭 시에만** 발생한다. 연속 렌더가 아니라 체감되지 않는다
- 반면 캐시는 리뷰에 적어둔 함정이 실재한다 — `removeDashboardFavorite` 가 버킷 배열만 교체하고
  `enrichedFavorites` 참조는 유지해서, 키를 잘못 잡으면 **해제한 카드가 화면에 남는 회귀**가 생긴다
- 18ms 를 아끼려고 그 위험을 지는 건 손해다

→ 리뷰에서 Medium 으로 올린 두 건은 **근거가 약했다.** 코드는 그대로 둔다.

---

## V4. 화면 검증 (실서버)

### 홈 대시보드 (관심 지표 12건 등록 상태)

- 지표 카드 그리드 — 스파크라인 · 값 · 일간 델타 · 기간 라벨 · 기준일 실렌더
- 카테고리 필터 칩 — `전체 / 시장금리 / 주식 / 환율 / 소비자·생산자 물가 / 성장률 / 국제수지`
- 카드 클릭 → 비교 차트 렌더 (14포인트)
- 오늘 브리핑 3종, 포트폴리오 다크 카드(5,730만원 · 안전 47% / 투자 53% · 배분 밴드 초과)
- 우측 패널 — 알림 1건(부동산 49.8%, 목표 25%, 747만원 초과)

### 디자인 (목업 computed style 대조)

```
body: IBM Plex Sans KR / rgb(238,240,243)
h1:   20px / 600 / rgb(20,22,26)
cell: radius 9px / padding 11px 12px 8px / 1px solid rgba(0,0,0,.08)
value: IBM Plex Mono / 17px / 600
sidebar active: 12px / 600 / rgb(31,79,158) / bg rgb(239,243,250) / radius 7px / 행 34px / 점 5px
```

### 모바일 반응형 (375×812, 관심 지표 12건)

| 항목 | 결과 |
|---|---|
| 가로 스크롤 | 없음 (`scrollWidth 375 = clientWidth`) |
| 2단 → 1단 | 우측 패널이 아래로 스택 |
| 지표 카드 | 1열 309px, 12장 |
| 카테고리 필터 | 2줄 wrap |
| 사이드바 | 숨김 + 햄버거 노출 |
| 넘치는 요소 | 0건 |

### 타 화면 회귀 (폰트·배경·사이드바 전역 변경 영향)

11개 화면 순회 — **전부 렌더, 가로 넘침 0건, 콘솔 오류 0건.**

키워드 · 뉴스 검색 · 국내 경제지표 · 글로벌 경제지표 · 포트폴리오 · 종목 평가 ·
기업 리포트 · 월급 사용 비율 · 뉴스 기록 · 용어 사전 · 부동산 시장

국내 경제지표 화면은 시각 확인까지 수행 — 파생지표 카드 · 표 · 별표 · 카테고리 탭 정상,
새 폰트에서 숫자가 모노로 정렬돼 가독성이 개선됐다.

---

## 운영 적용 필수 항목

**없음.** 이번 변경에는 마이그레이션이 없다.

`display_mode` 컬럼은 `NOT NULL DEFAULT 'INDICATOR'` 라 매핑만 제거해도 INSERT 가 통과한다(실서버 확인).
컬럼 DROP 은 롤백 여지를 남기려고 하지 않았다.

외부 폰트(IBM Plex)를 Google Fonts CDN 에서 로드하므로 **운영 환경의 외부 CDN 접근이 필요**하다.
이미 Tailwind · Chart.js · Pretendard 를 CDN 으로 쓰고 있어 새로 생긴 제약은 아니다.

## 미검증 항목

- **운영 DB 반영** — 로컬 도커 Postgres 에서만 확인
- **글로벌 지표 카드** — 이 계정에 글로벌 관심 지표가 없어 ECOS 카드만 실데이터로 확인
  (글로벌 조회 실패 카드 · 재조회 버튼 경로는 목 하네스에서만 확인)
- **키워드 뉴스 패널 실데이터** — 계정에 활성 키워드가 0건이라 빈 상태만 확인
- **지표 추세 알림** — history 가 짧아(8~16포인트) 4회 연속 조건이 성립하는 지표가 없었다
- **만기 임박 알림** — 120일 이내 만기 항목이 없어 미노출

## 정리한 테스트 데이터

- 측정용으로 등록한 관심 지표 30건 → **전량 삭제**
- 이후 태형님 요청으로 12건 재등록 후 **유지**
  (시장금리 2 · 주식 2 · 환율 2 · 물가 2 · 성장률 2 · 국제수지 2)
- 앱은 :8080 에 기동 상태로 유지
