# 홈 대시보드 경제 대시보드형 리디자인 게이트 로그

## 원칙
- 각 단계 시작 전 태형님에게 작업 내용을 제시한다.
- 다음 단계로 넘어갈지에 대한 판단은 태형님에게 넘긴다.
- 단계별 산출물은 이 게이트 로그를 `gate: docs/gates/2026-08-17-home-dashboard-redesign-gates.md`로 참조한다.

## Stage Decisions
- start: approved (2026-08-17, 태형님이 목업 `경제 대시보드 리디자인 (단일파일).html` 제시하며 "이거대로 현재 화면 변경 해줄래")
- brainstorm: approved (2026-08-17, 결정 3건 회신 — "표시 모드 없애고, 키워드 뉴스는 통합 엔드포인트로 만들자, 목업대로 없애줘")
- issue: approved (2026-08-17, GitHub Issue #114 등록, worktree `feat/issue-114-home-dashboard-redesign` 생성)
- plan: approved (2026-08-17, 태형님 "진행해" — 5 Phase 분할 승인, Phase 1부터 착수)
- work: Phase 1~5 완료
- review: approved (2026-08-17, 태형님 "리뷰 진행해" → findings 제시 → "수정안대로 바꿔줘" 로 조치 승인)
- validation: approved (2026-08-17, 태형님 "진행해" — 실서버 기동·로그인·검증 데이터 등록까지 승인)
- commit: approved (2026-08-17, 태형님 "맞아 커밋해")
- push: approved (2026-08-17, 태형님 "진행해" — 브랜치 푸시 + main 병합 + main 푸시)

## Stage Log
- 2026-08-17: 태형님이 목업 제시. 번들 HTML(React + `x-dc` 템플릿)이라 템플릿·로직 스크립트를 추출해 현재 `home.html`(496줄)과 대조
  - 추출물: 스크래치패드 `econ_design.html`(템플릿 296KB), `econ_app.jsx`(로직 13KB)
- brainstorm: 문서 작성 완료 (2026-08-17, docs/brainstorms/2026-08-17-home-dashboard-redesign-brainstorm.md)
- 2026-08-17: Open Questions 3건 태형님 회신
  1. 표시 모드(displayMode) **폐지** — 모든 카드를 스파크라인 한 형태로 통일
  2. 키워드 뉴스 **통합 엔드포인트 신설** — 프론트 N회 호출 대신 백엔드 집계
  3. 기존 홈 섹션 **목업대로 제거** — 기능 요약 3카드 · 요약 카드 4장 · 최근 업데이트 박스
- issue: 완료 (2026-08-17, https://github.com/osnet-th/stock-market/issues/114, label: enhancement)
  - worktree 생성: `scripts/create-worktree.sh --issue 114 feat/issue-114-home-dashboard-redesign` (base main, .env 복사됨)
- plan: 문서 작성 완료 (2026-08-17, docs/plans/2026-08-17-001-feat-home-dashboard-redesign-plan.md)
  - 5 Phase 분할: ①셸·헤더 ②비교 보기 ③표시 모드 폐지·카드 그리드 ④키워드 뉴스 API·우측 패널 ⑤기존 섹션 제거
  - Phase 3~4 는 public API 변경 → Phase 착수 전 개별 승인 필요
  - 확인 사실: `display_mode` 는 `NOT NULL DEFAULT 'INDICATOR'` → 매핑 제거해도 INSERT 안전, 선행 마이그레이션 불필요
  - 확인 사실: `home.html` 496줄 + 목업 구조면 900줄 초과 → partial 분리
- work Phase 1: 완료 (2026-08-17, docs/works/2026-08-17-home-dashboard-redesign-work.md)
  - partial 2종 신규 + `home.html` 셸 축소(496 → 256줄) + `home.js` 헬퍼 10종 + `app.js`/`index.html` 등록
  - 목 하네스 실측 12항목 PASS (마운트·헤더·브리핑·기간·모드·포트폴리오 카드·2단/1단 전환·빈 상태 4종)
  - **실행 중 확정**: 2단 그리드 래퍼를 `index.html` 에 배치 — `PartialLoader` 가 부트 시 host 를 일괄 `querySelector` 해서
    `home.html` 내부에 host 를 중첩하면 마운트 전이라 잡히지 않음
  - 기존 섹션(기능 요약·요약 카드·최근 업데이트)은 Phase 5 까지 유지 — 중간 Phase 에서 홈이 반쪽이 되는 것 방지
  - Phase 2 진입 승인 (2026-08-17, 태형님 "진행해")
- work Phase 2: 완료 (2026-08-17, docs/works/2026-08-17-home-dashboard-redesign-work.md)
  - 비교 보기 카드(칩·차트·빈 상태) + `home.js` 비교 헬퍼 9종 + `cleanupRegistry` 에 `home-indicators` 추가
  - 목 하네스 실측 14항목 PASS (3개 제한·색 순서·정규화 2모드·base 0 폴백·기간 반영·차트 누수·빈 상태·카드 토글)
  - **실행 중 확정**: 주기가 다른 지표(일별 vs 월별)는 인덱스로 붙이면 짧은 쪽이 왼쪽에만 그려져 시점이 어긋남
    → 목업과 같이 전 구간 리샘플링, 대신 툴팁은 공유 라벨 대신 각 지표의 실제 날짜 사용
  - **실행 중 발견**: `history` 가 빈 카드는 선택해도 칩·선이 안 생겨 무반응처럼 보임 → 비교 버튼 `:disabled` 처리.
    현재 백엔드가 GRAPH 모드에만 history 를 붙이기 때문이며 Phase 3 에서 해소된다
  - Phase 3 진입 승인 (2026-08-17, 태형님 "진행해" — public API 변경 게이트 통과)
- work Phase 3: 완료 (2026-08-17, docs/works/2026-08-17-home-dashboard-redesign-work.md)
  - 백엔드: `FavoriteDisplayMode` enum·DTO 삭제, `PUT /api/favorites/display-mode` 제거,
    `reorder` 3인자화, Entity 매핑 제거(컬럼 유지), history 무조건 첨부
  - reorder 알고리즘 단순화 — 컨테이너가 `(sourceType)` 하나가 되며 인터리브 헬퍼 6종 제거(서비스 31.5KB → 27.4KB)
  - 프론트: 그래프/지표 2분할 → 단일 카드 그리드(스파크라인·카테고리 필터·순서 편집), `favorite.js` 죽은 차트 헬퍼 84줄 제거
  - `compileJava`·`compileTestJava`·`test`(전체) PASS, `displayMode` 잔여 참조 Java·static 각 0건
  - 하네스 실측 15항목 PASS
  - **실행 중 확정**: 별표를 목업대로 "비교 선택"으로 쓰고 관심 해제는 ✕ 로 분리
  - **실행 중 발견**: 카테고리 필터로 카드가 숨은 채 순서를 저장하면 숨은 카드가 페이로드에서 빠져 순서가 망가짐
    → 편집 중 필터 UI 숨김 + `getHomeIndicatorCards` 가 편집 중 필터 무시
  - **부수 정리**: 스파크라인이 인라인 SVG 라 `renderFavoriteChart`·`destroyFavoriteChart`·`favorites._charts` 가 죽은 코드가 되어 제거
  - Phase 4 진입 승인 (2026-08-17, 태형님 "진행해" — 신규 공개 API 게이트 통과)
- work Phase 4: 완료 (2026-08-17, docs/works/2026-08-17-home-dashboard-redesign-work.md)
  - 신규 API `GET /api/news/feed?userId&size` — 활성 키워드 전체를 합친 최신 피드(`keywordCount`·`todayCount`·`items`)
  - 리포지토리 2종(`findLatestByKeywordIds`·`countByKeywordIdsSince`) + `KeywordNewsFeedResponse` 신설
  - 우측 패널 3종 구현 — 알림(배분/추세/만기) · 키워드 뉴스 · 월급 스택 바
  - `compileJava`·`compileTestJava`·`test`(전체) PASS, 하네스 실측 16항목 PASS
  - **확정한 임계값**: 배분은 편차 최대 1건만 · 추세는 4회 연속(목업 3주는 일별 지표에서 과다) · 만기는 120일(90일이면 목업 D-102 사례가 걸러짐)
  - **실행 중 발견**: 뉴스 시각 포맷이 타임스탬프 차로 계산돼 **어제 늦은 기사가 '오늘'로 표시** → 날짜 경계 비교로 수정
  - `source`(언론사)는 News 도메인에 없어 응답에서 제외 (목업 패널도 미사용)
  - Phase 5 진입 승인 (2026-08-17, 태형님 "진행해")
- work Phase 5: 완료 (2026-08-17, docs/works/2026-08-17-home-dashboard-redesign-work.md)
  - 기능 요약 3카드·요약 카드 4장 제거 → `home.html` 496 → 67줄
  - `dashboardSummary` 에서 뉴스 기록·운영자 로그 페치와 월급 도넛 차트 제거(181 → 67줄), 홈 진입 호출 2건 감소
  - 죽은 헬퍼 정리 — `hasEcos/GlobalDashboardContent`·`scrollFavorites`
  - **후속 처리**: `destroyDashboardSummaryChart` 를 부르던 `app.js` 2곳 중 navigateTo 분기를
    `destroyHomeCompareChart()` 로 교체 — 지우기만 했으면 홈 이탈 후 비교 차트가 남아 누적됨
  - `compileJava`·`compileTestJava`·`test` PASS, 하네스 실측 7항목 PASS, 죽은 심볼 7종 잔여 0건
- work 전체: Phase 1~5 완료
- review: 완료 (2026-08-17, docs/reviews/2026-08-17-home-dashboard-redesign-review.md)
  - High 1 · Medium 2 · Low 6 제시 (조치 중 L6 추가 발견)
  - H1: `GET /api/news/feed` 가 클라이언트 `userId` 를 검증 없이 신뢰 → 인증 주체 일치 검증 추가(불일치·미인증 403)
    - `NewsSecurityContext.matchesCurrentUser()` 신설. 예외를 던지지 않는 이유는 공용 핸들러에
      인증/인가 핸들러가 없어 500 으로 떨어지기 때문 — 공용 핸들러 변경은 타 도메인 영향으로 범위 밖
  - L1(미사용 import) · L2(죽은 로더) · L3(필터 복구) · L6(실패/빈 상태 문구 분리) 수정
  - M1(뷰모델 메모이제이션) · M2(`/enriched` N+1) 는 **validation 실측 후 판단**으로 보류
  - L4(`formatKrwCompact` 공용화) · L5(도달 불가 방어) 는 범위 밖
  - `compileJava`·`compileTestJava`·`test` PASS, 조치 검증 6항목 PASS
  - **후속 후보**: `GlobalExceptionHandler` 인증/인가 예외 핸들러 추가, 기존 엔드포인트 userId 검증 일괄 적용
  - validation 단계 진입 승인 (2026-08-17)
- work Phase 6 (목업 디자인 반영): 완료 (2026-08-17)
  - 태형님 지적으로 구조만 맞고 **디자인이 반영되지 않은 상태**임을 확인
  - 목업 페이지의 computed style 을 실측해 `--dc-*` 토큰·타이포·색·사이드바를 그대로 적용
  - 배치 4건 정정(브리핑 카드화 · 포트폴리오 카드 재구성 · 기간 버튼 위치 · 카드 푸터 좌우)
  - 조작 방식 정정 — 목업대로 **카드 전체 클릭**으로 비교 추가(별표는 상태 표시)
  - 폰트·배경·사이드바는 전역 적용 → 타 화면 회귀 확인 필요 항목 추가
- validation: 완료 (2026-08-17, docs/validations/2026-08-17-home-dashboard-redesign-validation.md)
  - 실서버(dev, :8080) + 카카오 로그인으로 검증
  - **H1 조치 확인**: `userId=1` 200 / `userId=9999` **403**
  - 표시 모드 폐지 확인(`displayMode` 필드 없음, `/display-mode` 엔드포인트 제거)
  - history 무조건 첨부 확인(관심 지표 전건에 시계열 첨부)
  - 순서 편집·저장 확인 — `display_mode` 컬럼이 남은 기존 스키마에서 정상(204)
  - **V1 환경 이슈**: 이전 세션의 가짜 토큰(`dev-harness`) 탓에 인증 API 가 전부 실패해 "기능이 깨진 것"처럼 보였다.
    dev 도 `JwtAuthenticationFilter` 가 붙어 유효 JWT 없이는 principal 이 없다 → 실제 로그인 필요
  - **V2 실데이터에서만 드러난 결함 2건 수정**: `cycle` 이 주기 라벨이 아닌 날짜(`20260815`)여서 raw 노출 /
    기간 라벨이 실제 구간(6주)보다 과장(`6M −13.7%`) → 실제 시작일 기준으로 표기
  - **V3 review Medium 2건 종결**: 30건 실측 결과 `/enriched` 29ms, 프론트 재계산 12.6ms(250포인트 시뮬 18.4ms)
    → M1 메모이제이션·M2 배치화 **둘 다 조치 불필요**
  - 모바일 반응형 통과, 타 화면 11종 회귀 통과(콘솔 오류 0건)
  - **운영 적용 필수 항목 없음** (마이그레이션 없음)
  - commit 단계 진입 승인 (2026-08-17)
- commit: 완료 (2026-08-17, docs/commits/2026-08-17-home-dashboard-redesign-commit.md — 단일 커밋 `db5b73d`, 38 files, +3,073/−1,148)
  - 마이그레이션 없음 (`display_mode` 는 DB DEFAULT 가 있어 매핑 제거만으로 안전, 컬럼 DROP 은 후속)
  - **정정**: 커밋 직전 요약에서 표시 모드 폐지를 "비가역"이라 설명했으나 부정확했다.
    컬럼·값이 DB 에 그대로 남아 revert 하면 기존 설정이 살아난다 → "되돌릴 수 있으나 현재는 아무도 읽지 않는 상태"가 정확
- push: 완료 (2026-08-17, docs/pushes/2026-08-17-home-dashboard-redesign-push.md)
  - 브랜치 푸시 → main `--no-ff` 병합(`4c337a8`) → `origin/main` 푸시 (`2162979..4c337a8`)
  - 분기 이후 `origin/main` 이 움직이지 않아 **충돌 없음** (#110 과 달리 해소 작업 불필요)
  - 병합 후 `main` 에서 `compileJava`·`test` 재검증 PASS

## 잔여 처리 항목
- **GitHub Issue #114 종료 여부** — 병합됐으나 이슈는 열린 상태 (#110 도 동일하게 열려 있음)
- worktree `wt-issue-114-...` 정리 여부
- 로컬 dev 서버(:8080) 기동 상태 유지 중
- 후속 후보: `GlobalExceptionHandler` 인증/인가 예외 핸들러(401/403), 기존 엔드포인트 userId 검증 일괄 적용,
  `formatKrwCompact` 공용화(review L4), `display_mode` 컬럼 DROP

## Approval Gate 항목
- **표시 모드(displayMode) 폐지** — `changeFavoriteDisplayMode` 제거 + `reorderFavorites` 시그니처에서 displayMode 제거. **public API 변경 → 태형님 승인 완료 (2026-08-17, "표시 모드 없애고")**. 세부 처리(엔드포인트 제거 vs 유지·컬럼 처리)는 plan에서 확정
- **키워드 뉴스 통합 엔드포인트 신설** — 신규 공개 API. **태형님 승인 완료 (2026-08-17, "통합 엔드포인트로 만들자")**. 응답 스키마는 plan에서 확정
- **기존 홈 섹션 제거** — 화면 구성 변경(비즈니스 로직 아님). **태형님 승인 완료 (2026-08-17, "목업대로 없애줘")**
  - 운영자 로그 카드 제거 시 접근 경로 확인 완료 — `_sidebar.html` 에 admin 전용 메뉴로 이미 존재

## 확인 사실 (대조 과정에서 검증)
- `getEnrichedFavorites()` 가 `history` · `dataValue` · `previousDataValue` · `cycle` · `className` 을 이미 반환 → 비교 차트 · 스파크라인 · 브리핑 · 추세 알림 모두 **프론트 계산으로 충족**, 신규 지표 API 불필요
- `GET /api/news` 는 `keywordId` 필수(`NewsController:29`) → 키워드 뉴스 통합 목록은 신규 엔드포인트 필요
- 월급 데이터는 `dashboardSummary.salary` 로 이미 로드 중 → 도넛 → 스택 바는 표현만 변경
- 포트폴리오 배분 상태는 `homeSummary.allocationStatus` 로 이미 로드 중

## Notes
- 목업 색 토큰: RED `#c02a22` · BLUE `#1f4f9e` · AMBER `#b5854a` — #110 포트폴리오와 동일 계열
- 목업은 인라인 SVG로 차트를 그리지만 기존 코드가 Chart.js 기반이라 구현은 Chart.js로 맞춘다(스파크라인은 경량 인라인 SVG 검토)
