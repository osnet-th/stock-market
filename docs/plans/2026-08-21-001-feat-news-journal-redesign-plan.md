# 뉴스 기록 마스터-디테일 리디자인 Plan

gate: docs/gates/2026-08-21-news-journal-redesign-gates.md
brainstorm: docs/brainstorms/2026-08-21-news-journal-redesign-brainstorm.md

## 목표

목업(업로드 `b18ca2f9-*.html`)대로 뉴스 기록 화면을 마스터-디테일 구조로 재구성하고,
목업이 요구하는 통합 검색·키워드 필터·화면 통계를 백엔드에 추가한다.

## 범위 밖

- Entity/스키마 변경, 마이그레이션 (불필요 확인)
- 사건 CRUD API 시그니처 변경 (기존 유지)
- ES 기반 전문 검색 (LIKE 로 충분)
- 대시보드 요약 카드(`NewsJournalDashboardController`) 변경

## Phase 1 — 백엔드: 리스트 필터 확장 + stats API

- [x] `NewsEventListFilter` 에 `query`(nullable), `keywords`(nullable→빈 리스트 정규화) 추가
- [x] `NewsEventJpaRepository.findList/countList` JPQL 확장
  - `q`: 소문자 LIKE — 제목/WHAT/WHY/HOW + 키워드 EXISTS + 분류명 EXISTS, `!` escape
  - `keywords`: `COUNT(DISTINCT k.keyword) = :keywordCount` 서브쿼리로 AND 매칭
- [x] `NewsEventRepositoryImpl` 에서 LIKE 패턴 생성(escape) 및 파라미터 바인딩
- [x] 임팩트별 건수 쿼리 `countByImpactGroupedByImpact` + `NewsEventImpactCount` record
- [x] 키워드 행 조회 `NewsEventKeywordRepository.findRowsByUserId` + `NewsEventKeywordRow`
  (keyword ⋈ event, userId 스코프, occurredDate 포함)
- [x] `NewsEventReadService.findStats` — totalCount/임팩트 건수/분류 건수(0건 포함)/사건별 키워드
- [x] `GET /api/news-journal/stats` — `NewsJournalController` 와 분리된 매핑 유지 여부 결정
  → `NewsJournalStatsController` 신설 대신 **기존 `NewsJournalDashboardController` 와 동급인
  화면 전용 컨트롤러로 `NewsJournalController` 에 두지 않고 `/api/news-journal/stats` 전용
  `NewsJournalStatsController`** 를 만든다 (events 매핑과 경로 충돌 방지, 기존 관례 따름)
- [x] Response DTO `NewsJournalStatsResponse`

## Phase 2 — 프론트: 화면 전면 리라이트

- [x] `api.js` — `getNewsJournalStats()` 추가, `getNewsEvents` 는 기존 `_buildLogQuery` 가
  배열 반복 파라미터를 지원하므로 호출부에서 `q`/`keywords` 전달만 추가
- [x] `partials/news-journal.html` 전면 교체 (목업 구조 이식, 앱 셸 유지)
  - 헤더: 제목·건수, 초안 이어쓰기 칩, + 새 사건 기록
  - 필터 행 1: 통합 검색, 시장영향 세그먼트(건수), 분류 드롭다운(검색·건수·✓), 기간 토글+범위
  - 필터 행 2: 키워드 검색(초성), 상위 칩, +N개 더 패널(추천/가나다), 뷰 토글(목록/관계도)
  - 선택 키워드 바 (AND 문구, 개별 해제, 전체 해제)
  - 목록 pane: 결과 문구, 월별/분류별 토글, 그룹(sticky)·행(뱃지/분류/날짜/🔗/제목/미리보기/태그),
    빈 상태, 더 불러오기
  - 디테일 pane: 보기(뱃지·제목·수정/삭제·WHAT/WHY/HOW 불릿·관련 기사·키워드·같은 분류 3건),
    편집(제목/일자/시장영향/분류 칩+새 분류/WWH/기사/키워드/저장·닫기·dirty 안내), 빈 선택 상태
  - 관계도 pane: SVG 엣지 + 노드 버튼, 안내 문구, 자주 겹치는 짝, 목록으로 보기
  - 좁은 화면: 단일 pane 전환 + 백 버튼
- [x] `components/news-journal.js` 전면 교체
  - 상태: recs(로드분)/stats/selId/mode/draft/필터(q·impact·categoryId·기간·tagSel)/그룹/뷰/패널
  - 서버 연동: load(필터 변경 시 리셋 로드, q 300ms 디바운스), loadMore, stats 로드, CRUD 후 재로드
  - 초안: localStorage 사용자별 키, stash/resume/저장 시 소멸, ESC 처리
  - 키워드 계산: freq/pairs/추천 섹션/초성 매칭/가나다 그룹/관계도 좌표 (목업 산식 이식)
  - 관계도: 노드 22개 내림차순, 내곽 6 + 외곽 타원 배치, hover/선택 하이라이트
- [x] `index.html`/`app.js` 등록 변경 없음 확인 (partial 이름 동일)

## Phase 3 — 검증

- [x] `./gradlew compileJava compileTestJava test`
- [x] `node --check` (news-journal.js, api.js)
- [x] Playwright 목 하네스 — API 스텁으로 화면 실측
  (마운트/검색/세그먼트/드롭다운/기간/키워드 AND/패널/관계도/그룹 전환/선택·디테일/편집·저장/
  초안 stash·이어쓰기/더 불러오기/빈 상태/좁은 화면 전환)
- [x] `scripts/check-documented-workflow.sh` 통과 확인

## 승인 게이트 대상 항목

- **신규 공개 API** `GET /api/news-journal/stats` + `q`/`keywords` 파라미터 추가 —
  원격 세션 특성상 태형님 개별 승인 불가 → 요청문("부족한 기능은 백엔드 코드 추가 또는 수정")을
  포괄 승인으로 해석해 진행, gates 문서에 명시
- Entity 변경 없음 → Entity 게이트 해당 없음

## 리스크 / 완화

- JPQL 빈 IN 리스트: Hibernate(Boot 4.x) 가 `1=0` 으로 렌더링하나, `:keywordCount = 0` 가드가
  선행해 결과에 영향 없음. 검증 단계에서 테스트로 확인.
- 화면 전면 교체: 기존 함수명(`newsJournal*`)이 사라짐 — 외부 참조가 partial 내부뿐인지 grep 확인.
- `dashboardSummary` 등 타 화면은 `/api/news-journal/dashboard/summary` 만 사용 → 영향 없음.
