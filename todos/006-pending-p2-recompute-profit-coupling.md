---
status: pending
priority: p2
issue_id: 006
tags: [code-review, domain-model, code-simplicity, portfolio]
dependencies: []
---

# `update`와 `recomputeProfit` 분리로 인한 application 측 일관성 책임

## Problem Statement

`StockSaleHistory.update(...)` 호출 후 `recomputeProfit(...)`을 항상 별도로 불러야 일관성 유지. 행위 중심 모델링 위반.

## Findings

- `application/PortfolioService.java:751-758` 에서 `history.update(...)` 직후 `history.recomputeProfit(...)` 항상 따라옴
- `domain/model/StockSaleHistory.java:163-180` 두 메서드 분리됨
- 빠뜨리면 profit/profitRate/contributionRate가 stale 상태로 저장됨

## Proposed Solutions

### Option A — `update` 내부에서 마지막에 `recomputeProfit` 자동 호출
```java
public void update(int quantity, BigDecimal salePrice, SaleReason reason, String memo) {
    // ... 필드 변경 ...
    recomputeProfit(this.totalAssetAtSale, this.fxRate);
}
```
- Pros: 단일 호출로 일관성 보장, application 호출 1줄 감소
- Cons: 의존성 명시성 약간 감소

## Recommended Action

A 권장.

## Technical Details

- 영향 파일:
  - `src/main/java/com/thlee/stock/market/stockmarket/portfolio/domain/model/StockSaleHistory.java`
  - `src/main/java/com/thlee/stock/market/stockmarket/portfolio/application/PortfolioService.java`

## Acceptance Criteria

- [ ] `update()` 호출 후 application이 별도 `recomputeProfit` 호출 안 해도 일관성 유지
- [ ] 기존 테스트 통과

## Work Log

- 2026-04-27: ce-review 발견 (code-simplicity-reviewer P1, but downgraded to P2 in synthesis)