# 키워드 뉴스 워크스페이스 통합 Issue 기록

gate: docs/gates/2026-08-17-keyword-news-workspace-gates.md

## GitHub Issue
- status: **created**
- issue_number: 115
- issue_url: https://github.com/osnet-th/stock-market/issues/115
- title: `[enhancement] 키워드 뉴스 워크스페이스 통합 — 키워드·뉴스 검색 단일 화면 통합·통합 검색·키워드 CRUD·수집 이력`
- label: enhancement

### 등록 실패 이력 (2026-08-17) — 4회차에 성공
- `gh issue create` 1~3회차 → `HTTP 503 No server is currently available`
  - 1·3회차: `error fetching labels: non-200 OK status code: 503` (label 조회 단계에서 실패)
  - 2회차: `HTTP 503 ... (https://api.github.com/graphql)`
- `githubstatus.com`: `indicator: major` / `Partial System Outage`
- **원인은 외부 장애**이며 저장소·권한 문제가 아니었다 (`gh auth status` 정상)
- 대기하는 대신 plan 문서를 먼저 작성하고 4회차에 재시도 → **성공**.
  (status 는 여전히 `major` 였으나 issue 생성 경로는 복구됨)
- 이슈 번호를 추측하지 않고 실제 등록 후 브랜치·worktree 를 만들었다

## 근거
- brainstorm: docs/brainstorms/2026-08-17-keyword-news-workspace-brainstorm.md (Status: Decided)
- 태형님이 목업 `~/Downloads/키워드 뉴스 워크스페이스 (단일파일).html` 제시 —
  "이거 html 보고 그대로 똑같이 키워드랑 뉴스 검색 부분 통합하는 디자인으로 진행해줘"
- 목업 v2 재제출(2026-08-17 23:29) — "이거 보고 수정 삭제, 활성화 비활성화도 넣엇으니까 확인해"
- 결정 회신 5건:
  1. 범위 → **C (목업 100% 재현)**
  2. 메뉴 → **`키워드`·`뉴스 검색` 둘 다 제거하고 `키워드 뉴스` 하나로**
  3. 키워드 CRUD → **목업 v2 디자인** (케밥 메뉴 · 수정 모달 · 삭제 확인 모달 · 벌크)
  4. 키워드 수정 시맨틱 → **A. 재구독 방식**
  5. `국내+해외` → **C. 옵션 제거 (2개로)**
  6. 유사 기사 클러스터링 → **"빼고 진행해" (범위 제외, 별도 이슈)**

## Branch
- branch: `feat/issue-115-keyword-news-workspace`
- base: main (`cfc14fb`)
- worktree: `/Users/tang/Documents/workspace/wt-issue-115-feat-issue-115-keyword-news-workspace`
- 생성 명령: `scripts/create-worktree.sh --issue 115 feat/issue-115-keyword-news-workspace`
- `.env` 는 primary worktree 에서 복사됨

## 작업 범위 요약 (7 Phase)
1. 통합 화면 셸 — 2단 레이아웃 · 통합 검색창 · 필터 바 · 키워드 레일 · 날짜별 뉴스 스트림
2. 검색 확장 — 빈 검색어 허용 · 키워드 스코프(다중) · 제목만/제목+본문 · 최신/관련도 (public API 변경)
3. 키워드 통계 API — 총 건수 · 오늘 건수 · 마지막 성공 · 7일 스파크라인 (신규 API)
4. 키워드 CRUD — 케밥 메뉴 · 수정(재구독) · 삭제 확인 · 벌크 중단/삭제 (신규 API + 로직 변경)
5. 언론사(`source`) — `NewsEntity` 컬럼 + 수집 어댑터 3종 매핑 (Entity 수정)
6. 수집 이력 + 스케줄 상태 — 연속 실패 판정 · 마지막/다음 실행 (Entity 신규)
7. 읽음/저장 — 상태 저장 + "안 읽은 것만" 필터 · ☆저장 (Entity 신규)

## 범위 제외
- **유사 기사 클러스터링** — 별도 이슈. 동반해서 기사 카드 `유사 기사 N건` 버튼 · 등록 모달 안내 문구 박스 미렌더
- **일괄 활성화(재개)** — 목업 로직에 `bulkOn` 이 있으나 버튼이 없어 범위 밖

## 참고 자료
- 목업: `~/Downloads/키워드 뉴스 워크스페이스 (단일파일).html` (v2 — CRUD 포함)
- 추출물: 스크래치패드 `kw_template_v2.html`(316KB) · `kw_body_v2.html`(48.6KB)
- 선행 이슈: #114 (홈 대시보드 리디자인 — `--dc-*` 토큰·IBM Plex 폰트를 이 목업과 동일하게 도입해둠)
