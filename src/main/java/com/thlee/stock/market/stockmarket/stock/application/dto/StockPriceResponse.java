package com.thlee.stock.market.stockmarket.stock.application.dto;

import com.thlee.stock.market.stockmarket.stock.domain.model.StockPrice;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class StockPriceResponse {

    private final String stockCode;
    private final String currentPrice;
    private final String previousClose;
    private final String change;
    private final String changeSign;
    private final String changeRate;
    private final String volume;
    private final String tradingAmount;
    private final String high;
    private final String low;
    private final String open;
    private final String marketType;
    private final String exchangeCode;

    public static StockPriceResponse from(StockPrice price) {
        return new StockPriceResponse(
            price.stockCode(),
            price.currentPrice(),
            price.previousClose(),
            price.change(),
            price.changeSign(),
            price.changeRate(),
            price.volume(),
            price.tradingAmount(),
            price.high(),
            price.low(),
            price.open(),
            price.marketType().name(),
            price.exchangeCode().name()
        );
    }
}