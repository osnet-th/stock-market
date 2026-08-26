# CI 하네스 재사용 브레인스토밍

## 배경
- 현재 로컬 `pre-push` hook은 직접 분기 로직을 들고 있고, CI에서는 같은 규칙을 재사용하지 않는다.
- 검사 로직이 hook과 CI에 중복되면 한쪽만 수정되어 규칙이 쉽게 분기된다.

## 목표
- 로컬 hook과 CI가 같은 엔트리포인트 스크립트를 호출하도록 통일한다.
- worktree 성격상 로컬에서만 의미 있는 검사와 CI에서 재사용 가능한 검사를 분리한다.

## 결정
- `scripts/run-harness-checks.sh`를 공용 엔트리포인트로 추가한다.
- `local-documented`, `local-lightweight`, `ci-documented` 모드를 지원한다.
- `.githooks/pre-push`는 직접 분기하지 않고 공용 엔트리포인트를 호출한다.
- GitHub Actions workflow에서 `ci-documented` 모드를 실행한다.
