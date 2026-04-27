package com.thlee.stock.market.stockmarket.portfolio.infrastructure.persistence;

import com.thlee.stock.market.stockmarket.portfolio.domain.model.enums.PortfolioItemStatus;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "other_detail")
@DiscriminatorValue("OTHER")
public class OtherItemEntity extends PortfolioItemEntity {

    protected OtherItemEntity() {
    }

    public OtherItemEntity(Long id,
                           Long userId,
                           String itemName,
                           BigDecimal investedAmount,
                           boolean newsEnabled,
                           String region,
                           String memo,
                           PortfolioItemStatus status,
                           LocalDateTime createdAt,
                           LocalDateTime updatedAt) {
        super(id, userId, itemName, investedAmount, newsEnabled, region, memo, status, createdAt, updatedAt);
    }
}