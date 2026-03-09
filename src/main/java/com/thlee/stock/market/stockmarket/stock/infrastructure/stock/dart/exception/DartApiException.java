package com.thlee.stock.market.stockmarket.stock.infrastructure.stock.dart.exception;

public class DartApiException extends RuntimeException {

    public DartApiException(String message) {
        super(message);
    }

    public DartApiException(String message, Throwable cause) {
        super(message, cause);
    }
}