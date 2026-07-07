# 사용자 게이트 단계 하네스 브레인스토밍

gate: docs/gates/2026-07-07-user-gated-stage-harness-gates.md

## 배경
- 기존 documented workflow는 `brainstorm -> plan -> work -> review`를 선언하지만 검증, 커밋, 푸시 단계는 명시 산출물로 강제하지 않는다.
- 단계 전환 판단이 작업자에게 남아 있으면 사용자가 원하는 승인 중심 흐름이 약해진다.
- 각 단계마다 무엇을 할지와 다음 단계로 넘어가도 되는지에 대한 판단을 태형님에게 넘기는 계약이 필요하다.
- 승인 기록을 각 단계 md에 반복하면 산출물이 장황해지고 수정 지점이 늘어난다.

## 목표
- documented workflow의 표준 단계를 `start -> brainstorm -> issue -> plan -> work -> review -> validation -> commit -> push`로 확장한다.
- brainstorm 완료 후 plan 진입 전 대응 GitHub Issue를 확인/등록하도록 강제한다.
- 각 단계 산출물 md는 별도 게이트 로그 md를 참조한다.
- 로컬/CI 하네스가 단계별 산출물, 게이트 로그, 게이트 참조 누락을 실패로 처리한다.

## 결정
- 단계별 산출물 경로는 `docs/brainstorms`, `docs/issues`, `docs/plans`, `docs/works`, `docs/reviews`, `docs/validations`, `docs/commits`, `docs/pushes`로 둔다.
- 단계 전환 승인 기록은 `docs/gates/*.md`에 모은다.
- issue 산출물에는 GitHub Issue 번호와 URL을 기록한다.
- worktree 생성은 `scripts/create-worktree.sh --issue <number> ...`로만 허용한다.
- documented workflow는 branch diff에 각 단계별 md가 최소 1개씩 있어야 한다.
- 각 단계 md는 `gate: docs/gates/*.md`로 게이트 로그를 참조해야 한다.
- 게이트 로그는 각 단계의 `approved` 기록을 포함해야 한다.
- lightweight workflow는 같은 순서를 대화 안에서 따르되 파일 산출물 강제는 하지 않는다.
