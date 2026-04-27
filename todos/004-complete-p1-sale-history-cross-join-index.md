---
status: complete
priority: p1
issue_id: 004
tags: [code-review, performance, database, jpa, portfolio]
dependencies: []
---

# `StockSaleHistory.findByUserId` JPQL implicit cross join + 정렬 인덱스 부재

## Problem Statement

`StockSaleHistoryJpaRepository.java:19-26` 의 사용자 단위 매도 이력 조회 쿼리가 콤마 조인이고, ORDER BY를 위한 복합 인덱스가 없다. 누적량 증가 시 풀스캔 비용 발생.

## Findings

```jpql
SELECT s FROM StockSaleHistoryEntity s, PortfolioItemEntity p
WHERE s.portfolioItemId = p.id AND p.userId = :userId
ORDER BY s.soldAt DESC, s.id DESC
```

- 콤마 조인은 PostgreSQL이 hash join으로 풀어주지만 명시 ANSI JOIN보다 가독성/플랜 안정성 떨어짐
- `(portfolio_item_id, sold_at DESC)` 복합 인덱스 부재 → ORDER BY 시 sort node가 풀스캔
- 매도 이력은 사용자 단위로 누적되며 페이징도 없어서(`todo 016` 참고) 시간 지나면 명백한 병목

## Proposed Solutions

### Option A — ANSI JOIN + 복합 인덱스 추가
```jpql
SELECT s FROM StockSaleHistoryEntity s
JOIN PortfolioItemEntity p ON s.portfolioItemId = p.id
WHERE p.userId = :userId ORDER BY s.soldAt DESC, s.id DESC
```
`StockSaleHistoryEntity`에:
```java
@Index(name = "idx_sale_item_solddate", columnList = "portfolio_item_id, sold_at DESC")
```

## Recommended Action

(triage 시 결정)

## Technical Details

- 영향 파일:
  - `src/main/java/com/thlee/stock/market/stockmarket/portfolio/infrastructure/persistence/StockSaleHistoryJpaRepository.java`
  - `src/main/java/com/thlee/stock/market/stockmarket/portfolio/infrastructure/persistence/StockSaleHistoryEntity.java`

## Acceptance Criteria

- [ ] JPQL을 ANSI JOIN으로 변환
- [ ] 복합 인덱스 추가
- [ ] EXPLAIN으로 sort node 제거 확인 (운영 적용 시)

## Work Log

- 2026-04-27: ce-review 발견 (performance-oracle P1)
- 2026-04-27: 적용 완료
  - `StockSaleHistoryJpaRepository.findByUserId` 콤마 조인 → ANSI `JOIN ... ON` 변환
  - `StockSaleHistoryEntity`에 `@Index(name = "idx_stock_sale_history_item_solddate", columnList = "portfolio_item_id, sold_at")` 추가 (PostgreSQL은 단방향 인덱스로도 reverse scan 효율적)
  - 기존 `idx_stock_sale_history_item_id` 단일 인덱스는 복합 인덱스가 cover하므로 redundant이지만 운영 데이터 영향 회피 위해 유지(후속 정리 항목)