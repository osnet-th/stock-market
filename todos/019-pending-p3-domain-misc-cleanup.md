---
status: pending
priority: p3
issue_id: 019
tags: [code-review, domain-model, code-simplicity, portfolio]
dependencies: []
---

# 도메인/응용 계층 마이너 정리 묶음 (P3)

## Problem Statement

작은 가독성/캡슐화 정리 항목 묶음.

## Findings

| # | 위치 | 문제 | 개선 |
|---|---|---|---|
| 1 | `PortfolioItem.java:354-360` | `closeItem()` public이지만 외부 호출처 없음(자기 자신만) | `private` 강등 (단, 매도 사후 수정 흐름이 외부 호출하면 유지) |
| 2 | `PortfolioService.java:865-879` | `applyCashDelta`의 if/else 분기 가독성 | `delta.abs()` 추출 후 선분기 또는 도메인 `cashItem.adjustAmount(delta)` 단일 메서드 |
| 3 | `PortfolioEvaluationService.java:170-174` | `computeTotalAsset` 한 줄 wrapper | 호출자에서 직접 호출 또는 명명 의미 유지 시 보존 |
| 4 | `StockSaleHistory.create:191-219` | `portfolioItemId == null`, `stockName == null/blank`, `currency.length() == 3` 검증 누락 | validateInputs 보강 |
| 5 | `SaleReason` enum | description 필드 부재 (챗봇 어휘 / Swagger 표현) | enum constructor + description getter 추가 |
| 6 | `POST /sale` 응답 | 200 OK인데 신규 리소스라 `201 Created`가 RESTful | 기존 컨벤션 유지로 보류 권장 |

## Proposed Solutions

### Option A — 항목 1~5 정리 (1 PR)
### Option B — 사용 시점 발생 시 개별 처리

## Recommended Action

5번(SaleReason description)은 todo 005(챗봇 컨텍스트)와 같이 처리하면 시너지. 4번(create 검증)은 즉시. 1·2·3은 시간 여유 있을 때.

## Technical Details

- 영향 파일:
  - `src/main/java/com/thlee/stock/market/stockmarket/portfolio/domain/model/PortfolioItem.java`
  - `src/main/java/com/thlee/stock/market/stockmarket/portfolio/domain/model/StockSaleHistory.java`
  - `src/main/java/com/thlee/stock/market/stockmarket/portfolio/application/PortfolioService.java`
  - `src/main/java/com/thlee/stock/market/stockmarket/portfolio/application/PortfolioEvaluationService.java`
  - `src/main/java/com/thlee/stock/market/stockmarket/portfolio/domain/model/enums/SaleReason.java`

## Acceptance Criteria

- [ ] 항목별 정리 후 회귀 없음
- [ ] 도메인 검증 보강 단위 테스트 추가(권장)

## Work Log

- 2026-04-27: ce-review 발견 (code-simplicity-reviewer P2/P3, architecture-strategist P2-4, agent-native-reviewer P3)