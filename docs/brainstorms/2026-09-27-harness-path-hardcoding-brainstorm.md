# Brainstorm: 하네스 저장소 절대경로 하드코딩 제거

gate: docs/gates/2026-09-27-harness-path-hardcoding-gates.md

## 입력
- issue: #125
- worktree: stock-market-issue-125
- source: 이슈 #124 착수 실패 (`Git repository not found`)

## 문제 정의
하네스 스크립트가 `/Users/thlee/Documents/personal/stock-market`을 상수로 들고 있는데 현재 환경에 그 경로가 없다. 계정·작업 디렉터리가 바뀐 뒤 스크립트와 문서가 따라오지 않았다.

특히 `harness-stage-reminder.sh`는 매 턴 실행되는 UserPromptSubmit hook이라, 단계별 의무사항 주입이 조용히 실패하고 있었다.

## 목표
저장소를 어느 경로에 두더라도 하네스 스크립트가 동작하게 한다.

## 범위
- `start-issue-worktree.sh`, `harness-stage-reminder.sh`, `checkpoint-guard.sh`, `test-gate-reminder.sh`, `validate-plan.sh`
- `docs/ai/` 하위 실행 안내 문서

## 제외 범위
- worktree 디렉터리·브랜치 이름 규칙
- 스크립트 검사 로직·출력 형식
- 하네스 정책

## 현재 코드 확인 완료 지점
- 루트 상수는 4개 스크립트에 있고, `validate-plan.sh`는 usage 예시 문자열만 해당된다.
- `check-plan-conflicts.sh`에는 하드코딩이 없다.
- worktree 안에서 실행해도 메인 저장소를 가리켜야 한다. `git rev-parse --git-common-dir`이 worktree에서도 메인 저장소의 git 디렉터리를 반환함을 실측 확인했다.

## 후보 접근
| 후보 | 내용 | 평가 |
| --- | --- | --- |
| A. git 공용 디렉터리 기준 산출 | `--git-common-dir`의 상위를 루트로 사용 | worktree·메인 양쪽에서 메인 루트를 얻는다 |
| B. 스크립트 위치 기준 | `dirname $0`의 상위 | worktree 사본 실행 시 worktree 루트를 가리켜 의미가 달라진다 |
| C. 환경변수 주입 | 호출자가 ROOT 전달 | 호출 지점이 여러 곳이라 누락 위험 |

## 권장 접근
후보 A. 단, git 조회 실패 시 빈 값이 되어 기존 가드(저장소 없음·디렉터리 없음)가 그대로 동작하게 한다.

## 결정 사항
1. 루트는 git 공용 디렉터리 기준으로 산출한다.
2. 상위 디렉터리와 plans 경로는 산출된 루트에서 파생한다.
3. 이름 규칙과 검사 로직은 변경하지 않는다.
4. 완료 상태 계획 문서의 `worktree:` 값은 `validate-plan.sh`의 절대경로 규칙 대상이므로 수정하지 않는다.

## 태형님 확인 필요
없음. 접근과 범위는 2026-09-27 확인 완료.

## plan 입력 요약
- 확정 범위: 스크립트 5개 + `docs/ai/` 실행 안내
- 확정 제외: 이름 규칙, 검사 로직, 정책, 완료 계획 문서 메타데이터
- DB 영향: 없음
- public API 영향: 없음
