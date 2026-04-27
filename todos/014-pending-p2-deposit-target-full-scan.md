---
status: pending
priority: p2
issue_id: 014
tags: [code-review, performance, database, portfolio]
dependencies: []
---

# `resolveDepositTarget` 의 사용자 항목 전체 스캔

## Problem Statement

`PortfolioService.java:893` 의 `userHasAnyCash` 판정이 사용자 전체 PortfolioItem을 메모리로 끌어와 stream filter.

## Findings

```java
boolean userHasAnyCash = portfolioItemRepository.findByUserId(userId).stream()
        .anyMatch(i -> i.getAssetType() == AssetType.CASH);
```

- "CASH 0개" 분기 전용
- STOCK/FUND/REAL_ESTATE 등 모든 자산을 함께 로딩하여 단순 존재 확인에 사용
- 매도 1건마다 호출됨

## Proposed Solutions

### Option A — `existsByUserIdAndAssetType` 메서드 추가, 단일 EXISTS 쿼리
```java
// PortfolioItemJpaRepository
boolean existsByUserIdAndAssetTypeAndStatus(Long userId, AssetType assetType, PortfolioItemStatus status);
```

## Recommended Action

A 적용. 기존 portfolio API에서도 유사 패턴 있다면 함께 정리.

## Technical Details

- 영향 파일:
  - `src/main/java/com/thlee/stock/market/stockmarket/portfolio/infrastructure/persistence/PortfolioItemJpaRepository.java`
  - `src/main/java/com/thlee/stock/market/stockmarket/portfolio/domain/repository/PortfolioItemRepository.java`
  - `src/main/java/com/thlee/stock/market/stockmarket/portfolio/application/PortfolioService.java`

## Acceptance Criteria

- [ ] 매도 1건당 사용자 전체 항목 fetch 없음 (EXPLAIN/로그 검증)
- [ ] 기존 동작 동일

## Work Log

- 2026-04-27: ce-review 발견 (performance-oracle P2, code-simplicity-reviewer P2 #8)