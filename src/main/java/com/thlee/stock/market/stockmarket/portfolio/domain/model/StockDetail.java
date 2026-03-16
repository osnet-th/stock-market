package com.thlee.stock.market.stockmarket.portfolio.domain.model;

import com.thlee.stock.market.stockmarket.portfolio.domain.model.enums.PriceCurrency;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.enums.StockSubType;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class StockDetail {
    private final StockSubType subType;
    private final String stockCode;
    private final String market;
    private final String exchangeCode;
    private final String country;
    private final Integer quantity;
    private final BigDecimal avgBuyPrice;
    private final BigDecimal dividendYield;
    private final PriceCurrency priceCurrency;
    private final BigDecimal investedAmountKrw;

    public StockDetail(StockSubType subType,
                       String stockCode,
                       String market,
                       String exchangeCode,
                       String country,
                       Integer quantity,
                       BigDecimal avgBuyPrice,
                       BigDecimal dividendYield,
                       PriceCurrency priceCurrency,
                       BigDecimal investedAmountKrw) {
        this.subType = subType;
        this.stockCode = stockCode;
        this.market = market;
        this.exchangeCode = exchangeCode;
        this.country = country;
        this.quantity = quantity;
        this.avgBuyPrice = avgBuyPrice;
        this.dividendYield = dividendYield;
        this.priceCurrency = priceCurrency;
        this.investedAmountKrw = investedAmountKrw;
    }
}
