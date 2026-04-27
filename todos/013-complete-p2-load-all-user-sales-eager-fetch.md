---
status: complete
priority: p2
issue_id: 013
tags: [code-review, performance, frontend, portfolio]
dependencies: []
---

# `loadAllUserSales`가 보유 카드 disabled 판정용으로 매번 전체 매도 이력 페치

## Problem Statement

`loadPortfolio()` 마지막에 무조건 `loadAllUserSales()` 호출 → `/api/portfolio/sales`가 사용자 전체 매도 이력을 매번 가져옴(현재 페이징 없음). 매도 이력이 1000건+ 누적 시 매 화면 갱신마다 1000행 transfer.

## Findings

- 위치:
  - `static/js/components/portfolio.js:161` — `loadPortfolio` 끝에서 호출
  - `static/js/components/portfolio.js:1224, 1234-1247` — `loadAllUserSales`
  - `static/index.html:1342, 1577` — `hasSaleHistories(item.id)`, `groupSalesByMonth()` 가 Alpine 표현식으로 매 렌더 호출
- 디스에이블 판정 전용으로 사용자 전체 매도 이력 매번 가져옴
- `groupSalesByMonth()` 는 Alpine reactive 환경에서 매 키스트로크/입력에 O(n) 그룹핑 수행

## Proposed Solutions

### Option A — 경량 엔드포인트 분리
- `GET /api/portfolio/sales/item-ids` 추가, `Long[]` 만 반환 (디스에이블 판정 전용)
- 보유 카드 렌더에는 이것만 사용
- 매도 이력 탭 진입 시에만 `getAllUserSaleHistories` 호출

### Option B — `groupSalesByMonth` 결과 캐시
- `loadAllUserSales` 직후 `this.portfolio.salesGrouped`에 저장
- 템플릿은 캐시된 배열 참조

### Option C — 둘 다

## Recommended Action

C 권장. 추가로 todo 016(페이징)과 묶어 처리.

## Technical Details

- 영향 파일:
  - `src/main/resources/static/js/components/portfolio.js`
  - `src/main/resources/static/index.html`
  - `src/main/java/com/thlee/stock/market/stockmarket/portfolio/presentation/PortfolioController.java` (신규 엔드포인트)

## Acceptance Criteria

- [ ] 보유 카드 렌더에 매도 이력 풀 페치 불필요
- [ ] 매도 이력 탭 진입 시에만 풀 데이터 로드
- [ ] `groupSalesByMonth` 결과 캐시 적용

## Work Log

- 2026-04-27: ce-review 발견 (performance-oracle P2)
- 2026-04-28: A + C 적용 (B는 매도 탭이 입력 폼 없는 읽기 전용이라 우선순위 낮음, 보류)
  - A. 경량 엔드포인트 신설:
    - `StockSaleHistoryJpaRepository.findItemIdsByUserId` (`SELECT DISTINCT s.portfolioItemId ... JOIN PortfolioItemEntity p ... WHERE p.userId = :userId`)
    - port `StockSaleHistoryRepository.findItemIdsByUserId` + Impl
    - `PortfolioService.getSaleItemIds`
    - `GET /api/portfolio/sales/item-ids` (JWT 일치 검증 포함)
    - `api.js: getSaleItemIds(userId)`
  - C. 흐름 분리:
    - `loadPortfolio()` 끝에서 `loadAllUserSales()` → `loadSaleItemIds()` (경량) 로 교체
    - `loadAllUserSales()` 는 `setActiveTab('sales')` / 매도 등록·수정·삭제 시점에만 호출 (기존 유지)
    - 응답 페이로드: 매도 1000건 사용자 기준 약 ~수백 KB → 수십 바이트로 축소
  - 컴파일/단위 테스트 통과