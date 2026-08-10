# 납입 리마인더 당일 표시 + 이력 0건 판정 누락 Commit 기록

**Date:** 2026-08-10
**Issue:** #111
gate: docs/gates/2026-08-10-deposit-reminder-due-today-gates.md

## 태형님 승인

- 2026-08-10 "진행해" — commit → push(PR 생성·main 병합) 일괄 승인

## 포함 파일

- src/main/java/.../portfolio/application/PortfolioService.java
- src/main/java/.../portfolio/application/dto/PortfolioItemResponse.java
- src/main/resources/static/js/components/portfolio.js
- src/main/resources/static/partials/portfolio-deposit-financial.html
- docs/brainstorms/2026-08-10-deposit-reminder-due-today-brainstorm.md
- docs/issues/2026-08-10-deposit-reminder-due-today-issue.md
- docs/plans/2026-08-10-001-fix-deposit-reminder-due-today-plan.md
- docs/works/2026-08-10-deposit-reminder-due-today-work.md
- docs/reviews/2026-08-10-deposit-reminder-due-today-review.md
- docs/validations/2026-08-10-deposit-reminder-due-today-validation.md
- docs/gates/2026-08-10-deposit-reminder-due-today-gates.md
- docs/commits/2026-08-10-deposit-reminder-due-today-commit.md
- docs/pushes/2026-08-10-deposit-reminder-due-today-push.md

## 제외 파일

- 없음 (worktree 내 기타 변경 없음. 하네스는 scratchpad, launch.json 항목은 primary worktree 로컬 설정으로 커밋 대상 아님)

## 커밋 메시지

```
fix(portfolio): #111 납입 리마인더 — 당일 항목 표시·이력 0건 판정 누락 수정

- isDepositDueToday 신설: 오늘==납입일(말일 보정)·당월 기록 없음 → 팝업 대상 (#99 스펙 "당일+미납")
- getItems null 가드 수정: 납입 이력 0건 항목도 getOrDefault(빈 리스트)로 판정 수행
- PortfolioItemResponse.depositDueToday additive 추가, 미납 배지는 기존(다음날부터) 유지
- 팝업: 당일/미납 상태 배지 구분, 헤더·문구 조정
```
