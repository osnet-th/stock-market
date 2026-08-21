# 용어 사전 마스터-디테일 리디자인 게이트 로그

## 원칙
- 각 단계 시작 전 태형님에게 작업 내용을 제시한다.
- 다음 단계로 넘어갈지에 대한 판단은 태형님에게 넘긴다.
- 단계별 산출물은 이 게이트 로그를 `gate: docs/gates/2026-08-21-glossary-redesign-gates.md`로 참조한다.

## 세션 특성 (승인 방식 주석)

본 작업은 **원격 자율 세션**(claude.ai 웹 세션, 지정 브랜치 `claude/glossary-ui-backend-update-dnbbk2`)
으로 실행됐다. 태형님이 대화에 상주하지 않아 단계별 개별 승인을 받을 수 없으므로, 최초 요청문

> "이거 대로 용어 사전에 대한 화면 변경해주고 부족한 부분은 백엔드 코드 수정해서 보강해줘"
> (업로드 목업 `4b044daa-*.html` 첨부)

을 start~push 전 단계의 포괄 승인으로 해석했다. 단계 전환마다 승인을 대기하는 대신,
각 단계 산출물과 자율 결정 사항을 본 로그와 단계 문서에 남긴다. **최종 수용 여부는 태형님이
푸시된 브랜치를 검토해 판단한다** (main 병합은 이 세션 범위 밖).

동일 방식 선례: 2026-08-21 뉴스 기록 리디자인 (#117, `docs/gates/2026-08-21-news-journal-redesign-gates.md`).

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
- 2026-08-21: 태형님이 용어 사전 목업 업로드(번들 HTML 5.7MB). 번들에서 디자인 캔버스
  템플릿(35KB 본문 + `text/x-dc` 로직 스크립트)을 추출해 현재
  `glossary.html`(282줄) · `glossary.js`(334줄) · glossary 백엔드 패키지와 대조
- brainstorm: 문서 작성 완료 (docs/brainstorms/2026-08-21-glossary-redesign-brainstorm.md)
  - 자율 결정: 앱 셸 유지 · definition='풀이' 재해석(rename 없이 데이터 보존) ·
    구조화 필드 5종 신설 · 함께 볼 용어 @ElementCollection(ID 참조) ·
    검색/초성/그룹/건수 클라이언트 사이드(전량 로드) · 초안 localStorage(새 용어만) ·
    카테고리 관리 보존('관리' 팝오버) · 정렬 2종 축소 · 삭제 confirm 유지 ·
    좁은 화면 임계 1280px · 한 줄 정의 소프트 필수
- issue: GitHub MCP `create_issue` 시도 → Bad credentials 실패 (뉴스 기록 세션과 동일 증상).
  docs/issues/2026-08-21-glossary-redesign-issue.md 에 bootstrap-exception 상태로 기록,
  사후 등록으로 이관 (#117 선례)
- plan: 문서 작성 완료 (docs/plans/2026-08-21-001-feat-glossary-redesign-plan.md)
  - Phase 1 백엔드(구조화 필드 + 함께 볼 용어) → Phase 2 프론트 전면 리라이트 → Phase 3 검증
  - 확인 사실: list 응답이 전체 필드를 이미 포함 (GlossaryTermResponse.from) → 디테일 pane 추가 페치 불필요
  - 확인 사실: 스키마 권위는 Entity(ddl-auto: update) — 컬럼 rename 불가, 추가만 안전.
    SQL 파일은 DBA 수동 적용 백업용 컨벤션
  - 확인 사실: JPQL 벌크 DELETE 는 @ElementCollection 행을 지우지 않음 → 관계 정리 쿼리 별도 필요
- work: 완료 (docs/works/2026-08-21-glossary-redesign-work.md)
  - Phase 1 백엔드: GlossaryTermContent VO + 도메인/Entity/Mapper/Repository/Service/DTO 확장,
    related 소유권 검증·자기참조 제거·상한 20·삭제 시 양방향 정리, SQL 백업 작성
  - Phase 2 프론트: partial 407줄·컴포넌트 732줄 전면 교체, `.gl-*` CSS 추가,
    Alpine `:style` 객체 바인딩 준수(#117 실버그 회피), 전량 로드(200건/페이지 루프)
  - **실측 수정(High) 2건**: ① @ElementCollection FK 자동 생성이 용어-먼저 삭제 순서와 충돌
    → `@ForeignKey(NO_CONSTRAINT)` ② @OrderColumn position 구멍이 `[null,…]` 로 로드돼 매퍼
    NPE → null 필터. 임시 스모크로 red→green 재현 검증 (로컬 PG16 기동 실측)
- review: 완료 (docs/reviews/2026-08-21-glossary-redesign-review.md)
  - High 2 (F1 FK 충돌 · F2 position 구멍 NPE — 조치) · Medium 1 (F3 인라인 background 가
    :hover 차단 — .on 클래스 전환) · Low 3 (F4 하네스 타이밍 / F5 초안 키 사용자 미구분 /
    F6 전량 로드 상한 — 기록). Open Questions 4건은 태형님 확인 대기
- validation: 완료 (docs/validations/2026-08-21-glossary-redesign-validation.md)
  - gradle 전체 117 테스트 PASS(로컬 PG16 기동) · 관계 수명주기 스모크(임시, red→green 후 삭제) ·
    Hibernate 생성 스키마/백업 SQL psql 대조 · node --check PASS ·
    Playwright 목 하네스 57항목 3회 연속 PASS · check-documented-workflow.sh --through push PASS
- commit: 완료 (docs/commits/2026-08-21-glossary-redesign-commit.md) — feat 1건 + docs 1건
- push: 완료 (docs/pushes/2026-08-21-glossary-redesign-push.md) —
  origin/claude/glossary-ui-backend-update-dnbbk2
