# 글로벌 경제지표 히스토리 적재 보강 Issue 기록

gate: docs/gates/2026-08-02-global-indicator-history-hardening-gates.md

## GitHub Issue
- status: 기존 open 이슈 재사용 (신규 등록 없음)
- issue_number: 51
- issue_url: https://github.com/osnet-th/stock-market/issues/51
- title: global 경제 지표 히스토리 처리 불가 (2026-05-10 태형님 등록)

## 근거
- brainstorm: docs/brainstorms/2026-08-02-global-indicator-history-hardening-brainstorm.md (Status: Decided)
- 태형님 지시(2026-08-02): "한번에 처리하지 왜 이걸 나눠서 처리하는거야" — #51 전체 범위 일괄 진행
- 선행: #96(스케줄러 전체 정지, PR #98 병합)이 #51 증상의 직접 원인이었고, 본 작업은 글로벌 히스토리 파이프라인 자체의 잔여 결함 4건(트랜잭션·정렬·catch-up·1포인트 UI) 보강

## Branch
- branch: claude/global-economic-indicator-history-bug-dnjci5 (원격 세션 지정 브랜치 — PR #101 병합 후 main 9579dea에서 재시작)
- base: main (9579dea)
