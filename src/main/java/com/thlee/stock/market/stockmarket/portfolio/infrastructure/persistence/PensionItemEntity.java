package com.thlee.stock.market.stockmarket.portfolio.infrastructure.persistence;

import com.thlee.stock.market.stockmarket.portfolio.domain.model.enums.PortfolioItemStatus;
import jakarta.persistence.*;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pension_detail")
@DiscriminatorValue("PENSION")
@Getter
public class PensionItemEntity extends PortfolioItemEntity {

    @Column(name = "sub_type", length = 20)
    private String subType;

    @Column(name = "provider", length = 100)
    private String provider;

    @Column(name = "evaluated_amount", precision = 18, scale = 2)
    private BigDecimal evaluatedAmount;

    @Column(name = "monthly_deposit_amount", precision = 18, scale = 2)
    private BigDecimal monthlyDepositAmount;

    @Column(name = "deposit_day")
    private Integer depositDay;

    protected PensionItemEntity() {
    }

    public PensionItemEntity(Long id,
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
                             String provider,
                             BigDecimal evaluatedAmount,
                             BigDecimal monthlyDepositAmount,
                             Integer depositDay) {
        super(id, userId, itemName, investedAmount, newsEnabled, region, memo, status, version, createdAt, updatedAt);
        this.subType = subType;
        this.provider = provider;
        this.evaluatedAmount = evaluatedAmount;
        this.monthlyDepositAmount = monthlyDepositAmount;
        this.depositDay = depositDay;
    }
}
