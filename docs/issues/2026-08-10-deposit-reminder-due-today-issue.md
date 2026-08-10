# 납입 리마인더 당일 미표시 + 이력 0건 판정 누락 Issue 기록

gate: docs/gates/2026-08-10-deposit-reminder-due-today-gates.md

## GitHub Issue

- status: created
- issue_number: 111
- issue_url: https://github.com/osnet-th/stock-market/issues/111
- title: [bug] 납입 리마인더 팝업 — 납입일 당일 미표시 + 납입 이력 없는 항목 판정 누락
- label: bug

## 근거

- brainstorm: docs/brainstorms/2026-08-10-deposit-reminder-due-today-brainstorm.md (Status: Decided)
- 태형님 보고(2026-08-10): 납입일 10일 적금이 당일 리마인더에 안 뜸 — "당일날부터 떠야하는거아냐??"
- 방식 확정: 팝업만 당일 포함(depositDueToday 추가), 미납 배지는 다음날부터 유지, null 가드 수정 포함

## Branch

- branch: fix/issue-111-deposit-reminder-due-today
- base: main
- worktree: /Users/tang/Documents/workspace/wt-issue-111-fix-issue-111-deposit-reminder-due-today (scripts/create-worktree.sh --issue 111)
