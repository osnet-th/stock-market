---
status: pending
priority: p2
issue_id: 016
tags: [code-review, agent-native, api-consistency, portfolio]
dependencies: [003]
---

# `StockSaleHistoryResponse`에 `@JsonInclude(NON_NULL)` 미적용 — KRW 종목에서 토큰 낭비

## Problem Statement

`PortfolioItemResponse`(`application/dto/PortfolioItemResponse.java:11`)는 `@JsonInclude(JsonInclude.Include.NON_NULL)`을 사용하는데 `StockSaleHistoryResponse`(`application/dto/StockSaleHistoryResponse.java`)는 사용하지 않음. KRW 종목에서 `salePriceKrw`/`profitKrw`/`fxRate`가 항상 채워져 토큰 낭비 + LLM 입력에 잡음.

## Findings

- 위치: `application/dto/StockSaleHistoryResponse.java:11`
- KRW 종목은 `fxRate=null`, `salePriceKrw=null`, `profitKrw=null`이 자연스러운 의미인데 직렬화에서 누락 안 됨
- `unrecordedDeposit` 같은 UI 전용 플래그도 LLM 입력에 섞임

## Proposed Solutions

### Option A — `@JsonInclude(NON_NULL)` 추가
- todo 003의 Lombok 적용과 함께 처리하면 효율적

### Option B — KRW 종목에서 명시적으로 null 세팅 (현재 0/공백이면 그대로)
- service 레이어에서 `currency == "KRW" ? null : value` 분기 추가

## Recommended Action

A + B 조합. todo 003과 동시 처리.

## Technical Details

- 영향 파일:
  - `src/main/java/com/thlee/stock/market/stockmarket/portfolio/application/dto/StockSaleHistoryResponse.java`
  - `src/main/java/com/thlee/stock/market/stockmarket/portfolio/application/PortfolioService.java` (또는 from() 정적 팩토리 안에서 KRW 분기)

## Acceptance Criteria

- [ ] KRW 매도 응답에서 `fxRate/salePriceKrw/profitKrw` 키가 빠짐
- [ ] 외화 매도 응답은 동일

## Work Log

- 2026-04-27: ce-review 발견 (agent-native-reviewer P2)