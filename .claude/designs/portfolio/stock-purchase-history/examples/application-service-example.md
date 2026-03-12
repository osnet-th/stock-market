# 애플리케이션 서비스 예시

## PortfolioService 변경 부분

```java
// 기존 필드에 추가
private final StockPurchaseHistoryRepository purchaseHistoryRepository;

/**
 * 주식 항목 등록 - 최초 매수이력 자동 생성
 */
@Transactional
public PortfolioItemResponse addStockItem(Long userId, String itemName,
                                           String region, String memo,
                                           String subType, String stockCode, String market,
                                           String exchangeCode, String country,
                                           Integer quantity, BigDecimal purchasePrice, BigDecimal dividendYield) {
    // ... 기존 로직 유지 ...
    PortfolioItem saved = portfolioItemRepository.save(item);

    // 최초 매수이력 생성
    StockPurchaseHistory history = StockPurchaseHistory.create(
            saved.getId(), quantity, purchasePrice, LocalDate.now(), null);
    purchaseHistoryRepository.save(history);

    return PortfolioItemResponse.from(saved);
}

/**
 * 주식 추가 매수 - 이력 저장 후 재계산
 */
@Transactional
public PortfolioItemResponse addStockPurchase(Long userId, Long itemId,
                                               Integer quantity, BigDecimal purchasePrice) {
    PortfolioItem item = findUserItem(userId, itemId);

    // 매수이력 저장
    StockPurchaseHistory history = StockPurchaseHistory.create(
            itemId, quantity, purchasePrice, LocalDate.now(), null);
    purchaseHistoryRepository.save(history);

    // 전체 이력 기반 재계산
    List<StockPurchaseHistory> histories = purchaseHistoryRepository.findByPortfolioItemId(itemId);
    item.recalculateFromPurchaseHistories(histories);

    PortfolioItem saved = portfolioItemRepository.save(item);
    return PortfolioItemResponse.from(saved);
}

/**
 * 매수이력 조회
 */
public List<StockPurchaseHistoryResponse> getPurchaseHistories(Long userId, Long itemId) {
    findUserItem(userId, itemId); // 권한 검증
    return purchaseHistoryRepository.findByPortfolioItemId(itemId).stream()
            .map(StockPurchaseHistoryResponse::from)
            .collect(Collectors.toList());
}

/**
 * 매수이력 수정 + 재계산
 */
@Transactional
public PortfolioItemResponse updatePurchaseHistory(Long userId, Long itemId, Long historyId,
                                                    Integer quantity, BigDecimal purchasePrice,
                                                    LocalDate purchasedAt, String memo) {
    PortfolioItem item = findUserItem(userId, itemId);

    StockPurchaseHistory history = purchaseHistoryRepository.findById(historyId)
            .orElseThrow(() -> new IllegalArgumentException("매수 이력을 찾을 수 없습니다."));
    if (!history.getPortfolioItemId().equals(itemId)) {
        throw new IllegalArgumentException("해당 항목의 매수 이력이 아닙니다.");
    }

    history.update(quantity, purchasePrice, purchasedAt, memo);
    purchaseHistoryRepository.save(history);

    // 전체 이력 기반 재계산
    List<StockPurchaseHistory> histories = purchaseHistoryRepository.findByPortfolioItemId(itemId);
    item.recalculateFromPurchaseHistories(histories);

    PortfolioItem saved = portfolioItemRepository.save(item);
    return PortfolioItemResponse.from(saved);
}

/**
 * 매수이력 삭제 + 재계산
 */
@Transactional
public PortfolioItemResponse deletePurchaseHistory(Long userId, Long itemId, Long historyId) {
    PortfolioItem item = findUserItem(userId, itemId);

    StockPurchaseHistory history = purchaseHistoryRepository.findById(historyId)
            .orElseThrow(() -> new IllegalArgumentException("매수 이력을 찾을 수 없습니다."));
    if (!history.getPortfolioItemId().equals(itemId)) {
        throw new IllegalArgumentException("해당 항목의 매수 이력이 아닙니다.");
    }

    // 최소 1건 검증
    List<StockPurchaseHistory> histories = purchaseHistoryRepository.findByPortfolioItemId(itemId);
    if (histories.size() <= 1) {
        throw new IllegalArgumentException("매수 이력은 최소 1건 이상이어야 합니다.");
    }

    purchaseHistoryRepository.delete(history);

    // 삭제 후 남은 이력으로 재계산
    histories.removeIf(h -> h.getId().equals(historyId));
    item.recalculateFromPurchaseHistories(histories);

    PortfolioItem saved = portfolioItemRepository.save(item);
    return PortfolioItemResponse.from(saved);
}

/**
 * 항목 삭제 시 매수이력도 함께 삭제 (기존 deleteItem 수정)
 */
@Transactional
public void deleteItem(Long userId, Long itemId) {
    PortfolioItem item = findUserItem(userId, itemId);
    purchaseHistoryRepository.deleteByPortfolioItemId(itemId); // 추가
    newsCleanupService.deleteSourceAndCleanOrphans(NewsPurpose.PORTFOLIO, item.getId());
    portfolioItemRepository.delete(item);
}
```

## StockPurchaseHistoryResponse

```java
package com.thlee.stock.market.stockmarket.portfolio.application.dto;

import com.thlee.stock.market.stockmarket.portfolio.domain.model.StockPurchaseHistory;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
public class StockPurchaseHistoryResponse {
    private final Long id;
    private final Long portfolioItemId;
    private final int quantity;
    private final BigDecimal purchasePrice;
    private final BigDecimal totalCost;
    private final LocalDate purchasedAt;
    private final String memo;
    private final LocalDateTime createdAt;

    private StockPurchaseHistoryResponse(Long id, Long portfolioItemId, int quantity,
                                          BigDecimal purchasePrice, BigDecimal totalCost,
                                          LocalDate purchasedAt, String memo, LocalDateTime createdAt) {
        this.id = id;
        this.portfolioItemId = portfolioItemId;
        this.quantity = quantity;
        this.purchasePrice = purchasePrice;
        this.totalCost = totalCost;
        this.purchasedAt = purchasedAt;
        this.memo = memo;
        this.createdAt = createdAt;
    }

    public static StockPurchaseHistoryResponse from(StockPurchaseHistory history) {
        return new StockPurchaseHistoryResponse(
                history.getId(),
                history.getPortfolioItemId(),
                history.getQuantity(),
                history.getPurchasePrice(),
                history.getTotalCost(),
                history.getPurchasedAt(),
                history.getMemo(),
                history.getCreatedAt()
        );
    }
}
```