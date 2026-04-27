---
status: pending
priority: p2
issue_id: 008
tags: [code-review, plan-compliance, domain-model, portfolio]
dependencies: []
---

# `restoreStockQuantity` plan 명세에 없음 — plan-구현 일치성 위반 + `deductStockQuantity`와 통합 가능

## Problem Statement

CLAUDE.md "설계 이탈 금지" 규칙 위반 회색지대. plan(`docs/plans/2026-04-26-001-feat-portfolio-stock-sale-plan.md`) Unit 2의 신규 메서드 명단에는 `closeItem/reopenItem/deductStockQuantity` 만 있고, `restoreStockQuantity`는 어디에도 없음.

## Findings

- 위치: `PortfolioItem.java:380-405`
- 사후 수정/삭제(Unit 7)에서 수량 복원이 필요해 도입된 것으로 보이며 동작은 합리적
- `deductStockQuantity`(L407-447)와 거의 대칭 (StockDetail 9-필드 재생성 + quantity 계산 + status 분기 차이만)
- plan-구현 일치성 + 단순화 양쪽 이슈

## Proposed Solutions

### Option A — plan Unit 7 approach에 `restoreStockQuantity` 추가 명시 (사후 보완)
- 가장 가벼운 해결, 코드 변경 없음

### Option B — `applyStockQuantityDelta(int delta)` 단일 메서드로 통합
- `delta > 0` → 증가(=현재 restore), `delta < 0` → 감소(=현재 deduct)
- 새 quantity == 0 시 자동 closeItem
- `replaceQuantity(int newQuantity)` 헬퍼 추출로 StockDetail 재생성 중복 제거 (~25 LOC 감소)

### Option C — A + B 모두

## Recommended Action

A는 즉시. B는 별도 PR로 분리(리팩토링 — CLAUDE.md "요청받지 않은 리팩토링 금지" 규칙으로 재승인 필요).

## Technical Details

- 영향 파일:
  - `docs/plans/2026-04-26-001-feat-portfolio-stock-sale-plan.md` (Unit 7 approach 보강)
  - `src/main/java/com/thlee/stock/market/stockmarket/portfolio/domain/model/PortfolioItem.java`

## Acceptance Criteria

- [ ] plan Unit 7에 restoreStockQuantity 추가 근거 명시 (또는 통합 결정 명시)
- [ ] (B 채택 시) 기존 테스트 통과

## Work Log

- 2026-04-27: ce-review 발견 (architecture-strategist P2-1, code-simplicity-reviewer P2 #9)