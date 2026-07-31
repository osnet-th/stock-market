package com.thlee.stock.market.stockmarket.portfolio.infrastructure.persistence;

import com.thlee.stock.market.stockmarket.portfolio.domain.model.enums.PortfolioItemStatus;
import jakarta.persistence.*;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "gold_detail")
@DiscriminatorValue("GOLD")
@Getter
public class GoldItemEntity extends PortfolioItemEntity {

    @Column(name = "quantity_grams", precision = 12, scale = 3)
    private BigDecimal quantityGrams;

    protected GoldItemEntity() {
    }

    public GoldItemEntity(Long id,
                          Long userId,
                          String itemName,
                          BigDecimal investedAmount,
                          boolean newsEnabled,
                          String region,
                          String memo,
                          PortfolioItemStatus status,
                          Long version,
                          LocalDateTime createdAt,
                          LocalDateTime updatedAt,
                          BigDecimal quantityGrams) {
        super(id, userId, itemName, investedAmount, newsEnabled, region, memo, status, version, createdAt, updatedAt);
        this.quantityGrams = quantityGrams;
    }
}
