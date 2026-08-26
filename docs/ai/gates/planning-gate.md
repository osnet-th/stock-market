# Planning Gate

이 문서는 plan 작성, plan lifecycle, DB Schema Review Gate의 상세 규칙이다. 최상위 계약은 [agent-harness.md](../agent-harness.md)이며, 충돌 시 더 보수적인 규칙을 따른다.

---

## Planning Harness

- 구현 승인 기준 문서는 항상 `docs/plans/` 하위 plan 문서다.
- `/ce:plan` 결과는 공식 plan 문서로 간주한다.
- `/ce:plan`, `/ce:compound`는 compound-engineering 기본 경로를 그대로 사용한다.
- issue 기반 Plan 또는 Implementation에서 `/ce:plan` 전 [brainstorm-harness.md](../brainstorm-harness.md)의 Harness Brainstorm 문서를 먼저 작성한다.
- `/ce:plan`은 Harness Brainstorm 문서를 입력으로 수행한다.
- issue 기반 Implementation에서 `/ce:plan`은 필수 단계다. GitHub 이슈 상세 설명이 충분해도 `/ce:plan`은 생략할 수 없다.
- issue 기반 Implementation에서 root plan이 `active`가 되고 충돌 검사를 통과한 뒤에는 직접 구현하지 않고 반드시 `/ce:work`로 구현을 시작한다.
- `/ce:work`의 task 기준은 승인된 root plan의 작업 리스트와 `allowed_paths`/`blocked_paths`다.
- `/ce:work`가 root plan 작업 리스트에 없는 구현을 요구하면 구현하지 않고 root plan 갱신, 검증, 재승인 단계로 복귀한다.
- plan 문서는 메인 저장소 `/Users/thlee/Documents/personal/stock-market/docs/plans/` 하위에만 생성한다.
- Harness Brainstorm 문서는 메인 저장소 `/Users/thlee/Documents/personal/stock-market/docs/brainstorms/` 하위에만 생성한다.
- 현재 CWD가 issue worktree 하위여도 plan/Harness Brainstorm 문서는 worktree 내부 `docs/`가 아니라 메인 저장소 공용 위치에 생성한다.
- 착오로 issue worktree 내부에 plan/Harness Brainstorm 문서를 만든 경우 작업 종료 보고 또는 worktree 삭제 전에 메인 저장소 공용 위치로 이동한 뒤 worktree 내부 원본 문서를 삭제하고, 상대 링크와 `origin` 참조가 깨지지 않는지 확인한다.
- 신규 문서는 `.claude/designs/`, `.claude/analyzes/`에 작성하지 않는다.
- 기존 `.claude/designs/`, `.claude/analyzes/` 문서는 legacy reference로만 읽는다.
- 분석 내용은 별도 분석 문서로 분리하지 않고 plan 문서의 "배경 / 현재 상태 / 문제점" 섹션에 통합한다.
- 하나의 기능/문제에는 `docs/plans/` plan 문서 1개만 유지하고, 필요 시 기존 plan을 업데이트한다.
- plan 문서에는 issue, status, branch, worktree, allowed_paths, blocked_paths, 수정 가능 범위, 수정 금지 범위를 명시한다.
- issue 기반 plan 문서에는 `## 요구사항 원장` 섹션을 필수로 포함한다. 상세 규칙은 [요구사항 원장](#요구사항-원장) 절을 따른다.
- plan 문서에는 `test_plan_status`와 `schema_plan_status`를 명시한다.
- 테스트 작성 여부가 미확정이거나 `test_plan_status: pending`이면 plan에는 테스트 시나리오 미승인 상태와 다음 확인 절차만 남기고 상세 테스트 케이스를 쓰지 않는다.
- 기능 계획이 끝난 뒤 단위 테스트를 작성하기로 결정되면, 먼저 대화로 테스트 시나리오를 설명하고 사용자 승인을 받은 뒤 승인된 내용만 plan에 반영한다.
- 단위 테스트를 작성하기로 한 plan은 Given/When/Then, 정상/예외 케이스, Mock 대상, 제외 범위, 검증 명령을 사용자 승인받은 뒤에만 `test_plan_status: approved`로 둘 수 있다. 승인 전에는 `/ce:work`로 구현을 시작하지 않는다.
- `/ce:plan` 이후 구현 승인 전 `/Users/thlee/Documents/personal/stock-market/scripts/validate-plan.sh {plan-file}`을 실행한다.
- `validate-plan.sh` 통과 후 승인 요청 시점에 [notion-guide.md](../notion-guide.md)에 따라 해당 작업의 Plan 페이지를 동기화하고, `active` 전환 시 같은 페이지를 최종본으로 업데이트한다.
- issue 기반 Implementation에서 "진행해", "구현해", "계속해" 요청을 받아도 `/ce:plan` 결과로 확정한 root plan이 `active`가 아니면 구현하지 않고 plan 작성/승인 단계로 복귀한다. root plan이 `active`이면 `/ce:work`를 먼저 실행해 root plan 작업 리스트 기준 task를 선택한다.
- plan status lifecycle은 `draft -> active -> blocked -> done` 기준으로 관리한다.

## Status 기준

- `draft`: plan 작성 중이며 구현 불가.
- `active`: 사용자 승인 후 구현 가능.
- `blocked`: 충돌, 외부 의존성, 재승인 필요 상태로 구현 중단.
- `done`: 구현, 검증, PR 병합, 체크리스트 갱신 완료.

## 요구사항 원장

issue 기반 plan은 `## 요구사항 원장` 섹션을 필수로 포함한다. 이 섹션의 `REQ-n` ID는 구현 이해 확인 Gate의 요구사항 커버리지 대조 기준이다.

- GitHub 이슈 본문의 작업 내용과 완료 조건의 모든 항목을 누락 없이 먼저 나열한 뒤 각 항목에 `REQ-n` ID를 부여한다.
- 각 REQ에 `이번 범위`(포함 또는 제외)를 적고, 제외인 경우 근거를 함께 적는다.
- 이슈에 없지만 Harness Brainstorm 또는 태형님 확인에서 추가 확정된 요구사항도 REQ로 편입하고 출처를 적는다.
- 구현 중 REQ 행을 삭제하지 않는다. 범위가 바뀌면 `이번 범위`와 근거만 갱신한다.
- `## 제외 범위` 산문은 원장의 제외 REQ를 대체하지 않는다. 제외 항목은 원장에 REQ 단위로 남는다.

```markdown
## 요구사항 원장

| ID | 요구사항 | 출처 | 이번 범위 | 근거 |
|---|---|---|---|---|
| REQ-1 | 매도 이력 저장 시 수익률 함께 계산 | 이슈 본문 작업내용 1 | 포함 | — |
| REQ-4 | 월별 타임라인 집계 API | 이슈 본문 작업내용 3 | 제외 | 화면 요구 미확정. 후속 이슈로 분리 |
```

원장 없이 작성된 plan으로 구현 이해 확인 Gate를 수행하는 경우, Gate에서 이슈 본문의 작업 내용과 완료 조건으로 REQ를 즉석 도출하고 도출본임을 커버리지 표에 명시한다.

## DB Schema Review Gate

Entity 신규 작성, 기존 Entity 필드 변경, DB table/column/index/constraint 변경, migration, backfill, rollback, 기존 데이터 영향이 있는 작업은 구현 승인 전에 DB Schema Review Gate를 통과해야 한다.

plan frontmatter의 `schema_plan_status`는 아래 값을 사용한다.

- `none`: Entity/DB 스키마 변경이 없는 작업.
- `pending`: Entity/DB 스키마 변경 가능성이 있고 테이블 설계 승인 대기 중인 작업.
- `approved`: 태형님이 테이블 설계를 명시 승인한 작업.

`schema_plan_status: pending` 상태에서는 plan을 `active`로 전환하거나 구현을 시작하지 않는다.

Entity/DB 스키마 변경 가능성이 있는 plan을 `active`로 전환하려면 plan 본문에 아래 설계 항목을 포함하고 태형님 승인 후 `schema_plan_status: approved`로 변경한다.

- table명과 목적
- column명, DB type(PostgreSQL 기준), nullable, default
- PK, UK, FK, index, constraint
- Entity와 table 매핑 방식(JPA Entity는 ID 기반 참조만 허용, 연관관계 금지)
- migration/backfill 필요 여부
- rollback 방식
- 기존 데이터 영향 범위

## 문제 발견 흐름

```text
문제 발견 -> docs/brainstorms/ Harness Brainstorm -> docs/plans/ plan 작성/업데이트 -> 승인 대기 -> 구현 -> 체크리스트 갱신
```

## 문서 경로

- Harness Brainstorm: `docs/brainstorms/{YYYY-MM-DD}-{topic}-brainstorm.md`
- plan: `docs/plans/{YYYY-MM-DD}-{NNN}-{type}-{descriptive-name}-plan.md`
- solution: `docs/solutions/`
- plan 예시 코드: `docs/plans/examples/{component}-example.md`

세부 작성 규칙은 [MD_WRITE_GUIDE.md](../../../MD_WRITE_GUIDE.md)를 따른다.
