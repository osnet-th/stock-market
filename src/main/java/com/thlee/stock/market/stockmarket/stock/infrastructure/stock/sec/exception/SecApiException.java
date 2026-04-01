package com.thlee.stock.market.stockmarket.stock.infrastructure.stock.sec.exception;

import lombok.Getter;

@Getter
public class SecApiException extends RuntimeException {

    private final SecErrorType errorType;

    public SecApiException(String message) {
        super(message);
        this.errorType = SecErrorType.UNKNOWN;
    }

    public SecApiException(String message, Throwable cause) {
        super(message, cause);
        this.errorType = SecErrorType.UNKNOWN;
    }

    public SecApiException(SecErrorType errorType, String message) {
        super(message);
        this.errorType = errorType;
    }

    public SecApiException(SecErrorType errorType, String message, Throwable cause) {
        super(message, cause);
        this.errorType = errorType;
    }
}