# ES 클라이언트 행 스케줄러 풀 고갈 수정 Commit 기록

**Date:** 2026-07-26
gate: docs/gates/2026-07-26-scheduler-pool-es-hang-gates.md

## 커밋 구성

원격 세션 특성(턴 종료마다 push 요구하는 stop hook)상 단계별 증분 커밋으로 진행:

| 커밋 | 내용 |
|------|------|
| `dbe06bb` | fix(logging): ES 행 시 스케줄러 풀 고갈 방지 — flush 전용 스레드 격리 (#96) — 코드 4파일 + work 문서 |
| `aeea0c6` | docs(logging): #96 리뷰 기록 — 4-agent 병렬 리뷰 결과 |
| `e8410f5` | fix(logging): #96 리뷰 반영 — M1·M2·L1·L2·N2 |
| (본 커밋) | docs(logging): #96 validation·commit·push 게이트 기록 |

## 포함 파일

- 코드 4파일
  - `src/main/java/.../logging/infrastructure/elasticsearch/LogBatchBuffer.java`
  - `src/main/java/.../stock/infrastructure/stock/kis/KisMasterFileClient.java`
  - `src/main/java/.../stock/infrastructure/stock/dart/config/DartRestClientConfig.java`
  - `src/main/resources/application.yml`
- workflow 문서 (brainstorm/issue/plan/gates/work/review/validation/commit/push)

## 제외 파일

- 없음 (작업 외 변경 없음 — git status 확인)

## 승인

- work: 태형님 "진행해" (plan 승인, 2026-07-26)
- review 반영: 태형님 "권장안대로 반영해" (2026-07-26)
- validation→commit/push: 태형님 "진행해" (2026-07-26)
