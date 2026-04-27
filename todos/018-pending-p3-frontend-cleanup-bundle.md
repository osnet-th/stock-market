---
status: pending
priority: p3
issue_id: 018
tags: [code-review, frontend, code-simplicity, portfolio]
dependencies: []
---

# Frontend 매도 모달 코드 정리 묶음 (P3)

## Problem Statement

매도 모달/이력 관련 Alpine.js 코드의 마이너 정리 항목 묶음. 개별 영향은 작지만 후속 유지보수 비용 누적 방지.

## Findings

| # | 위치 | 문제 | 개선 |
|---|---|---|---|
| 1 | `portfolio.js:1086-1093` | `closeSaleModal` 폼 리셋이 마법 객체 리터럴 | `INITIAL_SALE_FORM`/`INITIAL_SALE_CONTEXT` 상수 추출 |
| 2 | `portfolio.js:1183-1212` | `submitSale`의 5중 alert 검증 — 서버 위임 가능 | quantity/salePrice/soldAt/reason은 서버 응답에 위임, 클라이언트 1줄 가드 + 서버 메시지 alert |
| 3 | `portfolio.js:1219-1222` | `const link = ...; if (link && !form.confirmUnrecorded) {/*빈 블록*/}` dead code | 삭제 또는 의도된 검증 추가 |
| 4 | `portfolio.js:1095-1108` | `openSaleModal`이 `closeSaleModal` 직후 또 폼 세팅 | 상수 + spread 패턴으로 통합 |
| 5 | `portfolio.js:1167-1172` | `Math.round(x*100)/100` 5회 반복 | `const round2 = v => Math.round(v * 100) / 100;` 헬퍼 |

## Proposed Solutions

### Option A — 5건 모두 정리 (1 PR)
- 누적 약 30~40 LOC 감소
- 동작 변경 없음 (refactor only)

### Option B — 보류

## Recommended Action

A 권장. 별도 작은 refactor PR로 처리.

## Technical Details

- 영향 파일:
  - `src/main/resources/static/js/components/portfolio.js`

## Acceptance Criteria

- [ ] 각 항목 정리 후 매도 모달 동작 회귀 없음
- [ ] 매도 등록/사후 수정/삭제 정상 동작

## Work Log

- 2026-04-27: ce-review 발견 (code-simplicity-reviewer P3 #11~15)