# GitHub Issue Gate

이 문서는 GitHub 이슈 기반 작업의 착수, 조회, 완료 규칙이다. 최상위 계약은 [agent-harness.md](../agent-harness.md)이며, 충돌 시 더 보수적인 규칙을 따른다.

---

## GitHub Harness

- 이슈/PR 조회와 조작은 `gh` CLI를 사용한다. 별도 인증 파일 없이 로컬 `gh auth` 세션을 사용한다.
- 대상 저장소는 `osnet-th/stock-market`이다.
- 토큰 값은 터미널 출력, 로그, 이슈 코멘트, PR 본문, plan 문서에 기록하지 않는다.
- issue 기반 작업은 현재 브랜치가 `issue/{이슈번호}-{slug}` 형식이고 이슈 번호가 확인된 경우로 본다.

## 이슈 착수 Gate

issue 기반 작업 공간 준비는 `/Users/thlee/Documents/personal/stock-market/scripts/start-issue-worktree.sh {이슈번호}`를 사용한다. 스크립트는 아래를 수행한다.

1. `gh issue view {이슈번호}`로 이슈 존재와 open 상태를 확인하고, 제목/본문을 `{worktree}/.claude/issues/{이슈번호}/{이슈번호}.md`에 저장한다.
2. 메인 저장소에서 `main` 전환 후 `git pull`로 최신화한다.
3. 최신 `main` 기준으로 `../stock-market-issue-{이슈번호}` worktree를 `issue/{이슈번호}-{slug}` 브랜치로 생성한다.
4. `gh issue edit {이슈번호} --add-assignee @me`로 착수를 표시한다.
5. stage 마커 `{worktree}/.claude/issues/{이슈번호}/stage`를 `brainstorm`으로 생성한다.

- 이슈 확인, worktree 생성, stage 마커 생성 중 하나라도 실패하면 작업 시작을 중단하고 보고한다.
- 착수 시점에는 이슈 코멘트를 작성하지 않는다.
- 이 절차는 plan을 생성하지 않는다. 이슈 상세 저장과 worktree 생성까지만 수행한다.
- 이슈 상세 저장 이후 issue 기반 Plan 또는 Implementation은 Harness Brainstorm 문서를 먼저 작성한다.
- issue 기반 Implementation은 Harness Brainstorm 문서를 입력으로 어떤 경우에도 `/ce:plan`을 수행한다.
- `/ce:plan` 결과는 root `docs/plans/` plan으로 사용하고, 검증과 태형님 승인 후 `active`로 전환한다.
- active root plan과 충돌 검사를 통과한 뒤 구현은 반드시 `/ce:work`로 시작한다.

## PR 단계 이슈 처리

- issue 기반 Implementation 작업에서 "작업 완료" 의사를 보이면 [git-pr-gate.md](git-pr-gate.md)의 Implementation PR Gate를 수행해 PR 생성까지만 처리한다.
- PR 본문에 `Closes #{이슈번호}`를 포함해 병합 시 이슈가 자동 close되게 한다.
- PR 단계와 완료 단계 모두 이슈 코멘트를 작성하지 않는다. 변경 요약은 PR 본문이 담당한다.

## 이슈 완료 처리

- 이슈 close는 PR 병합 시 `Closes #{이슈번호}`로 자동 처리되는 것을 기본으로 한다.
- PR 없이 이슈를 직접 close해야 하는 경우(중복, 취소 등)는 태형님 지시가 있을 때만 `gh issue close`를 수행한다.
- 병합 후 이슈가 close되지 않았으면 Final Cleanup Gate 전에 확인하고 보고한다.

## 이슈 문서 경로 금지

- 이슈 본문, 이슈 코멘트, PR 본문에는 plan 문서, brainstorm 문서, 분석 문서 같은 내부 작업용 md 파일 경로를 포함하지 않는다.
- `docs/plans/**`, `docs/brainstorms/**`, `.claude/designs/**`, `.claude/analyzes/**` 경로를 쓰지 않는다.

## 이슈 생성

- 신규 이슈 생성은 태형님 요청이 있을 때만 `gh issue create`로 수행한다.
- 제목은 작업 내용을 요약한 명사구로, 본문은 작업 내용과 완료 조건 중심으로 작성한다.
- 생성 전 제목과 본문 미리보기를 태형님에게 확인받는다.
