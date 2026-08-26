# Brainstorm Harness

이 문서는 issue 기반 Plan/Implementation에서 `/ce:plan` 전에 수행하는 하네스 브레인스토밍 단계의 규칙이다. 최상위 계약은 [agent-harness.md](agent-harness.md)이며, 충돌 시 더 보수적인 규칙을 따른다.

---

## 목적

- GitHub 이슈 상세와 태형님 요청을 구현 계획으로 바로 넘기기 전에 문제, 범위, 접근 후보, 확인 필요 사항을 정리한다.
- compound-engineering의 별도 브레인스토밍 명령은 사용하지 않는다.
- 결과물은 `/ce:plan`의 입력 문서로 사용한다.
- 구현 승인 기준은 여전히 root `docs/plans/` plan이며, 브레인스토밍 문서는 구현 승인 기준이 아니다.

## 적용 시점

- issue 기반 Plan 또는 Implementation에서 이슈 상세 저장과 worktree 준비가 끝난 뒤 수행한다.
- `/ce:plan` 실행 전에 완료되어야 한다.
- 단순 질문, 코드 읽기, 일반 조사에는 필요할 때만 부분 적용한다.

## 입력

- 태형님 요청
- GitHub 이슈 상세 설명 원문 또는 저장 파일(`{worktree}/.claude/issues/{이슈번호}/{이슈번호}.md`)
- 대상 worktree
- 필요한 경우 관련 코드, [ARCHITECTURE.md](../../ARCHITECTURE.md), 기존 solution(`docs/solutions/`) 또는 기존 brainstorm 문서

## 출력 경로

브레인스토밍 문서는 메인 저장소 기준 아래 경로에만 작성한다.

```text
docs/brainstorms/{YYYY-MM-DD}-{issue-or-topic}-brainstorm.md
```

현재 CWD가 issue worktree 하위여도 메인 저장소 `/Users/thlee/Documents/personal/stock-market/docs/brainstorms/` 하위에 작성한다.

## 문서 형식

```markdown
# Brainstorm: {작업명}

## 입력
- issue:
- worktree:
- source:

## 이슈 상세 기반 이해

## 문제 정의

## 목표

## 범위

## 제외 범위

## 현재 코드 확인 필요 지점

## 후보 접근

## 권장 접근

## 결정 사항

## 태형님 확인 필요

## /ce:plan 입력 요약
```

## 작성 기준

- 이슈 상세를 요약하되 요구사항, 수용 기준, 제외 범위가 변형되지 않게 한다.
- 구현 방향을 단정하기 전에 후보 접근과 trade-off를 적는다.
- 구현 전 확인이 필요한 질문은 `태형님 확인 필요`에 남긴다.
- 확인되지 않은 항목이 구현 범위, public API, Entity/DB, 권한/보안, 테스트 계획에 영향을 주면 `/ce:plan`으로 넘어가지 않는다.
- `/ce:plan 입력 요약`에는 plan으로 넘길 결정 사항, 범위, 제외 범위, 확인 완료 항목만 적는다.

## 금지

- 브레인스토밍 문서를 구현 승인 기준으로 삼는 것.
- 브레인스토밍 문서만 근거로 코드 구현을 시작하는 것.
- 미해결 질문이 구현 범위에 영향을 주는데 `/ce:plan`으로 넘어가는 것.
- 메인 저장소가 아닌 worktree 내부 `docs/brainstorms/`에 신규 문서를 작성하는 것.

## 다음 단계

1. Harness Brainstorm 문서를 작성한다.
2. 문서 작성 완료 후 태형님 확인 요청 시점에 [notion-guide.md](notion-guide.md)에 따라 작업 페이지(개요 포함)와 Brainstorm 페이지를 동기화한다.
3. 태형님 확인이 필요한 항목이 있으면 확인받고 문서를 갱신한다. 확인 반영으로 내용이 바뀌면 `/ce:plan` 진입 전에 같은 Notion 페이지를 업데이트한다.
4. Harness Brainstorm 문서를 입력으로 `/ce:plan`을 수행한다.
5. `/ce:plan` 결과를 root `docs/plans/` plan으로 사용하고, 검증과 태형님 승인 후 `active`로 전환한다.
