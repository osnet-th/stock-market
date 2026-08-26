# worktree 자동화 계획

## 작업 리스트
- [x] `scripts/create-worktree.sh` 작성
- [x] `scripts/check-worktree.sh` 작성
- [x] documented workflow용 `scripts/check-documented-workflow.sh` 작성
- [x] `.githooks/pre-push` 추가
- [x] 로컬 git hook 경로를 `.githooks`로 연결
- [x] 셸 문법 검증 및 실행 권한 적용

## 구현 범위
- 신규 스크립트: `scripts/create-worktree.sh`, `scripts/check-worktree.sh`, `scripts/check-documented-workflow.sh`
- 신규 hook: `.githooks/pre-push`
- 로컬 설정: `git config core.hooksPath .githooks`

## 주의사항
- linked worktree는 documented workflow로 취급한다.
- primary workspace는 lightweight workflow로 취급하고 documented workflow 문서 검사는 생략한다.
