---
status: pending
priority: p2
issue_id: 010
tags: [code-review, security, validation, portfolio]
dependencies: []
---

# `fxRate` 사용자 입력 boundary 검증 부재 — 잔액 음수화 위험

## Problem Statement

`StockSaleRequest.fxRate`(`presentation/dto/StockSaleRequest.java`)에 `@DecimalMin/@DecimalMax` 등 boundary 검증이 없다. 음수, 0, 1e10 같은 값을 넣으면 `computeSalePriceKrw`(`PortfolioService.java:857`)가 그대로 계산하여 CASH 잔액에 반영.

## Findings

- 본인 데이터지만 `applyCashDelta`에서 음수 KRW 입금이 가능 → CASH 잔액 음수화
- 총자산/수익률 통계 오염
- update 메서드 `StockSaleHistoryUpdateRequest`에는 fxRate 필드 자체가 없어 OK (현재 설계상 fxRate 변경 불가)

## Proposed Solutions

### Option A — Bean Validation 추가
```java
@DecimalMin("0.0001")
@DecimalMax("1000000")
private BigDecimal fxRate;
```

### Option B — 도메인 메서드에서 검증
- `PortfolioService.resolveFxRate`에서 음수/0 검증

### Option C — A + B 모두 (boundary)

## Recommended Action

A + 짧은 service 단 검증. (도메인 검증은 이미 `StockSaleHistory.validateInputs`에 포함되었는지 확인 필요)

## Technical Details

- 영향 파일:
  - `src/main/java/com/thlee/stock/market/stockmarket/portfolio/presentation/dto/StockSaleRequest.java`
  - `src/main/java/com/thlee/stock/market/stockmarket/portfolio/application/PortfolioService.java`

## Acceptance Criteria

- [ ] `fxRate < 0.0001` 또는 `> 1000000` 입력 시 400 Bad Request
- [ ] CASH 잔액 음수화 회귀 테스트 통과

## Work Log

- 2026-04-27: ce-review 발견 (security-sentinel P2)