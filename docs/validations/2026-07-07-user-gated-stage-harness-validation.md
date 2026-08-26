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
- local documented harness: expected fail until commit document is updated for the latest scope

## 미검증 / 리스크
- issue gate 추가 후 validation 문서가 변경에 포함되지 않으면 `--through validation`에서 실패한다.
- 확인된 실패 메시지: `Documented workflow requires at least one docs/validations/*.md file through validation against main`
- validation 문서 갱신 후 `scripts/check-documented-workflow.sh --base main --through validation`은 통과했다.
- commit 단계에서는 최신 scope를 반영한 `docs/commits/*.md` 갱신 후 local documented harness를 다시 실행해야 한다.
