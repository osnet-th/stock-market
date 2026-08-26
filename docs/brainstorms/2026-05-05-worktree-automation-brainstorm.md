# worktree 자동화 브레인스토밍

## 배경
- worktree 정책을 문서로만 두면 실제 작업자가 매번 수동으로 판단해야 한다.
- documented workflow는 worktree 사용과 plan/brainstorm 존재 검사를 자동으로 묶는 편이 하네스 강도가 높다.

## 목표
- worktree 생성 스크립트를 제공한다.
- 현재 worktree 상태를 점검하는 스크립트를 제공한다.
- linked worktree에서 documented workflow 문서 존재를 push 전에 자동 검사한다.

## 결정
- `scripts/create-worktree.sh`로 브랜치와 worktree를 정책에 맞게 생성한다.
- `scripts/check-worktree.sh`로 primary/linked 상태와 documented workflow 적합성을 점검한다.
- `scripts/check-documented-workflow.sh`로 브랜치 diff에 brainstorm/plan 문서가 있는지 검사한다.
- `.githooks/pre-push`를 사용해 linked worktree에서 documented workflow 검사를 자동 실행한다.
