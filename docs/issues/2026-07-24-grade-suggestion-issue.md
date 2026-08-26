# 투자판단 등급 자동 제안 Issue 기록

gate: docs/gates/2026-07-24-grade-suggestion-gates.md

## GitHub Issue
- status: created
- issue_number: 91
- issue_url: https://github.com/osnet-th/stock-market/issues/91
- title: [enhancement] 기업분석리포트 투자판단 정량 5항목 등급 자동 제안 + 수동 오버라이드
- label: enhancement

## 근거
- brainstorm: docs/brainstorms/2026-07-24-grade-suggestion-brainstorm.md (Status: Decided)
- 태형님 지시(2026-07-24): 등급 판정 기준표 안내 후 "이제 추가해줘" — 자동 제안 + 수동 오버라이드 구현 승인.
- #81에서 "전부 수동 입력"으로 확정했던 투자판단에, 당시 보류한 "자동 제안 + 수동 오버라이드 (권장)" 설계를 보완 적용.
- Entity·DB·스냅샷 스키마 변경 없음 — 조회 시 파생 계산, 응답 필드만 additive 확장.

## Branch
- branch: claude/operating-profit-growth-calculation-x839b1 (원격 세션 하네스 지정 브랜치)
- base: main (55fa737)
- worktree 미사용: 원격 세션 격리 환경에서 지정 브랜치 직접 개발 (게이트 로그 특이사항 참조)
