# 뉴스 기록 마스터-디테일 리디자인 Work

gate: docs/gates/2026-08-21-news-journal-redesign-gates.md
plan: docs/plans/2026-08-21-001-feat-news-journal-redesign-plan.md

## Phase 1 — 백엔드 (완료)

변경/신규 파일:
- `NewsEventListFilter` — `query`/`keywords` 추가. blank→null, null→빈 리스트 정규화,
  trim·distinct, `KEYWORDS_MAX=10` 상한 (초과 시 IllegalArgumentException → 400)
- `NewsEventJpaRepository.findList/countList` — `qLike`(제목/WHAT/WHY/HOW LIKE + 키워드/분류명
  EXISTS, `!` escape) + `keywords` AND(`COUNT(DISTINCT) = :keywordCount` 서브쿼리) 확장,
  `countByImpactGroupedByImpact` 신규
- `NewsEventRepositoryImpl` — LIKE 패턴 생성(`toLikePattern`, `%`/`_`/`!` escape),
  **빈 IN 리스트 렌더링 회피**: 비활성 시 절대 매칭 불가 sentinel(`""`) 1건 + `keywordCount=0`
- `NewsEventKeywordJpaRepository.findRowsByUserId` — keyword ⋈ event 조인 projection
  (`NewsEventKeywordRow(eventId, occurredDate, keyword)`), userId 스코프 강제
- `NewsEventReadService.findStats` — 임팩트 건수(0건 포함)/분류 건수(0건 포함)/사건별 키워드.
  컨벤션(작은 메서드) 준수 위해 `impactCountsOf`/`categoryCountsOf`/`keywordEventsOf` 로 분리
- `NewsJournalStatsController`(`GET /api/news-journal/stats`) + `NewsJournalStatsResponse`
- `NewsJournalExceptionHandler` — advice 대상에 stats 컨트롤러 추가
- 신규 domain record: `NewsEventImpactCount`, `NewsEventKeywordRow`
- Entity/스키마 변경 없음, 마이그레이션 없음

## Phase 2 — 프론트 (완료)

- `partials/news-journal.html` 전면 교체 (302 → 605줄) — 목업 구조 이식, 앱 셸 유지
  - 마스터-디테일 + 인라인 보기/편집/빈선택 (template x-if 3분기), 모달 제거
  - 헤더: 통합 검색(300ms 디바운스)·임팩트 세그(전체/호재/악재/중립+건수)·분류 드롭다운
    (검색·건수·✓)·기간 토글·키워드 검색(초성)·상위 칩·+N개 더 패널(추천/가나다)·뷰 토글
  - 목록: 월별/분류별 그룹(sticky), 행(뱃지·분류·날짜·🔗·제목·미리보기·태그3), 선택 하이라이트,
    더 불러오기(남은 건수), 빈 상태+필터 초기화
  - 디테일: WHAT/WHY/HOW 불릿 섹션(빈 섹션 "작성되지 않음"), 관련 기사(host·↗),
    키워드 칩(클릭 시 필터), 같은 분류의 다른 기록 3건
  - 편집: 목업 폼 그대로 (제목*/일자*(max=today)/시장영향*/분류 칩+새 분류 Enter/
    WWH(힌트·글자수·placeholder)/기사(제목 선택+URL 형식 경고)/키워드), dirty 안내
  - 관계도: SVG 엣지(x-html, 숫자만 조립) + 절대배치 노드 버튼(원 크기=기록 수),
    hover/선택 하이라이트, 자주 겹치는 짝 → 2키워드 필터
  - `data-nj` 테스트 앵커 24종 부여 (하네스/후속 실측용)
- `components/news-journal.js` 전면 교체 (335 → 831줄)
  - 서버 필터 리셋 로드 + 증분 로드(100건), stats 로드, CRUD 후 목록+통계 동시 재조회
  - 키워드 빈도/동시등장/추천/가나다/관계도 좌표 — 목업 산식 이식, stats 참조 기준 메모이즈
  - 초안: localStorage `newsJournalDraft:{userId}` 단일 보관, stash(새 기록만)/이어쓰기/저장 시 소멸
  - 선택 스냅샷(`sel`): 필터로 목록에서 빠져도 디테일 유지, 저장 후 필터 밖이면 단건 조회로 유지
  - ESC: 팝오버 → 편집 stash 순, 좁은 화면(<1280) 단일 pane 전환
- `api.js` — `getNewsJournalStats()` 추가 (기존 `_buildLogQuery` 가 배열 반복 파라미터 지원)
- `custom.css` — `.nj-*` 반복 패턴 클래스 (#114 `.dc-*` 관례 준수, `--dc-*` 토큰 재사용)
- `index.html`/`app.js` 변경 없음 (partial 등록명 동일)

## 실행 중 발견 (중요)

- **Alpine 문자열 `:style` 바인딩이 style 속성을 통째로 대체** — `setStylesFromString` 이
  `setAttribute('style', value)` 로 동작해 (1) 같은 요소의 정적 `style` 속성이 지워지고
  (2) `x-show` 가 넣은 `display:none` 도 다른 반응값(vw) 변경 시 함께 지워져
  **닫힌 태그 패널이 리사이즈 후 다시 나타나 목록 클릭을 가로채는 실버그**가 됐다.
  → 문자열 바인딩 35곳 전부 **객체 바인딩**(`:style="{ ... }"`) 으로 전환
  (`setStylesFromObject` 는 자기 키만 만지고 되돌려 정적 style·x-show 와 안전하게 합성).
  목 하네스 실측으로 발견 — 코드 리뷰만으로는 놓쳤을 유형.
- 목업 '약재' 표기는 오타로 판단 → 기존 '악재'(BAD) 유지 (brainstorm 결정 1 그대로)
- 좁은 화면 임계는 목업 1080 → **1280** (앱 사이드바 224px 보정), 칩 TOPN 임계 1400/1660 동일 보정
- 태그 패널 고정 스타일은 정적 속성으로 분리, 너비만 동적 바인딩

## 검증 (Phase 3 요약 — validation 문서 상세)

- `compileJava`/`compileTestJava`/`test` PASS (로컬 PG 기동 후 전체)
- 목 하네스 61항목 PASS + 실서버 통합 11항목 PASS
- `node --check` 2파일 PASS
