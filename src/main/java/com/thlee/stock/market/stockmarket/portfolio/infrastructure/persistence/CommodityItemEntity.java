package com.thlee.stock.market.stockmarket.portfolio.infrastructure.persistence;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "commodity_detail")
@DiscriminatorValue("COMMODITY")
public class CommodityItemEntity extends PortfolioItemEntity {

    protected CommodityItemEntity() {
    }

    public CommodityItemEntity(Long id,
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