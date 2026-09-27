# 하네스 절대경로 하드코딩 제거 검증

gate: docs/gates/2026-09-27-harness-path-hardcoding-gates.md

## 실행한 검증

| 항목 | 방법 | 결과 |
| --- | --- | --- |
| 셸 문법 | `sh -n` × 5 | 통과 |
| 파이썬 문법 | `python3 -m py_compile` | 통과 |
| 루트 산출 (메인) | 메인 저장소 `scripts/`에서 `--git-common-dir` 조회 | 메인 루트 반환 |
| 루트 산출 (worktree) | worktree `scripts/`에서 동일 조회 | 메인 루트 반환 (동일) |
| 착수 스크립트 | `start-issue-worktree.sh 999999` | `GitHub issue not found: #999999` — 저장소 검사를 통과해 이슈 조회 단계까지 진행. 수정 전에는 `Git repository not found`로 즉시 종료 |
| stage 리마인더 hook | stdin JSON 주입 후 실제 실행 | exit 0, worktree #125 / stage `implement` 정상 인식, 변경 파일 수 집계 정상 |
| `test-gate-reminder.sh` | 인자 없이 실행 | exit 0, pending draft 없어 무출력 (정상 동작) |
| `checkpoint-guard.sh` | 인자 없이 실행 | 사용법 안내 출력 (정상) |
| `validate-plan.sh` | 인자 없이 실행 | usage 출력, 예시가 상대경로로 표시됨 |
| 하드코딩 잔여 | `grep -rn "/Users/thlee" scripts/ docs/ai/` | 0건 |
| 기존 계획 문서 검증 | `validate-plan.sh` × 과거 문서 | 통과 (F1 조치 후) |
| 신규 계획 문서 검증 | `validate-plan.sh` × 본 plan | 통과 |

## 요구사항 커버리지

| ID | 판정 | 근거 |
| --- | --- | --- |
| REQ-1 | 충족 | 루트를 git에서 산출하므로 저장소 위치에 의존하지 않음 |
| REQ-2 | 충족 | 메인·worktree 양쪽 조회 결과가 동일한 메인 루트 |
| REQ-3 | 충족 | `scripts/`·`docs/ai/` 잔여 0건 |
| REQ-4 | 충족 | 착수 스크립트가 저장소 검사를 통과함을 확인 |
| REQ-5 | 충족 | 이름 규칙·검사 로직·출력 형식 diff 없음 |
| REQ-6 | 충족 | git 조회 실패 시 빈 값 → 기존 가드 동작 |

## 미실행

- 실제 이슈 번호로 worktree 생성까지의 전체 경로: 부작용(브랜치·디렉터리 생성)이 있어 미실행. 저장소 검사 통과까지로 대체 검증했다.
- 단위테스트: 대상이 셸·파이썬 스크립트이며 `test_plan_status: none`이다.
