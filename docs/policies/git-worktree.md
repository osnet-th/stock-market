# Git Worktree Policy

## 목적

이 문서는 작업 격리, 병렬 작업 안정성, 메인 작업공간 보호를 위한 git worktree 사용 규칙을 정의한다.

## 기본 원칙

- 한 worktree는 하나의 작업만 담당한다.
- 한 worktree는 하나의 브랜치에만 연결한다.
- 서로 다른 기능, 버그 수정, 문서 작업을 같은 worktree에 섞지 않는다.
- 사용자 변경사항이 있는 다른 worktree를 임의로 정리하지 않는다.

## 사용 기준

### documented workflow

아래 경우에는 worktree 사용을 기본값으로 한다.

- 기능 개발
- 버그 수정
- 구조 변경
- 병렬 작업
- 현재 작업공간과 분리된 검증이 필요한 경우

### lightweight workflow

아래 경우에는 현재 작업공간에서 바로 처리할 수 있다.

- 문서 수정
- 오타 수정
- 명백한 컴파일 에러 수정
- 로직 의미 변경이 없는 국소적 수정

단, 작업 중 범위가 커지거나 해석이 필요해지면 worktree 사용으로 승격한다.

## 생성 규칙

- 기준 브랜치는 `main`을 기본값으로 한다.
- 브랜치명은 작업 목적이 드러나게 작성한다.
- worktree 경로는 작업 브랜치와 쉽게 대응되도록 작성한다.
- documented workflow에서는 brainstorm 완료 후 GitHub Issue 번호를 확보한 뒤 `scripts/create-worktree.sh --issue <number> ...`로 worktree를 생성한다.
- `scripts/create-worktree.sh`로 생성한 worktree에는 primary worktree의 `.env`를 반드시 복사한다.
- primary worktree에 `.env`가 없거나 읽을 수 없으면 worktree 생성은 실패해야 한다.

예시:

- 브랜치: `feat/dashboard-summary`
- 브랜치: `fix/chat-context-reset`
- 브랜치: `docs/code-convention-policy`
- 경로: `../wt-feat-dashboard-summary`
- 경로: `../wt-fix-chat-context-reset`

## 작업 규칙

- worktree 생성 후 해당 디렉토리에서만 작업한다.
- 다른 worktree의 변경사항을 임의로 수정하거나 정리하지 않는다.
- 현재 작업과 무관한 브랜치 변경을 섞지 않는다.
- 같은 브랜치를 여러 worktree에서 동시에 작업하지 않는다.

## 종료 규칙

- 커밋, 푸시, 머지 완료 후 worktree 제거를 검토한다.
- 브랜치가 미머지 상태면 worktree를 삭제하지 않는다.
- dirty worktree는 정리 방향을 확인한 뒤 처리한다.
- 제거 전에 현재 worktree가 작업 목적을 이미 마쳤는지 확인한다.

## 금지 사항

- 같은 브랜치를 여러 worktree에서 동시에 사용
- 사용자 변경사항이 있는 worktree를 임의 삭제
- 검증되지 않은 상태에서 main 작업공간에 여러 작업을 혼합
- 병렬 작업인데도 기존 worktree를 재사용해 작업 범위를 섞는 행위

## 위반 시 행동

- lightweight workflow에서는 현재 작업공간 유지가 맞는지 먼저 재검토한다.
- documented workflow에서는 plan에 worktree 분리 또는 전환 작업을 반영한다.
- 기존 worktree와 충돌이 의심되면 사용자 확인 없이 정리하지 않는다.

## 자동화

- `scripts/create-worktree.sh`: Issue 번호, 브랜치, worktree를 정책에 맞게 생성한다.
- `scripts/check-worktree.sh`: 현재 worktree가 documented 또는 lightweight 조건에 맞는지 점검한다.
- `scripts/check-documented-workflow.sh`: documented workflow용 brainstorm/plan 문서가 브랜치 diff에 포함됐는지 점검한다.
- `scripts/run-harness-checks.sh`: 로컬 hook과 CI가 공통으로 호출하는 검사 엔트리포인트다.
- `.githooks/pre-push`: linked worktree에서는 documented workflow 문서 검사를 자동 실행한다.
