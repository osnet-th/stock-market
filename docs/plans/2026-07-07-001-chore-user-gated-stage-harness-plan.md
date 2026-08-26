# 사용자 게이트 단계 하네스 계획

gate: docs/gates/2026-07-07-user-gated-stage-harness-gates.md

## Origin
- `docs/brainstorms/2026-07-07-user-gated-stage-harness-brainstorm.md`

## 작업 리스트
- [x] `AGENTS.md` / `CLAUDE.md`에 단계 순서와 게이트 로그 참조 규칙 추가
- [x] `compound-engineering.local.md`에 확장 단계와 산출물 경로 반영
- [x] `scripts/check-documented-workflow.sh`가 단계별 md, 게이트 로그, 게이트 참조를 검사하도록 변경
- [x] brainstorm과 plan 사이의 GitHub Issue Gate 추가
- [x] `scripts/create-worktree.sh`에 `--issue <number>` 필수화
- [x] 이번 변경의 push 산출물 작성
- [x] 셸 문법 및 documented 하네스 검증

## 구현 범위
- 수정: `AGENTS.md`
- 수정: `CLAUDE.md`
- 수정: `compound-engineering.local.md`
- 수정: `scripts/check-documented-workflow.sh`
- 신규 문서: `docs/brainstorms/2026-07-07-user-gated-stage-harness-brainstorm.md`
- 신규 문서: `docs/issues/2026-07-07-user-gated-stage-harness-issue.md`
- 신규 문서: `docs/plans/2026-07-07-001-chore-user-gated-stage-harness-plan.md`
- 신규 문서: `docs/gates/2026-07-07-user-gated-stage-harness-gates.md`
- 신규 문서: `docs/works/2026-07-07-user-gated-stage-harness-work.md`
- 신규 문서: `docs/reviews/2026-07-07-user-gated-stage-harness-review.md`
- 신규 문서: `docs/validations/2026-07-07-user-gated-stage-harness-validation.md`
- 신규 문서: `docs/commits/2026-07-07-user-gated-stage-harness-commit.md`
- 신규 문서: `docs/pushes/2026-07-07-user-gated-stage-harness-push.md`

## 주의사항
- 하네스는 실제 대화 승인 여부를 기계적으로 증명할 수 없으므로 별도 게이트 로그 md의 승인 기록을 검사한다.
- 기존 branch 중 단계 산출물이 없는 branch는 이 변경 이후 documented 하네스에서 실패할 수 있다.
