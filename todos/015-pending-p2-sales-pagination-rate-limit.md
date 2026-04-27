---
status: pending
priority: p2
issue_id: 015
tags: [code-review, performance, security, portfolio]
dependencies: []
---

# 매도 이력 페이징 + 매도 등록/평가 API 레이트리밋 부재

## Problem Statement

두 가지가 결합된 운영 리스크:
1. `GET /api/portfolio/sales` 가 페이징 없이 전체 반환 — 누적 시 메모리 폭주
2. 매도 등록(`POST /sale`)이 매 요청마다 `computeTotalAsset` → KIS 외부 호출 발생, 무제한 → 외부 API 쿼터 소진/비용

## Findings

- 위치:
  - `PortfolioController.java:362-366` (getAllUserSaleHistories)
  - `StockSaleHistoryJpaRepository.findByUserId`
  - `PortfolioController.java:289, 304` (sale-context, addStockSale)
- plan에 "초기는 단순 전체 조회, 누적량 모니터링 후 페이징 도입"으로 명시되어 있음 (deferred 결정)
- favorite 도메인의 `RefreshRateLimitExceededException` 패턴 재사용 가능

## Proposed Solutions

### Option A — Pageable 도입 (페이징)
```java
@GetMapping("/sales")
public ResponseEntity<Page<StockSaleHistoryResponse>> getAllUserSaleHistories(
        @RequestParam Long userId, @PageableDefault(size = 50) Pageable pageable) { ... }
```
프론트는 무한 스크롤 또는 월별 그룹 단위 페이지 로딩

### Option B — favorite 패턴 재사용한 사용자당 분당 N건 제한 어드바이스

### Option C — A + B

## Recommended Action

페이징은 누적 임계 도달 시점에 도입(plan 결정 존중). 레이트리밋은 매도 등록에 우선 도입(외부 API 보호).

## Technical Details

- 영향 파일:
  - `src/main/java/com/thlee/stock/market/stockmarket/portfolio/presentation/PortfolioController.java`
  - `src/main/java/com/thlee/stock/market/stockmarket/portfolio/infrastructure/persistence/StockSaleHistoryJpaRepository.java`
  - `src/main/java/com/thlee/stock/market/stockmarket/favorite/...` (RefreshRateLimitExceededException 패턴 참고)

## Acceptance Criteria

- [ ] `GET /sales` Pageable 적용 또는 명시적 임계 도달 시점까지 보류
- [ ] `POST /sale` 사용자당 분당 N건 제한
- [ ] 동시성 이슈 없음

## Work Log

- 2026-04-27: ce-review 발견 (performance-oracle P3, security-sentinel P2)