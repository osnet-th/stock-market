# 사용자 게이트 단계 하네스 리뷰

gate: docs/gates/2026-07-07-user-gated-stage-harness-gates.md

## Findings
명시적 findings 없음.

## Open Questions / Assumptions
- 하네스는 실제 대화 승인 여부를 자동 증명하지 않고, `docs/gates/*.md`의 단계별 승인 기록을 검사한다.
- 로컬 pre-push는 push 전 실행되므로 `commit` 단계까지만 검사한다.
- CI의 `ci-documented`는 기본값 `--through push`를 사용한다. GitHub Actions가 비활성화된 동안에는 로컬 하네스가 주된 강제 지점이다.
- 이번 하네스 변경 자체는 Issue Gate 도입 전 시작된 bootstrap 작업이므로 `docs/issues/...`에 bootstrap exception을 기록했다.
- `docs/issues/*.md`는 bootstrap exception이 아니면 숫자 `issue_number`와 GitHub `issue_url` 형식을 검사한다.

## Change Summary
- 실행 흐름을 `start -> brainstorm -> plan -> work -> review -> validation -> commit -> push`로 확장했다.
- 단계별 승인 기록은 각 단계 문서에 반복하지 않고 `docs/gates/*.md`에 모으도록 했다.
- documented workflow 검사는 단계별 산출물과 `gate: docs/gates/*.md` 참조를 확인하도록 확장했다.
- 검사 대상은 committed diff, staged 변경, unstaged 변경, untracked 파일을 합산하도록 보강했다.
- `--through <stage>` 옵션으로 현재 승인된 단계까지만 검사할 수 있도록 했다.
- local documented 하네스는 `--through commit`까지만 검사하도록 조정했다.
- push 계약은 push 전 대상/의도/승인 기록으로 조정하고, push 결과는 완료 후 최종 응답 또는 후속 기록에 남기도록 했다.
- brainstorm과 plan 사이에 issue 단계를 추가하고, `docs/issues/*.md` 산출물과 `scripts/create-worktree.sh --issue <number>` 필수 규칙을 추가했다.
- bootstrap exception이 아닌 issue 산출물의 `issue_number` / `issue_url` 형식 검사를 추가했다.
