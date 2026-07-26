# 기업분석리포트 월봉 주가 차트 Issue 기록

gate: docs/gates/2026-07-16-company-report-monthly-price-chart-gates.md

## GitHub Issue
- status: created
- issue_number: 95
- issue_url: https://github.com/osnet-th/stock-market/issues/95
- title: [feat] 기업분석리포트 — 상장일부터 현재까지 월봉 주가 차트 추가
- label: enhancement

## 요약
기업 분석 리포트(위저드 5단계 + 상세 6번 섹션)에 상장일~현재 전구간 월봉 종가 라인 차트 추가.
KIS 기간별시세(FHKST03010100) 주기 파라미터화 + 신규 price-history 엔드포인트 + Chart.js 렌더.

## Worktree
- `scripts/create-worktree.sh --issue 95 feat/issue-95-monthly-price-chart`
- path: ../wt-issue-95-feat-issue-95-monthly-price-chart (base main 96528f2, .env 복사됨)
