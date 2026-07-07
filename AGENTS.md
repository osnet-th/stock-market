# CLAUDE.md

이 문서는 이 저장소에서 코딩 에이전트가 따라야 하는 실행 계약이다.

## Purpose

- 모든 작업은 `start -> brainstorm -> plan -> work -> review -> validation -> commit -> push` 순서를 따른다.
- workflow, 사용자 게이트, 승인 게이트, 컨텍스트 우선순위, 구현/리뷰/검증/커밋/푸시 기준만 정의한다.
- 세부 코드 규칙과 worktree 규칙은 별도 policy 문서에 둔다.

## Workflow Policy

### 공통 규칙

- work는 항상 현재 plan 범위 안에서만 진행한다.
- current plan이 없으면 work를 시작하지 않는다.
- work 중 범위가 바뀌면 plan부터 갱신한다.
- 각 단계는 시작 전에 태형님에게 무엇을 할지 제시하고, 다음 단계로 넘어가도 되는지 확인받는다.
- 태형님 승인 없이 다음 단계로 넘어가지 않는다.
- documented workflow의 단계 전환 승인은 `docs/gates/*.md`에 모으고, 각 단계 산출물은 해당 게이트 로그를 참조한다.

### documented workflow

아래 경우에는 문서를 파일로 남긴다.

- 비즈니스 로직 변경
- API, Entity, 구조 변경
- 요구사항 해석이 필요한 작업
- 영향 범위가 크거나 리스크가 높은 작업

정의:

- current brainstorm: `docs/brainstorms/*.md`
- current plan: `docs/plans/*.md`
- current work: `docs/works/*.md`
- current review: `docs/reviews/*.md`
- current validation: `docs/validations/*.md`
- current commit: `docs/commits/*.md`
- current push: `docs/pushes/*.md`
- current gate: `docs/gates/*.md`

### lightweight workflow

아래 경우에는 같은 순서를 따르되 문서를 파일로 남기지 않을 수 있다.

- 오타 수정
- 명백한 컴파일 에러 수정
- 문서 수정
- 로직 의미 변경이 없는 국소적 수정

정의:

- current brainstorm: 현재 대화에서 명시된 문제 정의
- current plan: 현재 대화에서 명시된 작업 범위와 단계
- work/review/validation/commit/push/gate는 현재 대화에서 결과와 태형님 승인 여부를 명시한다.

### escalation

lightweight workflow로 시작했더라도 아래 조건이 생기면 즉시 documented workflow로 승격한다.

- 로직 의미 변경 필요
- 영향 범위 확대
- 요구사항 해석 필요
- 구조, API, Entity 변경 필요

## Approval Gates

아래 조건은 반드시 중단 후 사용자 확인을 받는다.

- 패키지 구조, 레이어 책임, 의존성 방향 변경
- public API 시그니처 변경 또는 신규 공개 API 추가
- Entity 생성 또는 수정
- 비즈니스 로직의 동작 변경
- current plan 범위를 넘어서는 추가 작업 필요
- 현재 작업 완료 후 다음 작업으로 연속 진행 필요
- brainstorm, plan, work, review, validation, commit, push 각 단계로 전환

## Context Sources

작업 전 아래 순서로 컨텍스트를 확인한다.

1. 사용자 최신 명시 지시
2. `ARCHITECTURE.md`
3. `docs/policies/code-convention.md`
4. `docs/policies/git-worktree.md`
5. `compound-engineering.local.md`
6. current gate
7. current plan
8. current brainstorm
9. current work
10. current review
11. current validation
12. current commit
13. current push
14. `docs/solutions/**`
15. 현재 코드베이스
16. 레거시 참고 자료

레거시 참고 자료:

- `.claude/analyzes/**`
- `.claude/designs/**`
- `MD_WRITE_GUIDE.md`

규칙:

- documented workflow에서 `current plan`은 `docs/plans/*.md` 파일이다.
- documented workflow에서 work/review/validation/commit/push/gate는 각각 `docs/works/*.md`, `docs/reviews/*.md`, `docs/validations/*.md`, `docs/commits/*.md`, `docs/pushes/*.md`, `docs/gates/*.md` 파일이다.
- lightweight workflow에서 `current plan`은 현재 대화 내 명시된 작업 범위다.
- policy 문서의 세부 규칙은 `CLAUDE.md`에 중복 기재하지 않는다.

## Implementation Contract

- 아키텍처 기준은 [ARCHITECTURE.md](ARCHITECTURE.md)를 따른다.
- 코드 구조와 리뷰 기준은 [docs/policies/code-convention.md](docs/policies/code-convention.md)를 따른다.
- 작업 격리와 브랜치 분리 기준은 [docs/policies/git-worktree.md](docs/policies/git-worktree.md)를 따른다.
- 요청 없는 리팩토링 또는 API 변경은 하지 않는다.
- Entity는 ID 기반 참조만 허용한다.
- Lombok 사용을 기본으로 하며 수동 getter/setter는 작성하지 않는다.
- 테스트는 명시적 요청 시에만 작성하되, 기본 구현은 테스트 가능한 구조로 유지한다.
- 한 번에 하나의 작업만 진행한다.
- documented workflow에서는 plan의 체크리스트를 완료 상태로 갱신한다.
- documented workflow에서는 단계별 산출물이 current gate 문서를 참조하도록 유지한다.

## Review Contract

리뷰 시 아래 순서를 따른다.

1. Findings를 심각도 순으로 제시
2. Open Questions / Assumptions 정리
3. Change Summary를 짧게 정리

Findings에는 아래를 포함한다.

- 버그, 회귀 위험, 누락 테스트, 설계 위반
- `docs/policies/code-convention.md` 위반 여부
- 파일/라인 근거

문제 없음이면 `명시적 findings 없음`을 먼저 적고 남은 리스크를 덧붙인다.

## Validation Contract

- validation 단계에서는 실행한 명령, 결과, 미검증 항목을 `docs/validations/*.md`에 기록한다.
- 검증 실패 또는 미검증 항목이 있으면 태형님에게 다음 진행 여부를 확인한다.

## Commit / Push Contract

- commit 단계에서는 포함 파일, 제외 파일, 커밋 메시지, 태형님 승인 여부를 `docs/commits/*.md`에 기록한다.
- push 단계에서는 대상 remote/branch, push 의도, 태형님 승인 여부를 `docs/pushes/*.md`에 기록한다.
- push 결과는 push 완료 후 최종 응답 또는 후속 기록으로 남긴다.

## Response Contract

- 작업 중 업데이트와 최종 응답에서 사용자를 `태형님`으로 호칭한다.

최종 응답은 아래 형식을 기본으로 한다.

1. 요약
2. 변경 파일
3. 검증 결과
4. 리스크/다음 단계

단순 질의는 짧게 답하되, 규칙 위반 가능성이 있으면 즉시 게이트를 안내한다.
