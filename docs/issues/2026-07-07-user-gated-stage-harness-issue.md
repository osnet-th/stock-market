# 사용자 게이트 단계 하네스 Issue 기록

gate: docs/gates/2026-07-07-user-gated-stage-harness-gates.md

## GitHub Issue
- status: bootstrap-exception
- issue_number: not-created-before-issue-gate
- issue_url: not-created-before-issue-gate

## 판단
- 이 하네스 변경은 Issue Gate 규칙을 도입하는 bootstrap 작업으로, 작업 시작 시점에는 Issue Gate가 존재하지 않았다.
- 이 변경 이후 documented workflow는 brainstorm 완료 후 plan 진입 전 GitHub Issue를 확인한다.
- 대응 Issue가 없으면 brainstorm 내용을 기반으로 GitHub Issue를 생성하고, 해당 Issue 번호로 worktree를 생성한다.

## Worktree Rule
- future command shape: `scripts/create-worktree.sh --issue <number> <branch>`
