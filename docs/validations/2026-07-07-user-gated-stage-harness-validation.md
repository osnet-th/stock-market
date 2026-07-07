# 사용자 게이트 단계 하네스 검증

gate: docs/gates/2026-07-07-user-gated-stage-harness-gates.md

## 실행 명령
- `bash -n scripts/check-documented-workflow.sh scripts/run-harness-checks.sh scripts/check-worktree.sh scripts/create-worktree.sh .githooks/pre-push`
- `diff -u AGENTS.md CLAUDE.md`
- `scripts/check-documented-workflow.sh --base main --through validation`
- `scripts/run-harness-checks.sh local-documented`

## 결과
- shell syntax: pass
- `AGENTS.md` / `CLAUDE.md` 동기화: pass
- documented workflow check through validation: pass
- local documented harness: expected fail

## 미검증 / 리스크
- `scripts/run-harness-checks.sh local-documented`는 `--through commit`까지 검사하므로 commit 산출물이 아직 없는 validation 단계에서는 실패한다.
- 실패 메시지: `Documented workflow requires at least one docs/commits/*.md file through commit against main`
- commit 단계에서 `docs/commits/*.md`와 commit gate를 추가한 뒤 local documented harness를 다시 실행해야 한다.
