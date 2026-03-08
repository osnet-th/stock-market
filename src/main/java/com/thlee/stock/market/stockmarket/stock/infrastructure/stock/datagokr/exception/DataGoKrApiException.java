package com.thlee.stock.market.stockmarket.stock.infrastructure.stock.datagokr.exception;

public class DataGoKrApiException extends RuntimeException {
    public DataGoKrApiException(String message, Throwable cause) {
        super(message, cause);
    }

    public DataGoKrApiException(String message) {
        super(message);
    }
}