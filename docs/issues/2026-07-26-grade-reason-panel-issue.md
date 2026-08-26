# 투자판단 제안 등급 사유 패널 Issue 기록

gate: docs/gates/2026-07-26-grade-reason-panel-gates.md

## GitHub Issue
- status: created
- issue_number: 93
- issue_url: https://github.com/osnet-th/stock-market/issues/93
- title: [enhancement] 기업분석리포트 투자판단 제안 등급 사유 패널 — 화살표 클릭 시 사유·기준·계산식·원천 데이터 표시
- label: enhancement

## 근거
- brainstorm: docs/brainstorms/2026-07-26-grade-reason-panel-brainstorm.md (Status: Decided)
- 태형님 지시(2026-07-26): "이런식으로 5개 자동으로 해주는거 다 저렇게 표현해줘 진행해" — 화살표 → 우측 슬라이드 패널(사유/등급 기준/계산식/원천 데이터) 방향을 정량 5항목 전부에 적용.
- #91에서 추가한 제안 등급의 근거 표시가 truncate 한 줄 + 툴팁뿐이라 한눈에 파악 불가 — 패널로 보완.
- 백엔드·API·Entity·DB 변경 없음 — 기존 Detail/Preview 응답 데이터만으로 프런트 구성.

## Branch
- branch: feat/issue-93-grade-reason-panel
- base: main (be097aa)
- worktree: /Users/tang/Documents/workspace/wt-issue-93-feat-issue-93-grade-reason-panel (scripts/create-worktree.sh --issue 93)
