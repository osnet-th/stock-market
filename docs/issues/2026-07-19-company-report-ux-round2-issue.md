# 기업 리포트 UX 개선 2차 Issue 기록

gate: docs/gates/2026-07-19-company-report-ux-round2-gates.md

## GitHub Issue
- status: created
- issue_number: 88
- issue_url: https://github.com/osnet-th/stock-market/issues/88
- title: [enhancement] 기업분석리포트 UX 개선 — 주가지표 자동/내 계산 통합 · DART 10년 정기보고서 바로가기 · 경쟁사 비교 여러 줄
- label: enhancement

## 근거
- brainstorm: docs/brainstorms/2026-07-19-company-report-ux-round2-brainstorm.md (Status: Decided)
- 태형님 확정(2026-07-19, AskUserQuestion 2건): "1개 이슈 3작업" + "재료를 셀에서 인라인 편집".
- 3작업: ① 주가지표 자동/내 계산 통합(재료 인라인 편집), ② DART 10년 정기보고서 바로가기(프론트 전용), ③ 경쟁사 비교 여러 줄(마크업 전용).
- 백엔드·Entity·API 변경 없음 — 기존 `getDisclosures` 재사용, `metricInputs` v2 스키마 유지.
- 선행 #81(기업분석리포트), #84(입력 개선)와 연속. 별도 worktree.

## Worktree
- branch: feat/issue-88-company-report-ux-round2
- base: main (79a43fe)
- 생성 명령: `scripts/create-worktree.sh --issue 88 feat/issue-88-company-report-ux-round2`
