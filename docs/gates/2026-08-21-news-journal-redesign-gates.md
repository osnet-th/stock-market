# 뉴스 기록 마스터-디테일 리디자인 게이트 로그

## 원칙
- 각 단계 시작 전 태형님에게 작업 내용을 제시한다.
- 다음 단계로 넘어갈지에 대한 판단은 태형님에게 넘긴다.
- 단계별 산출물은 이 게이트 로그를 `gate: docs/gates/2026-08-21-news-journal-redesign-gates.md`로 참조한다.

## 세션 특성 (승인 방식 주석)

본 작업은 **원격 자율 세션**(claude.ai 웹 세션, 지정 브랜치 `claude/news-record-html-update-wsz95j`)
으로 실행됐다. 태형님이 대화에 상주하지 않아 단계별 개별 승인을 받을 수 없으므로, 최초 요청문

> "현재 뉴스기록 화면을 이 html 로 변경하고 부족한 기능은 백엔드 코드 추가 또는 수정해서 맞춰줘"

을 start~push 전 단계의 포괄 승인으로 해석했다. 단계 전환마다 승인을 대기하는 대신,
각 단계 산출물과 자율 결정 사항을 본 로그와 단계 문서에 남긴다. **최종 수용 여부는 태형님이
푸시된 브랜치를 검토해 판단한다** (main 병합은 이 세션 범위 밖).

## Stage Decisions

아래 `approved` 는 개별 회신이 아니라 **요청문의 포괄 승인 해석**이다 (위 "세션 특성" 참조).
harness(`check-documented-workflow.sh`) 형식 요건에 맞춰 마커만 표준형으로 두고, 각 단계의
실제 진행 내용과 자율 결정은 Stage Log 에 기록한다.

- start: approved
- brainstorm: approved
- issue: approved
- plan: approved
- work: approved
- review: approved
- validation: approved
- commit: approved
- push: approved

## Stage Log
- 2026-08-21: 태형님이 목업 업로드. 번들 HTML(React + `x-dc` 템플릿)에서 템플릿 330KB /
  로직 28KB 추출해 현재 `news-journal.html`(302줄) · `news-journal.js`(335줄)와 대조
- brainstorm: 문서 작성 완료 (docs/brainstorms/2026-08-21-news-journal-redesign-brainstorm.md)
  - 자율 결정: '약재'→'악재' 오타 판단 · 앱 셸 유지 · 초안 localStorage · 더 불러오기 증분 로드 ·
    검색/키워드 서버사이드 · stats API 신설 · 관계도 React 미도입 · 삭제 confirm 유지 ·
    링크 제목 선택화(빈 제목 URL 대체) · 미래 일자 금지 유지 · Entity 변경 없음
- issue: GitHub MCP `create_issue` 2회 시도 모두 Bad credentials 실패 →
  docs/issues/2026-08-21-news-journal-redesign-issue.md 에 미등록 상태 기록, 후속 처리로 이관
- plan: 문서 작성 완료 (docs/plans/2026-08-21-001-feat-news-journal-redesign-plan.md)
  - Phase 1 백엔드(필터 확장+stats) → Phase 2 프론트 전면 리라이트 → Phase 3 검증
  - 확인 사실: list 응답이 WHAT/WHY/HOW 전문·링크·키워드를 이미 포함 → 디테일 pane 추가 페치 불필요
  - 확인 사실: `_buildLogQuery` 가 배열 반복 파라미터 지원 → keywords 전달에 api.js 변경 최소
  - 확인 사실: 미래 일자 금지는 도메인 규칙(`NewsEvent.create/updateBody`) → 프론트 max=today 유지
  - 확인 사실: 링크는 title/url 모두 NotBlank → 프론트에서 빈 제목을 URL 로 대체

- work Phase 1 (백엔드): 완료 (docs/works/2026-08-21-news-journal-redesign-work.md)
  - 리스트 필터 `q`/`keywords`(AND) + `GET /api/news-journal/stats` + projection record 2종
  - 빈 IN 리스트 렌더링 회피 sentinel, LIKE `!` escape, Entity/스키마 변경 없음
- work Phase 2 (프론트): 완료 — partial 605줄·컴포넌트 831줄 전면 교체, `.nj-*` CSS, api.js 확장
  - **실행 중 발견(High)**: Alpine 문자열 `:style` 바인딩이 style 속성을 통째로 대체해
    정적 style 소실 + `x-show` display:none 이 리사이즈 시 지워지는 실버그 → 35곳 객체 바인딩 전환.
    목 하네스 실측으로 발견 (work 문서 상세)
  - 좁은 화면 임계 1080→1280, 칩 TOPN 1400/1660 (사이드바 224px 보정)
- review: 완료 (docs/reviews/2026-08-21-news-journal-redesign-review.md)
  - High 1(F1 :style 클로버) · Medium 1(F2 키워드 상한 비동기) · Low 3(F3~F5) — 전부 조치
  - 미조치 수용 6건(R1~R6): 무인증 dev 500 은 기존 events 와 동일 실측 — 후속 후보 유지
- validation: 완료 (docs/validations/2026-08-21-news-journal-redesign-validation.md)
  - 컨테이너에 **로컬 PostgreSQL 16 기동** → `./gradlew test` 전체 PASS (컨텍스트 로드가 JPQL 검증 겸함)
  - 목 하네스 **61항목 PASS** + 실서버(dev :8080, 실 PG, 실 JWT) API 스모크 10시나리오 +
    프론트 통합 **11항목 PASS** (UI 생성→실 DB 반영까지)
  - LIKE escape(`q=%`·`q=100%`)·키워드 AND·상한 400·stats 응답 실측 일치
  - 미검증: 카카오 실로그인 플로우(컨테이너 OAuth 불가 — JWT 직접 서명 대체), 운영 대용량 성능
- commit: 완료 (docs/commits/2026-08-21-news-journal-redesign-commit.md — 단일 커밋, 26파일 + 문서)
- push: 완료 (docs/pushes/2026-08-21-news-journal-redesign-push.md — origin 지정 브랜치)

## Approval Gate 항목
- **신규 공개 API** `GET /api/news-journal/stats`, 기존 리스트 API `q`/`keywords` 파라미터 추가 —
  요청문 "부족한 기능은 백엔드 코드 추가 또는 수정해서 맞춰줘"를 포괄 승인으로 해석해 진행.
  파라미터는 추가 전용(기존 호출 무변경 호환), 응답 스키마 변경 없음
- **화면 구성 변경** (타임라인·모달 → 마스터-디테일·인라인) — 목업 요청 자체가 승인
- Entity 생성/수정 **없음** — 게이트 해당 없음
- 패키지 구조/레이어 책임 변경 **없음** — newsjournal 컨텍스트 내 기존 레이어에 파일 추가만

## 잔여 처리 항목
- GitHub Issue 수동 등록 (자격 증명 복구 후) — 제안 제목은 issue 문서 참조
- main 병합 여부 — 태형님 검토 후 결정 (본 세션은 지정 브랜치 푸시까지)
- 서버 실행 검증(실서버 + 로그인) — 원격 컨테이너에 DB/OAuth 미구성으로 목 하네스까지만 실측

## Notes
- 목업 색 토큰: RED `#c02a22` · BLUE `#1f4f9e` · GREEN `#2e8b62` · AMBER `#b5854a` —
  #110/#114 와 동일 계열, `custom.css` 의 `--dc-*` 토큰 재사용
- 목업 '약재' 표기는 오타로 판단해 '악재' 유지 (EventImpact.BAD)
  — **태형님 확인 완료 (2026-08-21, "오타야")**. 구현은 이미 '악재'라 코드 변경 없음
