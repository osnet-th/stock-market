# 사용자 게이트 단계 하네스 구현 기록

gate: docs/gates/2026-07-07-user-gated-stage-harness-gates.md

## 작업 내용
- 실행 계약 문서가 확장 단계와 게이트 로그 정책을 참조하도록 수정한다.
- documented workflow 검사 스크립트가 단계 산출물, 게이트 로그, 게이트 참조를 검사하도록 수정한다.
- 단계별 산출물은 승인 내용을 직접 담지 않고 별도 게이트 로그를 참조한다.
- 검사 스크립트는 committed diff, staged 변경, unstaged 변경, untracked 파일을 합쳐서 검사한다.
- `--through <stage>` 옵션으로 현재 승인된 단계까지만 검사할 수 있게 한다.
- 로컬 documented pre-push 경로는 push 전에 실행되므로 `--through commit`까지만 검사한다.
- push 산출물은 push 전 의도/대상/승인을 기록하고, push 결과는 완료 후 최종 응답 또는 후속 기록으로 남긴다.
- brainstorm과 plan 사이에 issue 단계를 추가한다.
- issue 산출물은 GitHub Issue 번호/URL을 기록하고, worktree 생성은 `--issue <number>`를 필수로 받는다.
- `docs/issues/*.md`는 bootstrap exception이 아니면 숫자 `issue_number`와 GitHub `issue_url`을 반드시 포함해야 한다.
