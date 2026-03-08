package com.thlee.stock.market.stockmarket.stock.application.dto;

import com.thlee.stock.market.stockmarket.stock.domain.model.Stock;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class StockResponse {
    private final String stockCode;
    private final String stockName;
    private final String marketType;
    private final String corpName;

    public static StockResponse from(Stock stock) {
        return new StockResponse(
            stock.stockCode(),
            stock.stockName(),
            stock.marketType(),
            stock.corpName()
        );
    }
}