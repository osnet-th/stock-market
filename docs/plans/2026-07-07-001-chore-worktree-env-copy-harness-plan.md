# worktree .env 복사 하네스 계획

## Origin
- `docs/brainstorms/2026-07-07-worktree-env-copy-harness-brainstorm.md`

## 작업 리스트
- [x] `scripts/create-worktree.sh`에서 primary worktree `.env` 원본 탐지
- [x] 새 worktree 생성 후 `.env` 복사 강제
- [x] `docs/policies/git-worktree.md`에 `.env` 복사 계약 추가
- [x] 작업 중/최종 응답 사용자 호칭을 `태형님`으로 고정
- [x] 셸 문법 및 하네스 검증

## 구현 범위
- 수정: `scripts/create-worktree.sh`
- 수정: `docs/policies/git-worktree.md`
- 수정: `AGENTS.md`
- 수정: `CLAUDE.md`
- 수정: `compound-engineering.local.md`
- 신규 문서: `docs/brainstorms/2026-07-07-worktree-env-copy-harness-brainstorm.md`
- 신규 문서: `docs/plans/2026-07-07-001-chore-worktree-env-copy-harness-plan.md`

## 주의사항
- `.env` 내용은 로그에 출력하지 않는다.
- `.env`가 없거나 읽을 수 없으면 새 worktree 생성 명령은 실패해야 한다.
- 대상 `.env`가 이미 있으면 덮어쓰지 않는다.
