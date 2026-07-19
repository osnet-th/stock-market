# 기업 리포트 입력 개선 Issue 기록

gate: docs/gates/2026-07-19-company-report-input-improvements-gates.md

## GitHub Issue

- status: created
- issue_number: 84
- issue_url: https://github.com/osnet-th/stock-market/issues/84
- title: [feat] 기업 리포트 입력 개선 — 연혁 여러줄·년월, 매입처 품목, 주가지표 공식형 재계산
- label: enhancement

## 근거

- brainstorm: docs/brainstorms/2026-07-19-company-report-input-improvements-brainstorm.md (Status: Decided)
- 태형님 확정 (2026-07-19): 연혁 여러줄(1)·년월(2)·매입처 품목 칸(3)·주가지표 파생지표 전체 공식화(B). 원래 4번 항목은 무시.
- 이슈 구성: **1개 통합** (2026-07-19 AskUserQuestion "1개 통합" 선택 = 등록 승인).

## 범위 요약

1. 연혁 내용 여러 줄 입력 (`<textarea>`).
2. 연혁 날짜 년 또는 년-월 허용 (연혁 전용, financialChanges·revenueForecasts는 4자리 유지).
3. 매입처 품목(원자재) 칸 추가 (PartnerItem 공유 여부는 plan에서 A/B 확정 — Approval Gate).
4. 주가지표 계산기 파생지표 전체 공식형 재계산 (MetricInputs 스키마 변경 + 하위호환 — Approval Gate).

## Worktree

- branch: feat/issue-84-company-report-input-improvements
- base: main (d03e4a1)
- 생성 명령: `scripts/create-worktree.sh --issue 84 feat/issue-84-company-report-input-improvements`
