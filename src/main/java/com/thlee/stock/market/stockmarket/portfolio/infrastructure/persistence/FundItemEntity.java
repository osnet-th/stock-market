package com.thlee.stock.market.stockmarket.portfolio.infrastructure.persistence;

import com.thlee.stock.market.stockmarket.portfolio.domain.model.enums.PortfolioItemStatus;
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

    @Column(name = "monthly_deposit_amount", precision = 18, scale = 2)
    private BigDecimal monthlyDepositAmount;

    @Column(name = "deposit_day")
    private Integer depositDay;

    protected FundItemEntity() {
    }

    public FundItemEntity(Long id,
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
                          String subType,
                          BigDecimal managementFee,
                          BigDecimal monthlyDepositAmount,
                          Integer depositDay) {
        super(id, userId, itemName, investedAmount, newsEnabled, region, memo, status, version, createdAt, updatedAt);
        this.subType = subType;
        this.managementFee = managementFee;
        this.monthlyDepositAmount = monthlyDepositAmount;
        this.depositDay = depositDay;
    }
}