package com.thlee.stock.market.stockmarket.portfolio.application.dto;

import com.thlee.stock.market.stockmarket.portfolio.domain.model.StockDetail;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class StockDetailResponse {
    private final String subType;
    private final String stockCode;
    private final String market;
    private final String exchangeCode;
    private final String country;
    private final Integer quantity;
    private final BigDecimal avgBuyPrice;
    private final BigDecimal dividendYield;

    private StockDetailResponse(String subType, String stockCode, String market, String exchangeCode,
                                String country, Integer quantity, BigDecimal avgBuyPrice,
                                BigDecimal dividendYield) {
        this.subType = subType;
        this.stockCode = stockCode;
        this.market = market;
        this.exchangeCode = exchangeCode;
        this.country = country;
        this.quantity = quantity;
        this.avgBuyPrice = avgBuyPrice;
        this.dividendYield = dividendYield;
    }

    public static StockDetailResponse from(StockDetail detail) {
        return new StockDetailResponse(
                detail.getSubType() != null ? detail.getSubType().name() : null,
                detail.getStockCode(), detail.getMarket(), detail.getExchangeCode(),
                detail.getCountry(), detail.getQuantity(),
                detail.getAvgBuyPrice(), detail.getDividendYield()
        );
    }
}