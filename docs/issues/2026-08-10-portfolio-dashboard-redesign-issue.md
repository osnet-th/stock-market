# 포트폴리오 화면 대시보드형 재설계 Issue 기록

gate: docs/gates/2026-08-10-portfolio-dashboard-redesign-gates.md

## GitHub Issue
- status: created
- issue_number: 110
- issue_url: https://github.com/osnet-th/stock-market/issues/110
- title: [enhancement] 포트폴리오 화면 대시보드형 재설계 — 4탭 구조·자산 수정 버튼·연금 자산군·자산추이/배당/CAGR/환차손익 집계
- label: enhancement

## 근거
- brainstorm: docs/brainstorms/2026-08-10-portfolio-dashboard-redesign-brainstorm.md (Status: Decided)
- 태형님이 Claude 디자인 목업 v1(2026-08-09) → v3(2026-08-10)을 제시하고 기능 누락 검토 요청
- 태형님 결정: CSV 내보내기 제외 / 자산 수정 버튼 추가 + 자산군별 액션 노출 규칙 복원
- Open Questions 6건 회신(2026-08-10): 뉴스 키워드 메뉴 완전 이관 · 연금 자산군 추가 · 국내/해외 표시 레벨만 분리 · 신규 백엔드 4종 이번 범위 포함 · 매도 이력 전체 기간 · 재무상세 현재 필터 유지
- 태형님 "진행해"(2026-08-10)로 brainstorm 확정 및 issue 등록·worktree 생성 승인

## Branch
- branch: feat/issue-110-portfolio-dashboard-redesign
- base: main
- worktree: /Users/tang/Documents/workspace/wt-issue-110-feat-issue-110-portfolio-dashboard-redesign

## 작업 범위 요약
1. 프론트엔드 4탭 재구성 (보유 자산 / 매도 이력 / 목표 배분 / 분석) + 자산 수정 버튼 + 자산군별 액션 노출 규칙
2. 뉴스 → 키워드 메뉴 완전 이관 (포트폴리오에는 키워드 등록만 잔류)
3. 연금 자산군 신설 (`AssetType.PENSION`)
4. 신규 집계 4종 (자산 추이 스냅샷 / 배당·이자 / CAGR·보유일수 / 환차손익)

## 참고 자료
- 목업 v1: `~/Downloads/포트폴리오 대시보드 (단일파일).html`
- 목업 v3: `~/Downloads/포트폴리오 대시보드 v3 (단일파일).html`
- 선행 이슈: #107 (포트폴리오 그래프 가독성 개선 — 편차 중심 배분 카드가 목표 배분 탭에 계승됨)
