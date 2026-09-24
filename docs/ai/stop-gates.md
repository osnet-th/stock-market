# Stop Gates

이 문서는 에이전트가 stock-market 작업 중 즉시 중단하고 태형님 확인을 받아야 하는 조건을 정의한다. 아래 조건 중 하나라도 해당하면 진행하지 않고, 관련 gate 문서의 상세 판단을 함께 적용한다.

---

## Plan / Worktree

- 승인된 `docs/plans/` plan 문서 없이 코드 구현/수정이 필요함.
- issue 기반 Plan 또는 Implementation에서 Harness Brainstorm 문서 없이 `/ce:plan`, plan 승인, 구현/수정이 필요함.
- issue 기반 Implementation에서 `/ce:plan`을 거치지 않은 상태로 구현/수정이 필요함.
- issue 기반 Implementation에서 active root plan 이후 `/ce:work` 없이 직접 구현/수정이 필요함.
- `/ce:work`가 선택한 task가 root plan 작업 리스트, `allowed_paths`, `blocked_paths`와 매핑되지 않음.
- `main` 또는 `master` 브랜치에서 코드 구현/수정/커밋이 필요함.
- issue worktree 기반 작업에서 현재 브랜치가 허용된 `issue/{이슈번호}-{slug}` 형식이 아니거나 plan의 branch/worktree와 다름.
- `/Users/thlee/Documents/personal/stock-market/scripts/validate-plan.sh {plan-file}`이 실패함.
- `/Users/thlee/Documents/personal/stock-market/scripts/check-plan-conflicts.sh {이슈번호}`가 활성 plan 충돌을 보고함.
- plan frontmatter에 `test_plan_status` 또는 `schema_plan_status`가 누락됨.
- test/schema 승인 상태가 필요한 구현 범위와 맞지 않음.

---

## Briefing

- issue 기반 Plan 또는 Implementation에서 필요한 [briefing-harness.md](briefing-harness.md) 실행 브리핑 또는 plan-to-code 매핑 브리핑 없이 plan 승인, `/ce:work`, 구현 단계로 넘어가려 함.
- 구현 중 합의된 체크포인트 브리핑과 태형님 확인 없이 다음 큰 작업 단위로 넘어가려 함.

---

## Test / Schema

- 테스트 작성 여부가 확정되지 않았거나 테스트 시나리오 사용자 승인 전인데 plan에 상세 단위 테스트 케이스, Given/When/Then 표, 구체 예외/클래스 중심 시나리오를 기입해야 함.
- 단위 테스트 작성을 선택했지만 Given/When/Then, 정상/예외 케이스, Mock 대상, 제외 범위, 검증 명령이 포함된 테스트 시나리오 사용자 승인 없이 `/ce:work` 진입 또는 테스트/운영 코드 구현이 필요함.
- Entity 신규 작성 또는 Entity 필드/제약 변경이 필요함(JPA Entity 연관관계는 금지 대상).
- 외부 시스템, DB 스키마, 보안 정책에 영향을 줄 수 있음.
- 테스트나 빌드 실패의 원인이 현재 plan 범위를 벗어남.

---

## Harness Policy

- `docs/ai/agent-harness.md`, `AGENTS.md`, `CLAUDE.md` 하네스 정책 변경 시 공통 문서와 래퍼 문서의 참조 구조 반영 또는 plan에 명시된 예외 승인이 누락됨.

---

## Issue / PR / Merge / Cleanup

- issue 착수 과정에서 이슈 확인, worktree 생성, stage 마커 생성 중 하나라도 실패함.
- issue 기반 Implementation 완료 의사 후 검증, plan 체크리스트 갱신, 커밋, push, PR 생성/확인 중 하나라도 실패함.
- 태형님 승인 없이 `gh pr merge` 등 PR 병합이 필요함.
- Final Cleanup Gate 수행 시 대상 worktree가 plan과 다르거나, main/master worktree이거나, dirty/untracked/mis-push 상태임.

---

## Review

- 리뷰 요청을 받았는데 `/ce:review`를 수행하지 않고 explain, 검증, PR 단계로 넘어가려 함.
- `/ce:review` finding을 표 형태로 제시하지 않거나, actionable issue에 관련 요구사항/기준과 파일 경로/line/메서드명/호출 부위 중 하나 이상의 코드 위치 근거가 없음.
- 기능 요구사항을 특정해야 판단 가능한 finding인데 요구사항 출처가 불명확한 항목을 `요구사항 확인 필요`로 분리하지 않고 actionable issue처럼 보고하려 함.
- 코드 위치를 특정할 수 없는 finding을 `위치 확인 필요`로 분리하지 않고 actionable issue처럼 보고하려 함.
- `/ce:review` actionable issue 목록에 대해 태형님 선택을 받지 않고 수정하거나 explain, 검증, PR 단계로 넘어가려 함.
- `/ce:review`에서 태형님이 선택한 actionable issue가 해소되지 않았는데 explain, 검증, PR 단계로 넘어가려 함.
- 태형님이 선택하지 않은 `/ce:review` issue를 임의 수정하려 함.

---

## Implementation Understanding / Verification

- 리뷰와 선택 이슈 수정 후 구현 이해 확인 Gate 없이, 또는 `check-implementation-explainer` 스킬의 설명 구조를 적용하지 않고 검증 선택, PR, 병합 단계로 넘어가려 함.
- 구현 이해 확인 Gate에서 root plan `## 요구사항 원장` 기준 요구사항 커버리지를 제시하지 않았는데 검증 선택, PR, 병합 단계로 넘어가려 함.
- 요구사항 커버리지에 `미충족`이 1건 이상인데 중단·보고 없이 검증 선택, PR, 병합 단계로 넘어가려 함.
- `범위 제외` 또는 `부분 충족` REQ에 대한 태형님 GAP 결정 없이 검증 선택, PR, 병합 단계로 넘어가려 함.
- 구현 이해 확인 Gate 후 "검증은 어떤 걸로 실행할까요?" 선택 gate 없이 PR 여부를 묻거나 PR 단계로 넘어가려 함.
- 태형님이 선택한 검증을 수행하지 않았거나 검증 증거 산출물에 결과/미실행 사유를 기록하지 않았는데 PR 단계로 넘어가려 함.
- 실제 테스트를 선택했는데 선택된 검증(`./gradlew test` 또는 `bootRun` + curl)을 실행하지 않았거나 실패했는데 PR, 병합 단계로 넘어가려 함.

---

## Scope / Architecture

- plan에 없는 파일, API, 흐름, 구조 변경이 필요함.
- public 클래스/메서드 시그니처 변경이 필요함.
- 패키지 구조 또는 레이어 의존성 변경이 필요함.
- 로직 변경이 필요한 버그를 작업 중 새로 발견함.

---

## Exceptions

- 린터/포맷터 자동 수정처럼 로직 변경이 없는 경우.
- 명백한 컴파일 오류 수정(오타, import 누락, 변수명 오류)처럼 plan 범위를 벗어나지 않는 경우.
- [agent-harness.md](agent-harness.md) Workflow Weight의 lightweight 대상(오타 수정, 명백한 컴파일 에러 수정, 문서 수정, 로직 의미 변경이 없는 국소적 수정)으로서 대화로 범위를 합의하고 태형님 승인을 받은 경우. escalation 조건이 생기면 즉시 documented workflow로 승격한다.
