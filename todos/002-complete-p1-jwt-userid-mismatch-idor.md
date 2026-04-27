---
status: complete
priority: p1
issue_id: 002
tags: [code-review, security, authorization, idor, portfolio]
dependencies: []
---

# JWT subject ↔ query `userId` 검증 부재 — 권한 우회 / IDOR (매도 6개 엔드포인트)

## Problem Statement

매도 신규 6개 엔드포인트(`PortfolioController.java:289-366`)가 `@RequestParam Long userId` 만 신뢰한다. JWT subject(authenticated principal)와 query `userId` 일치 검증이 어디에도 없다. 매도는 **금전 이동(보유 수량 차감 + CASH 잔액 변경)**을 동반하므로 영향이 크다.

## Findings

- **위치**: 6개 신규 엔드포인트 + `PortfolioService.findUserItem`(L1310, item.userId 일치만 확인하지만 query userId 자체가 신뢰 불가)
- `JwtAuthenticationFilter`(`infrastructure/security/jwt/JwtAuthenticationFilter.java:39`)가 principal에 userId를 박아두지만 컨트롤러는 `@AuthenticationPrincipal` / `SecurityContextHolder` 미사용
- 시나리오:
  - 인증된 A의 토큰으로 `?userId=B`로 호출 → B의 매도 등록/수정/삭제 가능
  - **`GET /api/portfolio/sales?userId=N`**: 사용자 ID enum만으로 다른 사용자 매도 이력(stockName/memo/profit) 덤프 → PII 노출
  - `depositCashItemId=B의CASH`로 호출 시 B의 보유 주식 수량을 임의로 차감 + CASH 잔액 부풀림(이중 위험)

## Proposed Solutions

### Option A — `@AuthenticationPrincipal Long jwtUserId` 받고 query userId와 동일성 검증
```java
@PostMapping("/items/stock/{itemId}/sale")
public ResponseEntity<StockSaleHistoryResponse> addStockSale(
        @AuthenticationPrincipal Long jwtUserId,
        @RequestParam Long userId,
        @PathVariable Long itemId,
        @Valid @RequestBody StockSaleRequest body) {
    if (!jwtUserId.equals(userId)) throw new AccessDeniedException("..."); 
    ...
}
```
- Pros: 변경 최소, 기존 인터페이스 유지
- Cons: 6개 엔드포인트 일괄 수정

### Option B — query userId 제거, principal에서만 추출
- Pros: 본질적 해결
- Cons: 기존 portfolio API 전체에 파급, breaking change

## Recommended Action

(triage 시 결정)

## Technical Details

- 영향 파일:
  - `src/main/java/com/thlee/stock/market/stockmarket/portfolio/presentation/PortfolioController.java`
  - 동일 결함이 portfolio 기존 API에도 존재 — 매도 PR에서는 매도 API에 한정 적용 권장(별도 PR로 전체 정리)

## Acceptance Criteria

- [ ] 6개 매도 엔드포인트 모두 JWT principal과 query userId 일치 검증
- [ ] 일치 안 함 시 403 Forbidden(또는 404 — info disclosure 방어 측면) 반환
- [ ] dev 환경(`permitAll`)에서 회귀 없는지 확인

## Work Log

- 2026-04-27: ce-review 발견 (security-sentinel P1, agent-native-reviewer P2 동일 지적)
- 2026-04-27: Option A 적용. `PortfolioController` 매도 6개 엔드포인트에 `@AuthenticationPrincipal Long jwtUserId` 추가, `assertUserMatches` 헬퍼로 일치 검증.
  - 불일치 시 `AccessDeniedException` (Spring Security가 403으로 변환)
  - dev 환경(permitAll, anonymous principal)은 jwtUserId가 null이라 검증 건너뜀 → 회귀 없음
  - `PortfolioControllerSaleTest` 6개 호출에 jwtUserId 인자 추가