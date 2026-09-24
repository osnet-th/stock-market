# 키워드 뉴스 워크스페이스 통합 게이트 로그

## 원칙
- 각 단계 시작 전 태형님에게 작업 내용을 제시한다.
- 다음 단계로 넘어갈지에 대한 판단은 태형님에게 넘긴다.
- 단계별 산출물은 이 게이트 로그를 `gate: docs/gates/2026-08-17-keyword-news-workspace-gates.md`로 참조한다.

## Stage Decisions
- start: approved (2026-08-17, 태형님이 목업 `키워드 뉴스 워크스페이스 (단일파일).html` 제시하며
  "이거 html 보고 그대로 똑같이 키워드랑 뉴스 검색 부분 통합하는 디자인으로 진행해줘")
- brainstorm: approved (2026-08-17, 결정 3건 회신 — 범위 **C(목업 100%)**, 메뉴 **둘 다 없애고 통합**,
  키워드 CRUD 는 **목업 v2 로 회신** — "이거 보고 수정 삭제, 활성화 비활성화도 넣엇으니까 확인해")
- issue: approved (2026-08-17, 태형님 "진행해" → GitHub Issue #115 등록,
  worktree `feat/issue-115-keyword-news-workspace` 생성)
- plan: **작성 완료, 착수 승인 대기**
- work: pending
- review: pending
- validation: pending
- commit: pending
- push: pending

## Stage Log
- 2026-08-17: 태형님이 목업 제시. #114 와 같은 번들 HTML(React + `x-dc` 템플릿)이라
  manifest/template 스크립트를 추출해 현재 화면과 대조
  - 원본 5.9MB / 390줄 (line 376 = manifest 5.6MB, line 388 = template 316KB)
  - 추출물: 스크래치패드 `kw_template.html`(306KB), `kw_body.html`(38KB — 마크업 + 로직 13KB)
  - 대조 대상: `partials/keywords.html`(206줄) · `partials/news-search.html`(99줄) ·
    `js/components/keyword.js`(74) · `news.js`(68) · `news-search.js`(75)
- brainstorm: 문서 작성 완료 (2026-08-17, docs/brainstorms/2026-08-17-keyword-news-workspace-brainstorm.md)
- 2026-08-17: Open Questions 3건 태형님 회신
  1. 범위 → **C. 목업 100% 재현** (수집 이력 · 유사 기사 · 언론사 · 읽음/저장 전부 신설)
  2. 메뉴 → **`키워드`·`뉴스 검색` 둘 다 제거하고 `키워드 뉴스` 하나로** (13개 → 12개)
  3. 키워드 CRUD 배치 → 최초 회신은 "이부분 디자인해서 다시 줄게"(보류)
- 2026-08-17 23:29: 태형님이 **같은 파일을 갱신해 재제출** (5,976,756B → 5,988,093B, 마크업 622 → 752줄)
  - 추출물: `kw_template_v2.html`(316KB), `kw_body_v2.html`(48.6KB). v1 과 diff 로 추가분만 확인
  - 추가된 것: 레일 `⋯` 케밥 메뉴(4항목) · 벌크 액션(중단·삭제) · 수정 모달 · 삭제 확인 모달 ·
    `KW` 상수 → `st.items` 상태 전환
  - **미사용 값 발견**: `bulkOn`(일괄 활성화)이 로직에만 있고 버튼이 없음 → 일괄 재개는 범위 밖으로 판단
  - **목업 오타**: 삭제 모달 `함게` → 구현 시 `함께`
  - **다건 삭제 확인 모달은 목업에 미정의** — 벌크 삭제용으로 단건 모달을 확장해야 함
- 2026-08-17: 목업 v2 대조 중 **백엔드 충돌 2건 발견 → 태형님 회신 완료**
  1. `이름 · 수집 범위 수정` — `Keyword` 가 **공유 리소스**(클래스 주석 명시)이고 변경 메서드가 없음.
     UPDATE 로 구현하면 다른 구독자까지 바뀜
     → **(A) 재구독 방식 확정.** `Keyword` 불변 유지, 단독 구독자면 `news.keyword_id` 이관,
     다중 구독자면 구독만 해제 + 모달 문구 조건부
  2. 등록 모달의 `국내+해외` — `Region` enum 에 없음(DOMESTIC/INTERNATIONAL 2개)
     → **(C) 옵션 제거 확정.** 등록 모달을 국내·해외 2버튼으로 맞춤. enum·수집 포트 팩토리 무변경
- 2026-08-17: 태형님 "유사 기사 클러스터링은 뭐야?" → 설명 후 **"빼고 진행해"**
  → **유사 기사 클러스터링 범위 제외 확정.** 별도 이슈로 분리. Phase 8개 → **7개**
  - 제외 근거: 목업 `dupes` 가 고정 데이터라 알고리즘 미정의. 임계값을 실데이터로 튜닝해야 확정됨
  - **동반 UI 차이 2곳**: 기사 카드 `유사 기사 N건` 버튼 미노출 · 등록 모달 안내 문구 박스 제거
    (문구가 "자동으로 묶여서 대표 1건만 보이고…" 라 기능 없이 두면 사실과 다른 안내가 됨)

- issue: 완료 (2026-08-17, https://github.com/osnet-th/stock-market/issues/115, label: enhancement)
  - **GitHub 장애로 3회 실패** — `HTTP 503` / `githubstatus: major, Partial System Outage`.
    대기하지 않고 plan 문서를 먼저 작성한 뒤 4회차 재시도로 성공
  - 이슈 번호를 **추측하지 않고** 실제 등록 후 worktree 생성 (스크립트는 `--issue` 를 검증하지 않으므로
    추측했다면 브랜치·경로가 어긋났을 것)
  - worktree: `scripts/create-worktree.sh --issue 115 feat/issue-115-keyword-news-workspace`
    → `/Users/tang/Documents/workspace/wt-issue-115-feat-issue-115-keyword-news-workspace` (base `cfc14fb`, .env 복사됨)
- plan: 문서 작성 완료 (2026-08-17, docs/plans/2026-08-17-002-feat-keyword-news-workspace-plan.md)
  - **7 Phase 분할**: ①통합 셸 ②검색 확장 ③키워드 통계 ④CRUD ⑤언론사 ⑥수집 이력 ⑦읽음/저장
  - Phase 1 만 프론트 전용, **Phase 2~7 은 각각 착수 전 개별 승인 필요**
  - **작성 중 발견한 함정**: `KeywordServiceImpl.unsubscribeKeyword()`·`deactivateUserKeyword()` 가
    `disablePortfolioNewsByKeywordId()` 로 **키워드 이름과 같은 포트폴리오 항목의 `newsEnabled` 를 OFF** 한다
    (이름 기반 매칭). 재구독 방식 rename 이 이 로직을 타면 **요청하지 않은 포트폴리오 연동 해제**가 발생
    → Phase 4 착수 시 처리 방식(연동 이관 vs 수정 경로에서 건너뛰기) 확정 후 게이트 기록
  - **확정 사항**: 빈 상태 3종 정의(목업 미정의) · 다건 삭제 확인 모달 확장(목업 미정의) ·
    `안 읽은 것만`+ES 페이징 정합성은 Phase 7 착수 시 확정 · 수집 이력 보존 정책은 Phase 6 착수 시 확정
  - **놓치기 쉬운 연쇄 변경 식별**: `home-side.html` 의 `navigateTo('keywords')` 2곳
    (안 고치면 홈 우측 패널이 죽은 링크가 된다)

## Approval Gate 항목
- **Entity 신규 2~3개** — 수집 이력(연속 실패 판정) · 읽음/저장 상태.
  **범위 C 승인으로 방향은 확정 (2026-08-17)**. 개별 스키마는 plan에서 확정 후 Phase 착수 전 재확인
- **`NewsEntity` 컬럼 추가 (`source`)** — Entity 수정. 수집 어댑터 3종 매핑 변경 동반.
  **범위 C 승인에 포함 (2026-08-17)**. 마이그레이션·기존 null 폴백은 plan에서 확정
- **`GET /api/news/search` 시그니처 변경** — `query` `@NotBlank` 완화 + `keywordIds`·`field`·`sort` 추가.
  public API 변경 → plan 확정 후 Phase 착수 전 승인 필요
- **신규 공개 API 3종** — 키워드 통계 · 스케줄 상태 · 읽음/저장 토글.
  신규 공개 API → plan 확정 후 Phase 착수 전 승인 필요
- **기존 두 메뉴 제거** — 화면 구성 변경. **태형님 승인 완료 (2026-08-17, "둘 다 없애고 하나로 통합")**
- **키워드 수정 API 신규 + 수정 시맨틱** — 신규 공개 API 이자 **비즈니스 로직 동작 변경**.
  **태형님 승인 완료 (2026-08-17, "A. 재구독 방식")**. 엔드포인트 형태·`news` 이관 쿼리는 plan에서 확정
- **벌크 중단/삭제 API** — 신규 공개 API. 목업 v2 에 포함. 다건 확인 모달 문구는 plan에서 확정
- **`국내+해외` 처리** — **태형님 승인 완료 (2026-08-17, "C. 옵션 제거")**. `Region` enum 무변경 확정

## 확인 사실 (대조 과정에서 검증)
- 디자인 토큰은 **추가 도입 불필요** — #114 에서 `css/custom.css` 의 `--dc-*` 로 목업과 동일한 팔레트
  (`#eef0f3` / `#1f4f9e` / `#c02a22` / IBM Plex)가 이미 도입됨
- `GET /api/news/search` 의 `query` 는 `@NotBlank` → **검색어 없는 열람이 현재 400**.
  목업 기본 화면이 "저장된 전체 뉴스"라 반드시 완화해야 함
- ES `NewsDocument` 에 **`keywordId` 가 이미 있다** → 키워드 스코프 검색은 필터 추가만으로 가능
- **ES 검색 결과에 뉴스 id 가 없다** — `_id` = `originalUrl`(`NewsDocumentMapper:13`),
  `toNews()` 가 `new News(null, …)`(`NewsElasticsearchSearcher:107`).
  읽음/저장·유사 기사를 붙이려면 재색인 or `originalUrl` 로 DB 재조회 필요 → **후자 권고**
- 언론사는 `NaverNewsItem`·`GNewsArticle`·`NewsApiArticle` **3종 DTO 전부 미매핑**.
  Naver 는 API 응답에 언론사가 없어 `originallink` 도메인 파생이 유일
- `countByKeywordIdInAndCreatedAtGreaterThanEqual`(#114 추가)은 **합계만** 반환 → 키워드별 분해 필요
- 수집 이력 테이블이 없어 **연속 실패 · 마지막 실행 시각을 현재 알 수 없다**
- 수집 스케줄 cron 은 `batch.schedule.economics-sync-cron:0 0 * * * *` (매시 정각)
- **`Keyword` 는 공유 리소스** — `registerKeyword` 가 `findByKeywordAndRegion().orElseGet(create)` 로
  같은 (이름, 지역) 을 여러 사용자가 공유. 사용자별 상태는 `UserKeyword.active` 에만 있음.
  `Keyword` 도메인 모델에 **이름·지역 변경 메서드가 없다**(불변)
- `unsubscribeKeyword()` 는 마지막 구독자가 떠나면 `news` 까지 삭제 →
  삭제 확인 모달의 "기사 N건도 함께 사라집니다" 문구와 **단독 구독자 케이스에서 일치**
- `Region` enum 은 `DOMESTIC`·`INTERNATIONAL` 2개뿐 → 목업 등록 모달의 `국내+해외` 는 백엔드에 없음
- 키워드 수정/벌크 엔드포인트는 `KeywordController` 에 **없음** (register/get/activate/deactivate/delete 5개만)

## 잔여 처리 항목
- **plan 착수 승인 대기** — Phase 1 부터 진행할지 태형님 확인
- **Phase 4 착수 전 확정 필요** — 포트폴리오 `newsEnabled` 처리 방식(연동 이관 vs 건너뛰기)
- **후속 이슈 후보**: 유사 기사 클러스터링(이번 범위 제외) · `news.source` 기존 데이터 백필
- #114 잔여: GitHub Issue #114 종료 여부, worktree `wt-issue-114-...` 정리

## Notes
- 목업 로직은 고정 데이터(키워드 14개 · 기사 10건) 기준이라 키워드 0개/과다, 기사 0건 레이아웃이 미정의
- 목업 반응형 기준선: `vw < 1120` 이면 레일이 스트림 아래로 내려가고 레일 목록 높이가 300px 로 제한
