# 프레젠테이션 계층 예시

## PortfolioController 추가 API

```java
// PortfolioController.java에 추가

/**
 * 매수이력 조회
 */
@GetMapping("/items/stock/{itemId}/purchases")
public ResponseEntity<List<StockPurchaseHistoryResponse>> getPurchaseHistories(
        @RequestParam Long userId,
        @PathVariable Long itemId) {
    List<StockPurchaseHistoryResponse> responses = portfolioService.getPurchaseHistories(userId, itemId);
    return ResponseEntity.ok(responses);
}

/**
 * 매수이력 수정
 */
@PutMapping("/items/stock/{itemId}/purchases/{historyId}")
public ResponseEntity<PortfolioItemResponse> updatePurchaseHistory(
        @RequestParam Long userId,
        @PathVariable Long itemId,
        @PathVariable Long historyId,
        @RequestBody StockPurchaseHistoryUpdateRequest request) {
    PortfolioItemResponse response = portfolioService.updatePurchaseHistory(
            userId, itemId, historyId,
            request.getQuantity(), request.getPurchasePrice(),
            request.getPurchasedAt(), request.getMemo());
    return ResponseEntity.ok(response);
}

/**
 * 매수이력 삭제
 */
@DeleteMapping("/items/stock/{itemId}/purchases/{historyId}")
public ResponseEntity<Void> deletePurchaseHistory(
        @RequestParam Long userId,
        @PathVariable Long itemId,
        @PathVariable Long historyId) {
    portfolioService.deletePurchaseHistory(userId, itemId, historyId);
    return ResponseEntity.noContent().build();
}
```

## StockPurchaseHistoryUpdateRequest

```java
package com.thlee.stock.market.stockmarket.portfolio.presentation.dto;

import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
public class StockPurchaseHistoryUpdateRequest {
    private Integer quantity;
    private BigDecimal purchasePrice;
    private LocalDate purchasedAt;
    private String memo;
}
```