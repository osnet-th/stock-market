---
issue: 114
issue_url: https://github.com/osnet-th/stock-market/issues/114
branch: feat/issue-114-home-dashboard-redesign
gate: docs/gates/2026-08-17-home-dashboard-redesign-gates.md
brainstorm: docs/brainstorms/2026-08-17-home-dashboard-redesign-brainstorm.md
---

# 홈 대시보드 경제 대시보드형 리디자인 (#114)

## Overview

홈 대시보드를 목업(`경제 대시보드 리디자인 (단일파일).html`)의 "오늘의 시장" 구조로 전면 교체한다.

5개 Phase로 나눈다. Phase 1~2는 프론트 전용이라 되돌릴 결정이 없고,
Phase 3~4는 public API 변경이 포함되어 **Phase 착수 전 개별 승인**을 받는다.

| Phase | 내용 | 성격 | 승인 |
|---|---|---|---|
| 1 | 2단 레이아웃 · 헤더 · 브리핑 · 포트폴리오 요약 카드 | 프론트 | 불필요 |
| 2 | 지표 비교 보기(오버레이 차트) | 프론트 | 불필요 |
| 3 | 표시 모드 폐지 + 지표 카드 그리드 통합 | **API 변경** | 필요 |
| 4 | 키워드 뉴스 통합 엔드포인트 + 우측 패널 | **신규 API** | 필요 |
| 5 | 기존 홈 섹션 제거 · 정리 | 프론트 | 불필요 |

### 공통 규칙
- 색 토큰은 목업을 따른다: RED `#c02a22` · BLUE `#1f4f9e` · AMBER `#b5854a` (#110 포트폴리오와 동일 계열)
- 차트는 Chart.js 유지(기존 코드 일관성). 스파크라인은 카드당 1개씩 다수 렌더되므로 **인라인 SVG**로 그린다
- `home.html` 이 496줄 → 목업 구조 반영 시 900줄 초과 예상 → **partial 분리**(아래 Phase 1)

---

## Phase 1 — 레이아웃 셸 · 헤더 · 브리핑

### 변경 파일
| 파일 | 변경 |
|---|---|
| `partials/home.html` | 셸로 축소 — 2단 그리드 + 헤더 + 브리핑 + 포트폴리오 요약 카드 |
| `partials/home-indicators.html` | **신규** — 비교 보기 + 관심/글로벌 지표 그리드 (Phase 2~3에서 채움) |
| `partials/home-side.html` | **신규** — 우측 패널 (Phase 4에서 채움) |
| `js/app.js` | `partialNames` 에 2종 추가 |
| `static/index.html` | 마운트 지점 추가 |
| `js/components/home.js` | 헤더 상태(`range`, `mode`) · 브리핑 계산 · 기준시각 |

### 화면 규칙
- 헤더 좌: `오늘의 시장` + `2026.08.17 (일) 08:45 기준`
- 헤더 우: 기간 버튼 `1M / 3M / 6M / 1Y` (기본 6M) + `변화율(%) 기준` 토글
- 브리핑: 관심 지표 중 상위 3종의 일간 델타를 한 줄로. 관심 지표가 3개 미만이면 있는 만큼만
- 포트폴리오 요약 카드: `homeSummary.allocationStatus` 로 총 평가액 · 안전/투자 비율 · 밴드 초과 뱃지.
  목표 미설정이면 카드 자체를 숨긴다(목업엔 미설정 상태가 없음)
- 2단 → 1단 전환: `xl` 미만에서 우측 패널이 메인 아래로 떨어진다

### Implementation Steps
- [x] `home.html` 셸 재작성 + partial 2종 신규(빈 셸)
- [x] `app.js` · `index.html` 등록
- [x] `home.js` 에 `homeDashboard` 상태 추가 (`range` · `mode` · `selected[]`)
- [x] 브리핑 계산 헬퍼 + 기준시각 포맷
- [x] 포트폴리오 요약 카드 마크업 이전(기존 요약 카드 4장 중 포트폴리오 카드 로직 재사용)
- [x] 목 하네스 실측 검증

---

## Phase 2 — 지표 비교 보기

### 변경 파일
| 파일 | 변경 |
|---|---|
| `partials/home-indicators.html` | 비교 차트 카드 + 선택 칩 |
| `js/components/home.js` | 선택 토글 · 정규화 · Chart.js 렌더 |

### 규칙
- 최대 3개. 4개째 선택 시 **가장 오래된 선택을 제거**하고 추가(목업 `toggle` 동작과 동일)
- 색은 선택 순서대로 BLUE → RED → AMBER 고정
- 정규화 2모드
  - `pct`: 기간 시작점 기준 `((v - base) / |base|) * 100`, 0% 기준선 표시
  - `raw`: 지표별 min~max 로 `-50 ~ +50` 정규화(모양만 비교), 축 라벨 없음
- 기간(`range`)은 history 뒤에서 N개를 자르는 방식. history 길이가 모자라면 있는 만큼
- 선택 0개면 안내 문구 — "아래 지표를 클릭하면 여기에 겹쳐서 비교됩니다"
- 차트 인스턴스는 재렌더마다 `destroy()` — #110 review H2(차트 누수)와 같은 실수 방지

### Implementation Steps
- [x] 선택 상태 토글 · 칩 렌더 · 개별 해제
- [x] 정규화 2모드 계산 헬퍼
- [x] Chart.js 멀티 데이터셋 렌더 + destroy 관리
- [x] 기간 버튼 연동
- [x] 빈 상태 · history 부족 상태 처리
- [x] 목 하네스 실측 검증

---

## Phase 3 — 표시 모드 폐지 + 지표 카드 그리드 (승인 게이트)

### 승인 필요 사유
`changeFavoriteDisplayMode` 제거 + `reorderFavorites` 시그니처 변경 → **public API 변경**.

### 백엔드 변경

| 대상 | 변경 |
|---|---|
| `FavoriteIndicatorController` | `PUT /api/favorites/display-mode` **제거** |
| `FavoriteIndicatorController` | `PUT /api/favorites/order` — 요청에서 `displayMode` 제거 |
| `FavoriteIndicatorService` | `changeDisplayMode()` 제거, `reorder()` 에서 컨테이너 분리 로직(`computeNewOrder` 인터리브) 단순화 |
| `FavoriteIndicatorService` | `attachEcosHistoryIfGraph` / `attachGlobalHistoryIfGraph` → **무조건 첨부**로 변경 |
| `FavoriteDisplayMode` enum | 제거 |
| `UserFavoriteIndicatorEntity.displayMode` | **컬럼은 남기고 매핑만 제거** (아래 사유) |
| `FavoriteDisplayModeRequest` DTO | 제거 |

**컬럼을 남기는 이유** (DDL 확인 완료): `UserFavoriteIndicatorEntity:41-47` 기준
`display_mode VARCHAR(10) NOT NULL DEFAULT 'INDICATOR'` — **DB 기본값이 있어 매핑만 제거해도 INSERT 가 안전하다.**
따라서 선행 마이그레이션은 필요 없고, 컬럼 DROP 은 후속으로 미룬다(롤백 여지 확보).

**성능**: history 무조건 첨부로 관심 지표 수만큼 시계열 조회가 발생한다.
`HISTORY_LIMIT` 유지하되 **스파크라인용으로는 과한 길이**이므로, 조회 건수 축소 또는 배치 조회를 검토한다.
현재 `attachHistoryToEcos` 가 항목별 `findHistory` 를 도는 구조라 N+1 성격이다 — #110 review M2와 같은 패턴.

### 프론트 변경
| 파일 | 변경 |
|---|---|
| `partials/home-indicators.html` | 관심/글로벌 지표를 단일 카드 그리드로. 그래프/지표 2분할 마크업 제거 |
| `js/components/favorite.js` | `attemptToggleDisplayMode` · `changeFavoriteDisplayMode` 제거, 순서 편집에서 displayMode 제거 |
| `js/api.js` | `changeFavoriteDisplayMode` 제거, `reorderFavorites` 시그니처 변경 |

### 카드 구성
지표명 · 분류(글로벌은 `국가 · 분류`) · 값 · 일간 델타 · 스파크라인 · 기간 변화율 · 기준일 · 별표

- 별표 = 비교 선택 토글(Phase 2 연동). 선택 시 카드 테두리·배경이 선택 색을 따른다
- 카테고리 필터 칩: `전체` + 관심 지표에 실제 존재하는 분류만 동적 생성
- 순서 편집: 드래그. 편집 중에는 필터가 `전체` 로 고정되고 별표 토글이 비활성(목업 동작과 동일)
- **빈 상태**: 관심 지표 0개면 "관심 지표를 추가해 주세요" + 국내/글로벌 지표 메뉴 링크
- **과다 상태**: 그리드는 `auto-fill` 이라 개수 제한 없음. 별표 선택만 3개로 제한

### Implementation Steps
- [x] 백엔드: 엔드포인트·서비스·enum·DTO 정리 (마이그레이션 불필요 — DB DEFAULT 확인 완료)
- [x] 백엔드: history 무조건 첨부 + 조회 비용 조정
- [x] `compileJava` · `compileTestJava` · `test --tests "*Favorite*"`
- [x] 프론트: 카드 그리드 통합 · 스파크라인 SVG · 카테고리 필터 · 순서 편집
- [x] `api.js` · `favorite.js` 정리, 잔여 참조 grep 0건 확인
- [x] 목 하네스 실측 검증

---

## Phase 4 — 키워드 뉴스 통합 엔드포인트 + 우측 패널 (승인 게이트)

### 승인 필요 사유
신규 공개 API 추가.

### 신규 API 제안

```
GET /api/news/feed?userId={id}&size=5
```

응답
```json
{
  "keywordCount": 13,
  "todayCount": 43,
  "items": [
    { "id": 1, "keyword": "원유", "title": "...", "source": "연합뉴스",
      "originalUrl": "...", "publishedAt": "2026-08-17T08:31:00" }
  ]
}
```

- 사용자의 **활성 키워드 전체**를 대상으로 최신순 병합
- `keywordCount` = 활성 키워드 수, `todayCount` = 오늘 수집된 뉴스 건수
- 도메인 포트에 `findByKeywordIds(List<Long>, int size)` 추가
  (현재 `NewsRepository` 는 `findByKeywordId` 단건 · `findAll` 만 있음)
- 키워드 0개면 `items: []` + `keywordCount: 0`

### 우측 패널 3종

**① 확인이 필요한 것** — 프론트 계산, 신규 API 없음
| 알림 | 조건 | 데이터 |
|---|---|---|
| 포트폴리오 배분 밴드 초과 | `bandExceeded` | `homeSummary.allocationStatus` |
| 지표 추세 | history 기준 N회 연속 동일 방향 | `enriched.history` |
| 예금 만기 임박 | 만기일 - 오늘 ≤ 임계 | 포트폴리오 항목 |

- 각 알림은 색(RED/BLUE/AMBER) + 제목 + 본문 + CTA 링크
- 알림 0건이면 패널에 "확인이 필요한 항목이 없습니다"
- 추세 판정 임계(연속 횟수)는 work 단계에서 실데이터 보고 정한다 — 목업은 "3주 연속"

**② 내 키워드 뉴스** — 위 신규 API

**③ 월급 사용 비율** — 도넛 → 스택 바
- `dashboardSummary.salary.spendings` 재사용
- 스택 바 + 항목별 색·비율·금액 목록. 금액 0인 항목 제외
- 지출 없으면 "이번 달 지출 정보가 없습니다"

### Implementation Steps
- [x] `NewsRepository.findByKeywordIds` + JPA 구현체
- [x] `NewsQueryService.getFeedByUser()` + DTO
- [x] `NewsController` 엔드포인트 + `api.js` 메서드
- [x] `compileJava` · `compileTestJava` · `test --tests "*News*"`
- [x] 우측 패널 3종 마크업 + 알림 계산 헬퍼
- [x] 목 하네스 실측 검증

---

## Phase 5 — 기존 섹션 제거 · 정리

### 제거 대상
| 대상 | 위치 | 대체 |
|---|---|---|
| 기능 요약 3카드 | `home.html` 6~123행 | 뉴스 기록·월급은 사이드바 메뉴, 월급은 우측 패널 |
| 요약 카드 4장 | `home.html` 126~193행 | 포트폴리오는 헤더 카드로 이동, 나머지는 사이드바 |
| 최근 업데이트 박스(국내·글로벌) | `home.html` 203~216 / 345~359행 | 지표 카드의 기준일로 대체 |
| 운영자 로그 카드 | `home.html` 97~119행 | `_sidebar.html` admin 메뉴(확인 완료) |

### 함께 정리
- `dashboardSummary.js` — `dashboardSalaryDonut` Chart 생성/파괴 로직. 스택 바로 바뀌므로 차트 코드 제거
- `home.js` — `hasEcosDashboardContent` · `hasGlobalDashboardContent` 등 제거 섹션 전용 헬퍼
- `dashboardSummary.news`(뉴스 기록 요약) — 목업에 자리 없음. **호출 자체를 제거**해 홈 진입 호출 수를 줄인다
- 제거 후 `static/` 전체 grep 으로 잔여 참조 0건 확인

### Implementation Steps
- [x] 마크업 제거
- [x] 죽은 헬퍼·상태·API 호출 제거
- [x] 잔여 참조 grep 0건
- [x] 목 하네스 실측 검증(홈 전체 회귀)

---

## Validation

- `./gradlew compileJava` · `compileTestJava`
- `./gradlew test --tests "*Favorite*" --tests "*News*"`
- 실서버 기동(dev) 후 홈 진입 — 지표 카드·비교 차트·우측 패널 실렌더 확인
- 관심 지표 0개 / 1개 / 다수 상태 각각 확인
- 표시 모드 폐지 후 **기존 사용자 데이터**로 순서 편집·저장이 동작하는지 확인
- 모바일 반응형(2단 → 1단)

## 리스크

- **표시 모드 폐지는 비가역**이다. 저장된 사용자 설정이 무의미해진다 — 태형님 승인 완료
- `display_mode` 는 DB DEFAULT 가 있어 매핑 제거가 안전하다(확인 완료). 다만 **컬럼 DROP 은 이번 범위에서 하지 않는다** —
  #110 V1 에서 CHECK 제약이 `ddl-auto` 로 갱신되지 않아 배포 블로커가 된 전례가 있어, 스키마 변경은 최소화한다
- history 무조건 첨부로 `/api/favorites/enriched` 비용 증가 — N+1 구조 그대로 두면 관심 지표 수에 비례
- 홈 진입 호출 수 증가 → `Promise.allSettled` 병렬 + 부분 실패 흡수 유지
- 목업에 없는 상태(관심 지표 0개, 목표 배분 미설정, 키워드 0개)의 규칙은 이 문서에서 정의한 대로 간다
