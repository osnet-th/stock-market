# 재무 타임라인 계정 taxonomy 불일치 Issue 기록

gate: docs/gates/2026-07-19-timeline-account-taxonomy-gates.md

## GitHub Issue
- status: created
- issue_number: 85
- issue_url: https://github.com/osnet-th/stock-market/issues/85
- title: [fix] 재무 타임라인 — 연도별 DART 계정 taxonomy 불일치로 값 누락 (영업CF 2017·2018, 당기순이익 2018~2022)
- label: bug

## 근거
- brainstorm: docs/brainstorms/2026-07-19-timeline-account-taxonomy-brainstorm.md (Status: Decided)
- 원인: 원천 DART 실측 — 영업CF는 id 접두사(ifrs_ vs ifrs-full_) 세부행 분리, 당기순이익은 주요계정 API 결측(값은 전체재무제표에 존재).
- 수정 범위: 태형님 "근본 수정" 선택(2026-07-19 AskUserQuestion) — 조립기(FinancialTimelineAssembler) 수정, 종목 평가·재무 타임라인 공유.
- #84(기업 리포트 입력 개선)과 독립. 별도 worktree.

## Worktree
- branch: fix/issue-85-timeline-account-taxonomy
- base: main (d03e4a1)
- 생성 명령: `scripts/create-worktree.sh --issue 85 fix/issue-85-timeline-account-taxonomy`
