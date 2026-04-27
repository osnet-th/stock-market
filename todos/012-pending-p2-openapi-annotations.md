---
status: pending
priority: p2
issue_id: 012
tags: [code-review, agent-native, documentation, portfolio]
dependencies: []
---

# 매도 컨트롤러 OpenAPI 어노테이션 부재 — 외부 에이전트 어휘 부재

## Problem Statement

`springdoc-openapi-starter-webmvc-ui` 의존성은 있으나 매도 컨트롤러 6개 메서드에 `@Operation`/`@Tag`/`@Schema` 등 어노테이션이 전무. 외부 에이전트(MCP, function-calling)가 의미를 파악할 수 없는 자동 생성 스펙만 노출.

## Findings

- 위치: `portfolio/presentation/PortfolioController.java:289-366`
- 특히 `SaleReason` enum 값 의미가 스펙에 안 보임 (TARGET_PRICE_REACHED, STOP_LOSS, ...)
- 현재 codebase가 controller에 Swagger 어노테이션을 사용하는지 컨벤션 확인 필요(없으면 본 PR에서만 도입은 컨벤션 외)

## Proposed Solutions

### Option A — 매도 컨트롤러 6개 메서드 + DTO에 어노테이션 추가
```java
@Operation(summary = "주식 매도 등록", description = "...")
@PostMapping("/items/stock/{itemId}/sale")
public ResponseEntity<StockSaleHistoryResponse> addStockSale(...)

// DTO
@Schema(description = "매도 사유. TARGET_PRICE_REACHED=목표가 도달, STOP_LOSS=손절, ...")
private SaleReason reason;
```

### Option B — 본 PR 스코프 외로 분리, 별도 "API 문서화" 스코프 PR

## Recommended Action

기존 컨트롤러 컨벤션 확인 후 결정. 컨벤션이 어노테이션 없는 쪽이면 B.

## Technical Details

- 영향 파일:
  - `src/main/java/com/thlee/stock/market/stockmarket/portfolio/presentation/PortfolioController.java`
  - `src/main/java/com/thlee/stock/market/stockmarket/portfolio/presentation/dto/StockSaleRequest.java`
  - `src/main/java/com/thlee/stock/market/stockmarket/portfolio/presentation/dto/StockSaleHistoryUpdateRequest.java`
  - `src/main/java/com/thlee/stock/market/stockmarket/portfolio/domain/model/enums/SaleReason.java`

## Acceptance Criteria

- [ ] (A 채택 시) `/swagger-ui/index.html` 에서 매도 API 의미 파악 가능
- [ ] enum 값 description 표시

## Work Log

- 2026-04-27: ce-review 발견 (agent-native-reviewer P1)