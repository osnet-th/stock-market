package com.thlee.stock.market.stockmarket.portfolio.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "bond_detail")
@DiscriminatorValue("BOND")
@Getter
public class BondItemEntity extends PortfolioItemEntity {

    @Column(name = "sub_type", length = 20)
    private String subType;

    @Column(name = "maturity_date")
    private LocalDate maturityDate;

    @Column(name = "coupon_rate", precision = 5, scale = 2)
    private BigDecimal couponRate;

    @Column(name = "credit_rating", length = 10)
    private String creditRating;

    protected BondItemEntity() {
    }

    public BondItemEntity(Long id,
                          Long userId,
                          String itemName,
                          BigDecimal investedAmount,
                          boolean newsEnabled,
                          String region,
                          String memo,
                          LocalDateTime createdAt,
                          LocalDateTime updatedAt,
                          String subType,
                          LocalDate maturityDate,
                          BigDecimal couponRate,
                          String creditRating) {
        super(id, userId, itemName, investedAmount, newsEnabled, region, memo, createdAt, updatedAt);
        this.subType = subType;
        this.maturityDate = maturityDate;
        this.couponRate = couponRate;
        this.creditRating = creditRating;
    }
}