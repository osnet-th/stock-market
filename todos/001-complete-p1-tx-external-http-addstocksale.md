---
status: complete
priority: p1
issue_id: 001
tags: [code-review, performance, architecture, transaction, portfolio]
dependencies: []
---

# 매도 트랜잭션 내 외부 HTTP 호출 (KIS/환율) — institutional learning 위반

## Problem Statement

`PortfolioService.addStockSale`(`PortfolioService.java:614-670`)이 `@Transactional` 안에서 다음을 호출한다.

- `resolveFxRate(...)` → `exchangeRatePort.getRate(...)` (한국수출입은행 RestClient)
- `portfolioEvaluationService.computeTotalAsset(userId)` (보유 STOCK N개에 대해 KIS `StockPriceService.getPrice` 루프)

`docs/solutions/architecture-patterns/external-http-per-item-transaction-isolation-2026-04-26.md` 패턴(plan L62에서 본인이 인용)을 정면 위반.

## Findings

- **trace**: `addStockSale` → `resolveFxRate (외부 HTTP)` → `computeTotalAsset` → `evaluatePortfolios` → 사용자 보유 STOCK 수만큼 `stockPriceService.getPrice` 동기 호출
- **영향**:
  - HikariCP 커넥션 hold time 1~2.5초(KIS 200~500ms × 종목 수)
  - PostgreSQL row lock 보유 시간 증가 → `@Version` 경합 확률 상승 → OptimisticLock 빈발
  - KIS/환율 API 장애 시 매도 API 전체 timeout
- **`getSaleContext`도 동일** — 클래스 레벨 `@Transactional(readOnly=true)`로 외부 HTTP를 트랜잭션 내부에서 부름

## Proposed Solutions

### Option A — 트랜잭션 진입 전 외부 호출 선행
```java
public StockSaleHistoryResponse addStockSale(Long userId, Long stockItemId, AddStockSaleParam param) {
    PreparedSaleSnapshot snapshot = prepareSnapshot(userId, stockItemId, param); // non-tx, KIS/FX 호출
    return persistSale(userId, stockItemId, param, snapshot); // @Transactional, DB만
}
```
- Pros: 가장 안전, 트랜잭션 짧음
- Cons: 시그니처 분리 필요
- Effort: Medium / Risk: Low

### Option B — 외부 호출 메서드만 `@Transactional(propagation=NOT_SUPPORTED)` 분리
- Pros: 변경 최소
- Cons: propagation 직접 지정 → 가독성 낮아짐
- Effort: Small / Risk: Medium

## Recommended Action

(triage 시 결정)

## Technical Details

- 영향 파일:
  - `src/main/java/com/thlee/stock/market/stockmarket/portfolio/application/PortfolioService.java`
  - `src/main/java/com/thlee/stock/market/stockmarket/portfolio/application/PortfolioEvaluationService.java`
- 관련 institutional learning: `docs/solutions/architecture-patterns/external-http-per-item-transaction-isolation-2026-04-26.md`

## Acceptance Criteria

- [ ] `addStockSale` 트랜잭션 안에서 외부 HTTP 호출 0건 (메서드/래퍼 레벨 검증)
- [ ] `getSaleContext` 클래스 레벨 readOnly 트랜잭션이 외부 호출에 적용되지 않음
- [ ] 동일 종목 동시 매도 시 KIS 응답 지연이 OptimisticLock 충돌율에 영향 주지 않음(부하 테스트)

## Work Log

- 2026-04-27: ce-review 발견 (architecture-strategist + performance-oracle 양쪽 P1)
- 2026-04-27: Option A 적용. `ObjectProvider<PortfolioService> selfProvider` 자기 참조 패턴 도입.
  - `addStockSale` (NOT_SUPPORTED) → `prepareStockSaleSnapshot` (NOT_SUPPORTED, 외부 호출) → `persistStockSale` (Transactional, DB만) 3단 분리
  - `getSaleContext` 도 `@Transactional(propagation = NOT_SUPPORTED)` 적용
  - `SaleSnapshot` private record 추가
  - `PortfolioServiceAddStockSaleTest`에 `@Mock ObjectProvider` + self stub 추가
  - 단위 테스트 통과