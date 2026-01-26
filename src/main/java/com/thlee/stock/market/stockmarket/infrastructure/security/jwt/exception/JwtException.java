package com.thlee.stock.market.stockmarket.infrastructure.security.jwt.exception;

/**
 * JWT 관련 기본 예외
 */
public abstract class JwtException extends RuntimeException {

    public JwtException(String message) {
        super(message);
    }

    public JwtException(String message, Throwable cause) {
        super(message, cause);
    }
}
