---
status: complete
priority: p2
issue_id: 007
tags: [code-review, yagni, dead-code, portfolio]
dependencies: []
---

# YAGNI 위반 — 호출처 0인 메서드 4건 제거

## Problem Statement

CLAUDE.md 규칙 10(YAGNI 원칙) 위반. plan 범위 외 메서드/저장소 메서드가 호출처 없이 추가됨.

## Findings

| 메서드 | 위치 | 호출처 |
|---|---|---|
| `StockSaleHistory.markDepositRecorded()` | `domain/model/StockSaleHistory.java:185-188` | 0 (코드/테스트 모두) |
| `StockSaleHistoryRepository.deleteByPortfolioItemId` | `domain/repository/StockSaleHistoryRepository.java` | 0 |
| `StockSaleHistoryRepository.findByPortfolioItemIdIn` | 동일 | 0 (단, batch N+1 회피용 — 사용 시점이 가까우면 유지) |
| `PortfolioItemRepository.findByUserIdIncludingClosed` | `domain/repository/PortfolioItemRepository.java:35` + Impl L39-44 | 0 |

## Proposed Solutions

### Option A — 호출처 0인 항목 모두 삭제
- `markDepositRecorded`: 시나리오 부재. 삭제.
- `deleteByPortfolioItemId`: 사용 안 됨. 삭제.
- `findByPortfolioItemIdIn`: 배치 패턴이 후속 페이징/이력 그래프 화면에 필요할 가능성. 1주일 내 사용 안 하면 삭제.
- `findByUserIdIncludingClosed`: "마감 항목 탭" 계획이 없으면 삭제.

### Option B — 사용 임박한 1~2건은 주석으로 의도 명시 후 유지

## Recommended Action

A 적용. 필요 시 추가하는 게 YAGNI 본질.

## Technical Details

- 영향 파일:
  - `src/main/java/com/thlee/stock/market/stockmarket/portfolio/domain/model/StockSaleHistory.java`
  - `src/main/java/com/thlee/stock/market/stockmarket/portfolio/domain/repository/StockSaleHistoryRepository.java`
  - `src/main/java/com/thlee/stock/market/stockmarket/portfolio/domain/repository/PortfolioItemRepository.java`
  - 각 Impl

## Acceptance Criteria

- [ ] 위 4건 호출처 재확인 후 0건이면 제거
- [ ] 컴파일/테스트 통과

## Work Log

- 2026-04-27: ce-review 발견 (code-simplicity-reviewer P2)
- 2026-04-27: 호출처 재검증 후 4건 + 의존 메서드 + 단독 테스트 함께 제거
  - `StockSaleHistory.markDepositRecorded()` + `StockSaleHistoryTest.markDepositRecorded_clearsFlag` 테스트
  - `StockSaleHistoryRepository.deleteByPortfolioItemId` (port + Impl) + `StockSaleHistoryJpaRepository.deleteByPortfolioItemId`
  - `StockSaleHistoryRepository.findByPortfolioItemIdIn` (port + Impl) + `StockSaleHistoryJpaRepository.findByPortfolioItemIdInOrderBySoldAtAsc`
  - `PortfolioItemRepository.findByUserIdIncludingClosed` (port + Impl) + `PortfolioItemJpaRepository.findByUserId` + `PortfolioItemRepositoryImplActiveFilterTest.findByUserIdIncludingClosed_returnsAllStatuses`
  - 도메인 RepositoryItemRepository 클래스 javadoc도 *IncludingClosed 언급 정리
  - 컴파일/단위 테스트 통과