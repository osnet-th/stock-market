# 인프라 계층 예시

## StockPurchaseHistoryEntity

```java
package com.thlee.stock.market.stockmarket.portfolio.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "stock_purchase_history",
    indexes = {
        @Index(name = "idx_purchase_history_item_id", columnList = "portfolio_item_id")
    }
)
@Getter
public class StockPurchaseHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "portfolio_item_id", nullable = false)
    private Long portfolioItemId;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "purchase_price", nullable = false, precision = 18, scale = 2)
    private BigDecimal purchasePrice;

    @Column(name = "purchased_at", nullable = false)
    private LocalDate purchasedAt;

    @Column(name = "memo", length = 200)
    private String memo;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected StockPurchaseHistoryEntity() {
    }

    public StockPurchaseHistoryEntity(Long id, Long portfolioItemId, int quantity,
                                       BigDecimal purchasePrice, LocalDate purchasedAt,
                                       String memo, LocalDateTime createdAt) {
        this.id = id;
        this.portfolioItemId = portfolioItemId;
        this.quantity = quantity;
        this.purchasePrice = purchasePrice;
        this.purchasedAt = purchasedAt;
        this.memo = memo;
        this.createdAt = createdAt;
    }
}
```

## StockPurchaseHistoryJpaRepository

```java
package com.thlee.stock.market.stockmarket.portfolio.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockPurchaseHistoryJpaRepository extends JpaRepository<StockPurchaseHistoryEntity, Long> {
    List<StockPurchaseHistoryEntity> findByPortfolioItemIdOrderByPurchasedAtAsc(Long portfolioItemId);
    void deleteByPortfolioItemId(Long portfolioItemId);
}
```

## StockPurchaseHistoryRepositoryImpl

```java
package com.thlee.stock.market.stockmarket.portfolio.infrastructure.persistence;

import com.thlee.stock.market.stockmarket.portfolio.domain.model.StockPurchaseHistory;
import com.thlee.stock.market.stockmarket.portfolio.domain.repository.StockPurchaseHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class StockPurchaseHistoryRepositoryImpl implements StockPurchaseHistoryRepository {

    private final StockPurchaseHistoryJpaRepository jpaRepository;

    @Override
    public StockPurchaseHistory save(StockPurchaseHistory history) {
        StockPurchaseHistoryEntity entity = toEntity(history);
        StockPurchaseHistoryEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<StockPurchaseHistory> findById(Long id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<StockPurchaseHistory> findByPortfolioItemId(Long portfolioItemId) {
        return jpaRepository.findByPortfolioItemIdOrderByPurchasedAtAsc(portfolioItemId)
                .stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void delete(StockPurchaseHistory history) {
        jpaRepository.deleteById(history.getId());
    }

    @Override
    public void deleteByPortfolioItemId(Long portfolioItemId) {
        jpaRepository.deleteByPortfolioItemId(portfolioItemId);
    }

    private StockPurchaseHistoryEntity toEntity(StockPurchaseHistory h) {
        return new StockPurchaseHistoryEntity(
                h.getId(), h.getPortfolioItemId(), h.getQuantity(),
                h.getPurchasePrice(), h.getPurchasedAt(), h.getMemo(), h.getCreatedAt()
        );
    }

    private StockPurchaseHistory toDomain(StockPurchaseHistoryEntity e) {
        return new StockPurchaseHistory(
                e.getId(), e.getPortfolioItemId(), e.getQuantity(),
                e.getPurchasePrice(), e.getPurchasedAt(), e.getMemo(), e.getCreatedAt()
        );
    }
}
```