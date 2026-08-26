# 포트폴리오 목표 자산 배분 비율 Issue 기록

gate: docs/gates/2026-07-30-asset-allocation-target-gates.md

## GitHub Issue
- status: created
- issue_number: 102
- issue_url: https://github.com/osnet-th/stock-market/issues/102
- title: [enhancement] 포트폴리오 목표 자산 배분 비율 — 안전/투자 편차 표시·금 시세 평가·대시보드 요약
- label: enhancement

## 근거
- brainstorm: docs/brainstorms/2026-07-30-asset-allocation-target-brainstorm.md (Status: Decided)
- 태형님 요청(2026-07-30): "전체 금액 비율을 지정하고 해당 비율대로 투자, 안전/투자자산 비율 초과 시 금액·퍼센트 표시, 투자자산 내 자산군별 비율"
- 설계 선택 7건 확정: 유형 고정 매핑 / 평가액 기준 / 허용밴드 강조 / 투자자산 내부만 세부 비율 / 금 시세만 연동 / 암호화폐 배분 제외 / 대시보드 요약 추가
- issue 단계 진행 승인: 태형님 "진행해" (2026-07-30)

## Branch
- branch: claude/portfolio-auto-payment-check-b6ecy4 (원격 세션 지정 브랜치 — worktree 스크립트 대체)
- base: main (13608fa)
