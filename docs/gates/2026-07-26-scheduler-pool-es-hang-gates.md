# ES 클라이언트 행 스케줄러 풀 고갈 게이트 로그

## 원칙
- 각 단계 시작 전 태형님에게 작업 내용을 제시한다.
- 다음 단계로 넘어갈지에 대한 판단은 태형님에게 넘긴다.
- 단계별 산출물은 이 게이트 로그를 `gate: docs/gates/2026-07-26-scheduler-pool-es-hang-gates.md`로 참조한다.

## Stage Decisions
- start: approved
- brainstorm: approved
- issue: approved
- plan: approved
- work: approved
- review: approved (2026-07-26, "권장안대로 반영해" — M1·M2·L1·L2·N2 반영 + M3 문서 명기, M4·L3 보류)
- validation: approved (2026-07-26, "진행해" — 결과 기록, 미검증 항목은 운영 배포 후 확인으로 명시)
- commit: approved (2026-07-26, "진행해")
- push: approved (2026-07-26, "진행해" — 세션 지정 브랜치. PR·main 병합은 별도 지시 대기)

## Stage Log
- start: 2026-07-26, 태형님 "글로벌 경제지표 그래프에 히스토리가 하나도 안 보여" — 원인 진단 시작. 진단 결과 시스템 전체 cron 정지(본 이슈)와 글로벌 히스토리 결함(Issue B)으로 분리, "Issue A부터 진행" 제안에 태형님 "진행해" (2026-07-26)
- brainstorm: 완료 (2026-07-26, docs/brainstorms/2026-07-26-scheduler-pool-es-hang-brainstorm.md)
  - 운영 서버 3중 교차 검증: DB(04-11 단일 시딩) + docker logs(72h 배치 로그 전무) + 스레드 덤프(scheduling-3·5가 ES bulk `BasicFuture.get()` 영구 블로킹) + WARN "I/O reactor has been shut down" + ES 서버 정상(5주 무중단) → 앱 측 reactor 사망 + 무기한 대기 + 공용 스케줄러 풀 잠식으로 확정
  - 방향: 로그 flush 전용 스레드 격리 + 버퍼 드롭 정책 + KIS/DART 타임아웃 위생 + 풀 상향. "진행해"는 A 진행 승인이며 세부 설계는 plan 게이트에서 확정
- issue: 완료 (2026-07-26, GitHub Issue #96 등록 — docs/issues/2026-07-26-scheduler-pool-es-hang-issue.md 참조. 검색 중 발견: Issue B는 기존 open #51 재사용, #25(04-11 오후 지표 10개 추가)로 미시딩 10개 원인 규명 — URL 문제 아님)
- plan: 완료 (2026-07-26, docs/plans/2026-07-26-002-fix-scheduler-pool-es-hang-plan.md — 태형님 "진행해" 승인)
- work: 완료 (2026-07-26, docs/works/2026-07-26-scheduler-pool-es-hang-work.md — 4파일 수정, compileJava 통과)
- review: 완료 (2026-07-26, 태형님 "ce:review 로 진행해" — ce:review 미설치로 #93 선례대로 compound-engineering review_agents 4종 병렬 서브에이전트 대체 수행. docs/reviews/2026-07-26-scheduler-pool-es-hang-review.md — 중간 4·낮음 4·nit 5)
- review 반영: 태형님 "권장안대로 반영해" (2026-07-26) — M1·M2·L1·L2·N2 반영 + M3 문서 명기, M4·L3 보류(후속 논의). review 문서 "반영 내역" 절 참조
- validation: 완료 (2026-07-26, docs/validations/2026-07-26-scheduler-pool-es-hang-validation.md — compileJava 2회 통과 + 리뷰 라인 추적 검증. 운영 확인 항목 5건 명시: log-flush 스레드·cron 재개(KST 16:00/16:30)·글로벌 적재 재개·ES 적재 동등성·재발 시 기대 동작)
- commit/push: 완료 (2026-07-26, docs/commits·docs/pushes 기록 — 증분 커밋 dbe06bb·aeea0c6·e8410f5 + 최종 문서 커밋. PR·main 병합은 태형님 별도 지시 대기)

## Approval Gate 항목
- 비즈니스 로직 변경: LogBatchBuffer 실행 모델 변경(공용 @Scheduled → 전용 단일 스레드 executor) — 로그 적재 의미(best-effort at-most-once)는 불변
- 설정 변경: task.scheduling.pool.size 5 → 10, KIS/DART HTTP 클라이언트 타임아웃 부여
- API·Entity·DB 변경 없음
- worktree: 본 세션은 원격 실행 환경의 지정 브랜치 `claude/global-economic-indicator-history-bug-dnjci5`를 사용 — `scripts/create-worktree.sh` 대체 (세션 하네스 제약, 태형님 인지 필요)

## Notes
- 중간 문서 커밋: 원격 세션 하네스(stop hook)가 미커밋 파일의 즉시 커밋·푸시를 요구하여, work 착수 전 워크플로 문서 4건(brainstorm/issue/plan/gate)만 지정 브랜치에 선커밋 (2026-07-26). 코드 변경 없음 — 본 커밋은 workflow의 commit 단계가 아니며, 실제 작업 커밋은 commit 게이트 승인 후 별도 수행.
- 연관: Issue B(글로벌 경제지표 히스토리 적재 결함) — 본 이슈 해결이 선행되어야 B의 배치가 실제로 돈다.
- 임시 조치: `docker restart hubth-app` 으로 풀 리셋 가능 (재발 가능성 있음), 글로벌 배치는 서버 시간 07:30 UTC(KST 16:30) 실행.
