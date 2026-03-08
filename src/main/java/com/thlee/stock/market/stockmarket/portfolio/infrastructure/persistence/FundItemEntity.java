package com.thlee.stock.market.stockmarket.portfolio.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "fund_detail")
@DiscriminatorValue("FUND")
@Getter
public class FundItemEntity extends PortfolioItemEntity {

    @Column(name = "sub_type", length = 20)
    private String subType;

    @Column(name = "management_fee", precision = 5, scale = 2)
    private BigDecimal managementFee;

    protected FundItemEntity() {
    }

    public FundItemEntity(Long id,
                          Long userId,
                          String itemName,
                          BigDecimal investedAmount,
                          boolean newsEnabled,
                          String region,
                          String memo,
                          LocalDateTime createdAt,
                          LocalDateTime updatedAt,
                          String subType,
                          BigDecimal managementFee) {
        super(id, userId, itemName, investedAmount, newsEnabled, region, memo, createdAt, updatedAt);
        this.subType = subType;
        this.managementFee = managementFee;
    }
}