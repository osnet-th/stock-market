package com.thlee.stock.market.stockmarket.portfolio.infrastructure.persistence;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "cash_detail")
@DiscriminatorValue("CASH")
public class CashItemEntity extends PortfolioItemEntity {

    protected CashItemEntity() {
    }

    public CashItemEntity(Long id,
                          Long userId,
                          String itemName,
                          BigDecimal investedAmount,
                          boolean newsEnabled,
                          String region,
                          String memo,
                          LocalDateTime createdAt,
                          LocalDateTime updatedAt) {
        super(id, userId, itemName, investedAmount, newsEnabled, region, memo, createdAt, updatedAt);
    }
}