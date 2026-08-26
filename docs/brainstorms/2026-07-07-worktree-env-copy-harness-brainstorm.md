# worktree .env 복사 하네스 브레인스토밍

## 배경
- `.env`는 `.gitignore` 대상이라 `git worktree add`로 새 작업공간을 만들 때 자동으로 포함되지 않는다.
- 이 프로젝트는 외부 API 키와 로컬 실행 설정을 `.env`에 둔다.
- 새 worktree에 `.env`가 없으면 구현 검증이나 로컬 실행이 뒤늦게 실패한다.

## 목표
- `scripts/create-worktree.sh`로 worktree를 만들 때 primary worktree의 `.env`를 새 worktree로 복사한다.
- `.env` 원본이 없거나 읽을 수 없으면 worktree 생성 절차를 실패시켜 누락을 조기에 드러낸다.
- `.env` 내용은 출력하지 않는다.
- 작업 중 업데이트와 최종 응답에서 사용자 호칭을 `태형님`으로 고정한다.

## 결정
- `.env` 원본은 현재 실행 디렉토리가 아니라 `git worktree list --porcelain`의 첫 번째 worktree(primary worktree)에서 찾는다.
- 대상 worktree 생성 후 `${target_path}/.env`로 `cp -p` 복사한다.
- 대상에 이미 `.env`가 있으면 덮어쓰지 않고 실패한다.
- 정책 문서에 worktree 생성 시 `.env` 복사 계약을 명시한다.
- `AGENTS.md`, `CLAUDE.md`, `compound-engineering.local.md`에 호칭 규칙을 명시한다.
