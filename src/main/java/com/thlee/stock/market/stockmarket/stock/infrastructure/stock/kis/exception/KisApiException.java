package com.thlee.stock.market.stockmarket.stock.infrastructure.stock.kis.exception;

public class KisApiException extends RuntimeException {

    public KisApiException(String message) {
        super(message);
    }

    public KisApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
