package com.thlee.stock.market.stockmarket.portfolio.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "stock_detail")
@DiscriminatorValue("STOCK")
@Getter
public class StockItemEntity extends PortfolioItemEntity {

    @Column(name = "sub_type", length = 20)
    private String subType;

    @Column(name = "stock_code", length = 20)
    private String stockCode;

    @Column(name = "market", length = 20)
    private String market;

    @Column(name = "country", length = 10)
    private String country;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "avg_buy_price", precision = 18, scale = 2)
    private BigDecimal avgBuyPrice;

    @Column(name = "dividend_yield", precision = 5, scale = 2)
    private BigDecimal dividendYield;

    protected StockItemEntity() {
    }

    public StockItemEntity(Long id,
                           Long userId,
                           String itemName,
                           BigDecimal investedAmount,
                           boolean newsEnabled,
                           String region,
                           String memo,
                           LocalDateTime createdAt,
                           LocalDateTime updatedAt,
                           String subType,
                           String stockCode,
                           String market,
                           String country,
                           Integer quantity,
                           BigDecimal avgBuyPrice,
                           BigDecimal dividendYield) {
        super(id, userId, itemName, investedAmount, newsEnabled, region, memo, createdAt, updatedAt);
        this.subType = subType;
        this.stockCode = stockCode;
        this.market = market;
        this.country = country;
        this.quantity = quantity;
        this.avgBuyPrice = avgBuyPrice;
        this.dividendYield = dividendYield;
    }
}