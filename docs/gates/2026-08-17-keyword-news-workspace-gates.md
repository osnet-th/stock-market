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

## Stage Log (이어서, 2026-09-24 ~ 09-27)

- 2026-09-24: 세션 재개. 작업이 Phase 6 중간에서 멈춰 있었고 백엔드 51개 파일이 미커밋 상태였다.
  화면(Phase 1)은 미착수라 main 에 반영된 사용자 변화는 없었다
- 실행 순서 변경: plan 은 Phase 1(화면) 선행이었으나, Phase 2 가 바로 지울 임시 코드를 피하려고
  **백엔드(2~7) 완료 후 화면을 한 번에** 만드는 순서로 진행했다
- commit(체크포인트): `873a189` — Phase 2·3·4·5 완료분 + Phase 6 진행분
- main 병합: approved (2026-09-24, 태형님 "main 한번 병합하고 진행하자 혹시 충돌 날 수도 있으니까")
  - `93c14a3` — main 27커밋 병합, **코드 충돌 0건**(내 변경 파일 ∩ main 변경 파일 = 공집합)
  - 병합 판단이 적중: main 이 `app.js`·`api.js`·`custom.css` 를 건드렸고 셋 다 Phase 1 에서 수정할 파일이었다
  - 병합 후 테스트 134건 PASS
- work Phase 6·7: 완료 (`a11afc5`)
  - **설계 결함 발견**: `NewsSearchService` 가 모든 예외를 삼켜 "수집 실패"와 "새 기사 없음"이
    구분되지 않았다. 그대로 두면 연속 실패 판정이 항상 0 인 죽은 기능이 된다
    → 반환형을 `NewsSearchOutcome` 으로 변경(포트 폴백 동작은 유지)
  - `안 읽은 것만` 페이징 정합성은 후처리 필터를 쓰지 않고 양 경로 모두 쿼리 안에서 해결
- work Phase 1(화면): 완료 (`013149d`)
  - 목 하네스 실측 중 **실패 키워드 행의 메타가 2줄로 넘쳐 행 높이가 58→72px 로 어긋나는** 결함 발견.
    레일 텍스트 폭이 117px 뿐이라 목업 문구가 실데이터에서 깨졌다 → 한 줄로 끊고 hover 툴팁에 전체 내용
- review: 완료 (2026-09-24, docs/reviews/2026-08-17-keyword-news-workspace-review.md)
  - H1 1건 · M 3건 · L 2건. 태형님 지시로 **H1 + M1~M3 조치**, L1·L2 보류 (`52e0cca`)
  - H1: 키워드 수정이 **남의 키워드로 기사를 이관**할 수 있었다.
    이관 조건에 "대상 키워드에 다른 구독자 없음" 추가. 양쪽 다 불가한 경우는 수정 자체를 막는다
- validation: 완료 (2026-09-24 ~ 09-27, docs/validations/2026-08-17-keyword-news-workspace-validation.md)
  - 실 PostgreSQL 검증 중 **제품 버그 발견** (`ec5ab4c`) — `findLatestByScope` 가
    `could not determine data type of parameter $2` 로 실패. 기본 화면 스트림이 통째로 죽는 상태였다.
    단위 테스트는 목이라 SQL 미실행, 컨텍스트 테스트는 파싱만 검증해 둘 다 놓쳤다
  - ES 검증 승인 (2026-09-27, 태형님 "3GB 말고 500MB 로 안 되냐" → 힙 512m/컨테이너 1GB 로 합의).
    실측 928MB/1GB 로 500MB 는 불가능했을 것이다. `docker-compose.yml` 무수정, 검증 후 컨테이너 제거
  - ES 검증 중 **기존 버그 발견** — `news-settings.json` 의 `E`·`J` 태그가 Lucene 10 에서 제거돼
    뉴스 인덱스 생성 자체가 실패. 태형님 승인 후 이번 작업에 포함해 수정 (`aaa1320`)
  - DB 5항목 · ES 5항목 전건 PASS, 전체 테스트 137건 PASS
- pr: 완료 (2026-09-27, https://github.com/osnet-th/stock-market/pull/123)
  - 82 files, +5,210/−717
- merge: **미승인 — 태형님 확인 대기**

## Approval Gate 항목 (추가분)
- **`news-settings.json` stoptags 수정** — 이번 작업 범위 밖의 기존 버그.
  **태형님 승인 완료 (2026-09-27, ES 검증 중 보고 후 "1" = 이번 작업에 포함해 수정)**
- **검증용 ES 컨테이너 기동** — 머신 변경. **태형님 승인 완료 (2026-09-27)**. 검증 후 컨테이너 제거, 이미지는 보존

## 잔여 처리 항목 (갱신)
- **운영 ES 인덱스 상태 확인 필수** — `GET /news/_settings`.
  인덱스가 없으면 그동안 뉴스 검색이 동작하지 않았던 것이고,
  있으면 옛 stoptags 기준이라 **재색인이 필요**하다
- 유사 기사 클러스터링 — 별도 이슈
- `news.source` 기존 데이터 백필
- 실 DB 쿼리 스모크 테스트 허용 여부 — 이번에 잡은 결함 2건은 목 기반 단위 테스트로 잡히지 않는다.
  저장소 정책이 통합 테스트를 두지 않아 검증 테스트는 삭제했고, 현재 회귀 가드가 없다
- review L1(벌크 실패 원인 미표기) · L2(todayTotal 이 레일 필터와 무관) 보류분
