# ES 클라이언트 행 스케줄러 풀 고갈 Issue 기록

gate: docs/gates/2026-07-26-scheduler-pool-es-hang-gates.md

## GitHub Issue
- status: created
- issue_number: 96
- issue_url: https://github.com/osnet-th/stock-market/issues/96
- title: [bug] ES 클라이언트 I/O reactor 사망 시 무한 대기로 스케줄러 풀 고갈 — 전체 cron 배치 정지
- label: bug

## 근거
- brainstorm: docs/brainstorms/2026-07-26-scheduler-pool-es-hang-brainstorm.md (Status: Decided)
- 태형님 승인(2026-07-26): "진행해" — Issue A(본 건) 우선 진행
- 운영 서버 3중 교차 검증: DB(글로벌 히스토리 04-11 단일 시딩) + docker logs(72h 배치 로그 전무) + 스레드 덤프(scheduling-3·5 ES bulk 영구 블로킹) + "I/O reactor has been shut down" WARN + ES 서버 정상

## 연관 이슈
- #51 (open): global 경제 지표 히스토리 처리 불가 — 본 이슈가 직접 원인. 본 이슈 해결 후 #51 별도 진행
- #25 (closed, 2026-04-11): 글로벌 지표 10개 추가 — 04-11 07:30 시딩 이후 추가되어, 배치 정지로 인해 한 번도 시딩되지 못함 (배치 재개 시 자동 시딩 예상)

## Branch
- branch: claude/global-economic-indicator-history-bug-dnjci5 (원격 세션 지정 브랜치 — scripts/create-worktree.sh 대체, 게이트 로그 Approval Gate 항목 참조)
- base: main (96528f2)
