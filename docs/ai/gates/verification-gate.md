# Verification Gate

이 문서는 구현 이해 확인 Gate, 검증, 작업 관리 상세 규칙이다. 최상위 계약은 [agent-harness.md](../agent-harness.md)이며, 충돌 시 더 보수적인 규칙을 따른다.

---

## 구현 이해 확인 Gate

리뷰와 선택 이슈 수정이 끝난 뒤, 검증 선택 전에 `explain` 단계에서 구현 이해 확인 Gate를 수행한다.

에이전트는 프롬프트상으로 아래 내용을 설명한다.

- root plan `## 요구사항 원장` 기준 요구사항 커버리지: REQ별 판정, 구현 위치, 충족 근거
- 현재 구현 흐름
- 주요 변경 파일과 컴포넌트
- 구현 중 확정된 도메인 정책과 조회 규칙
- 요청에서 실제 코드까지 이어지는 처리 흐름
- 예외, 권한, 트랜잭션, 외부 연동, 운영 영향
- 검증 전 잔여 위험과 미확인 항목

설명 후 요구사항 커버리지 집계를 제시하고 GAP 결정을 받는다. 객관식 문항은 제시하지 않는다.

| 커버리지 상태 | Gate 동작 |
|---|---|
| `미충족` 1건 이상 | 선택지를 제시하지 않고 즉시 중단한다. 태형님 판단으로 구현 복귀 또는 root plan 갱신·재승인으로 전환한다 |
| `범위 제외` 또는 `부분 충족` 1건 이상 | GAP 건별로 `수용` / `후속 이슈 생성` / `지금 구현 범위에 추가` 결정을 받는다 |
| 전부 `충족` | 커버리지 요약만 제시하고 Verification Harness로 넘어간다 |

`지금 구현 범위에 추가`를 선택하면 root plan 범위를 먼저 확인하고, 범위 밖이면 plan 갱신·재승인 절차로 전환한다.
GAP 결정 결과(REQ ID, 선택한 처리, 사유)는 리뷰 결과 산출물에 기록한다.
GAP 결정 전에는 검증 선택, PR, 병합 단계로 넘어가지 않는다.

Gate 진입 시점에 [notion-guide.md](../notion-guide.md)에 따라 Implementation 페이지를 생성하고 구현 설명을 동기화한다. Gate 진행 중 설명이 보완되면 같은 페이지를 갱신한다.

## Verification Harness

구현 이해 확인 Gate가 끝난 뒤에는 PR 여부를 묻기 전에 반드시 태형님에게 아래처럼 검증 선택을 먼저 요청한다.

```text
태형님, 검증은 어떤 걸로 실행할까요?
- 정적/문서 검증
- 빌드/테스트 검증: ./gradlew compileJava 또는 ./gradlew test
- 실제 테스트: bootRun 기동 후 curl 실검증
- 이번에는 실행하지 않고 미실행 사유 기록
```

태형님이 선택한 검증만 수행한다. 선택 없이 임의로 검증을 건너뛰거나 PR 단계로 넘어가지 않는다.
선택지를 물을 때 변경 규모 기준 권장안을 함께 제시한다. 단위테스트가 있는 변경은 `./gradlew test`, API 계약 변경은 `bootRun` + curl을 권장 기본으로 한다.
선택한 검증 결과, 실행 명령, 핵심 출력 요약, 미실행 사유는 검증 증거 산출물에 기록한다.

## 실제 테스트 Gate

태형님이 실제 테스트를 선택하면 아래 순서를 따른다.

1. worktree 기준으로 `./gradlew compileJava`가 통과하는지 먼저 확인한다.
2. `bootRun` 기동에 필요한 `.env` 설정이 worktree에 있는지 확인한다. 없으면 메인 저장소 기준 설정을 확인하고 태형님에게 보고한다.
3. `bootRun` 기동 실패 시 curl 검증, PR 단계로 넘어가지 않고 실패 원인을 보고한다.
4. 기동이 완료되면 변경 엔드포인트 기준 curl 시나리오를 제시하고 실행한다. 인증이 필요한 API는 JWT 발급 흐름을 포함한다.
5. 비밀번호, accessToken, refreshToken, 로그인 응답 전문은 검증 증거 산출물, plan, 최종 응답에 기록하지 않는다.

## 완료 전 체크리스트

- plan 체크리스트가 실제 작업 상태와 일치하는가
- plan 범위를 벗어난 파일 수정이 없는가
- `/Users/thlee/Documents/personal/stock-market/scripts/validate-plan.sh {plan-file}`이 통과했는가
- `/Users/thlee/Documents/personal/stock-market/scripts/check-plan-conflicts.sh {이슈번호}`가 통과했는가
- [ARCHITECTURE.md](../../../ARCHITECTURE.md)의 계층 규칙, DTO/Entity/Domain 경계를 지켰는가
- `docs/ai/agent-harness.md`, `AGENTS.md`, `CLAUDE.md`를 수정했다면 공통 하네스 단일 원본 구조를 유지했는가
- Implementation/Review 작업으로 만든 새 파일이 있으면 `git add -N`을 적용해 `git diff`에 보이도록 했는가
- issue 기반 Implementation 작업에서 완료 의사를 보였다면 커밋, push, PR 생성 또는 기존 PR 확인을 수행했는가
- 이슈 코멘트, PR 본문에 내부 작업용 md 파일 경로를 포함하지 않았는가
- 병합했다면 Final Cleanup Gate로 issue worktree 제거와 메인 저장소 로컬 `main` 최신화를 수행했는가
- 관련 Gradle 빌드 또는 테스트를 실행했는가
- 실행하지 못한 검증이 있으면 이유를 최종 응답에 적었는가

## Task Harness

- 한 번에 하나의 작업만 진행한다.
- 작업 리스트는 순서대로 한 단계씩만 수행한다.
- 현재 작업이 완료되면 다음 작업으로 넘어가기 전에 태형님에게 확인한다.
- 작업 완료 시 `docs/plans/` plan 문서의 `- [ ]` 항목을 `- [x]`로 갱신한다.
- TaskCreate 사용 시 subject는 `[도메인명] 구체적인 작업 내용` 형식으로 작성한다.
- TaskCreate activeForm은 현재 진행형으로 명확하게 작성한다.

## Root Path Harness

- 메인 저장소 문서와 스크립트 기준 경로는 `/Users/thlee/Documents/personal/stock-market`다.
- issue worktree는 `/Users/thlee/Documents/personal/stock-market-issue-{이슈번호}`에 생성한다.
- issue worktree 안에는 메인 저장소의 최신 `docs/`, `scripts/`가 없을 수 있으므로 하네스 스크립트는 절대 경로로 실행한다.
- plan 경로는 `/Users/thlee/Documents/personal/stock-market/docs/plans/`를 기준으로 사용한다.
- root script 예:
  - `/Users/thlee/Documents/personal/stock-market/scripts/start-issue-worktree.sh`
  - `/Users/thlee/Documents/personal/stock-market/scripts/validate-plan.sh`
  - `/Users/thlee/Documents/personal/stock-market/scripts/check-plan-conflicts.sh`
  - `/Users/thlee/Documents/personal/stock-market/scripts/checkpoint-guard.sh`
