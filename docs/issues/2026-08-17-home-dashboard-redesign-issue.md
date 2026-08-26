# 홈 대시보드 경제 대시보드형 리디자인 Issue 기록

gate: docs/gates/2026-08-17-home-dashboard-redesign-gates.md

## GitHub Issue
- status: created
- issue_number: 114
- issue_url: https://github.com/osnet-th/stock-market/issues/114
- title: [enhancement] 홈 대시보드 경제 대시보드형 리디자인 — 2단 레이아웃·지표 비교 차트·표시모드 폐지·키워드 뉴스 통합
- label: enhancement

## 근거
- brainstorm: docs/brainstorms/2026-08-17-home-dashboard-redesign-brainstorm.md (Status: Decided)
- 태형님이 목업 `~/Downloads/경제 대시보드 리디자인 (단일파일).html` 제시 — "이거대로 현재 화면 변경 해줄래"
- Open Questions 3건 회신(2026-08-17): 표시 모드 폐지 · 키워드 뉴스 통합 엔드포인트 신설 · 기존 홈 섹션 목업대로 제거

## Branch
- branch: feat/issue-114-home-dashboard-redesign
- base: main
- worktree: /Users/tang/Documents/workspace/wt-issue-114-feat-issue-114-home-dashboard-redesign

## 작업 범위 요약
1. 홈 2단 레이아웃 + `오늘의 시장` 헤더(기간·정규화 모드) + 오늘 브리핑 + 포트폴리오 요약 카드
2. 지표 비교 보기 신규 (최대 3개 오버레이 · 변화율/자체 스케일 정규화)
3. 관심 지표·글로벌 지표 카드 그리드 통합 (스파크라인 · 카테고리 필터 · 순서 편집)
4. 표시 모드(displayMode) 폐지 — API·서비스·프론트 정리 (승인 게이트)
5. 키워드 뉴스 통합 엔드포인트 신설 (승인 게이트)
6. 우측 패널 — 알림 3종 · 키워드 뉴스 · 월급 스택 바
7. 기존 홈 섹션 제거 — 기능 요약 3카드 · 요약 카드 4장 · 최근 업데이트 박스

## 참고 자료
- 목업: `~/Downloads/경제 대시보드 리디자인 (단일파일).html`
- 선행 이슈: #110 (포트폴리오 대시보드형 재설계 — 카드·색 톤 계승)
- 관련 후속: #113 (배당 집계 KSD 전환) — 이번 범위 무관
