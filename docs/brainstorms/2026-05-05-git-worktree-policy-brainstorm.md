# git worktree 정책 브레인스토밍

## 배경
- 작업 격리, 병렬 작업 안정성, 메인 작업공간 오염 방지를 위해 git worktree 사용 기준이 필요하다.
- worktree 사용 여부가 작업자마다 달라지면 브랜치 혼합, dirty workspace 충돌, 병렬 작업 간섭이 발생할 수 있다.

## 목표
- 언제 worktree를 기본값으로 사용하는지 정의한다.
- lightweight workflow에서 현재 작업공간을 바로 써도 되는 조건을 정의한다.
- 생성, 작업, 종료, 금지 사항을 판단 가능한 규칙으로 정리한다.

## 고려 사항
- documented workflow는 범위와 리스크가 크므로 worktree 기본 사용이 자연스럽다.
- lightweight workflow는 문서 수정, 오타 수정처럼 현재 작업공간에서 바로 끝나는 경우가 있다.
- 같은 브랜치를 여러 worktree에서 동시에 다루는 상황은 강하게 금지해야 한다.

## 결정
- 정책 문서는 `docs/policies/git-worktree.md`에 둔다.
- documented workflow는 worktree 사용을 기본값으로 둔다.
- lightweight workflow는 현재 작업공간 허용하되, 범위 확대 시 worktree로 승격한다.
- `CLAUDE.md`, `AGENTS.md`, `compound-engineering.local.md`가 이 정책 문서를 공식 참조한다.
