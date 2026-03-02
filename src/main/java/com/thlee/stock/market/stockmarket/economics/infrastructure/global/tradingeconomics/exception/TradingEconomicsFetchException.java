package com.thlee.stock.market.stockmarket.economics.infrastructure.global.tradingeconomics.exception;

public class TradingEconomicsFetchException extends RuntimeException {
    public TradingEconomicsFetchException(String message, Throwable cause) {
        super(message, cause);
    }
}