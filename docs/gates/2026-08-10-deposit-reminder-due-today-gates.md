# 납입 리마인더 당일 표시 + 이력 0건 판정 누락 게이트 로그

## 원칙

- 각 단계 시작 전 태형님에게 작업 내용을 제시한다.
- 다음 단계로 넘어갈지에 대한 판단은 태형님에게 넘긴다.
- 단계별 산출물은 이 게이트 로그를 `gate: docs/gates/2026-08-10-deposit-reminder-due-today-gates.md`로 참조한다.

## Stage Decisions

- start: approved
- brainstorm: approved (2026-08-10, 태형님 — 당일 처리 방식 "팝업만 당일 포함" 선택)
- issue: approved (2026-08-10, 태형님 "이슈 등록 + 수정 진행" — GitHub #111 등록)
- plan: approved (2026-08-10, "이슈 등록 + 수정 진행" 승인에 수정 작업 포함)
- work: done (2026-08-10)
- review: done (2026-08-10, 셀프 리뷰 — 명시적 findings 없음)
- validation: done (2026-08-10 — gradlew test 117/117, 브라우저 하네스 전 시나리오 PASS)
- commit: approved (2026-08-10, 태형님 "진행해" — docs/commits 기록)
- push: approved (2026-08-10, 태형님 "진행해" — PR 생성·main 병합 포함, docs/pushes 기록)

## Stage Log

- start: 2026-08-10, 태형님 "자동 납입 기능이랑 납입 안되어있으면 리마인더 하는 기능... 실제로 동작을 안하네??" — 원인 조사 착수
- brainstorm: 완료 (2026-08-10, docs/brainstorms/2026-08-10-deposit-reminder-due-today-brainstorm.md)
  - 원인 2건: 당일 미판정(#100이 depositOverdue 재사용, #99 스펙은 당일+미납) + 이력 0건 null 가드
  - 선택지 A(팝업만 당일 포함) vs B(미납 판정 당일부터) → 태형님 A 확정
- issue: 완료 (2026-08-10, GitHub #111 — docs/issues/2026-08-10-deposit-reminder-due-today-issue.md)
- plan: 완료 (2026-08-10, docs/plans/2026-08-10-001-fix-deposit-reminder-due-today-plan.md)
- work: 진행 중

## Approval Gate 항목

- public API 변경: `PortfolioItemResponse`에 `depositDueToday` 필드 additive 추가 — 태형님 선택지 답변으로 승인됨
- Entity·DB 변경 없음
- 미납 배지 의미(납입일 다음날부터) 불변 — 확정안에 명시
- worktree: fix/issue-111-deposit-reminder-due-today (scripts/create-worktree.sh --issue 111)
