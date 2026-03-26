package com.thlee.stock.market.stockmarket.portfolio.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "cash_detail")
@DiscriminatorValue("CASH")
@Getter
public class CashItemEntity extends PortfolioItemEntity {

    @Column(name = "cash_type", length = 20)
    private String cashType;

    @Column(name = "interest_rate", precision = 7, scale = 4)
    private BigDecimal interestRate;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "maturity_date")
    private LocalDate maturityDate;

    @Column(name = "tax_type", length = 20)
    private String taxType;

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
                          LocalDateTime updatedAt,
                          String cashType,
                          BigDecimal interestRate,
                          LocalDate startDate,
                          LocalDate maturityDate,
                          String taxType) {
        super(id, userId, itemName, investedAmount, newsEnabled, region, memo, createdAt, updatedAt);
        this.cashType = cashType;
        this.interestRate = interestRate;
        this.startDate = startDate;
        this.maturityDate = maturityDate;
        this.taxType = taxType;
    }
}