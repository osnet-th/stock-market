package com.thlee.stock.market.stockmarket.infrastructure.security.jwt.exception;

/**
 * 만료된 토큰 예외
 * - 토큰 만료 시간이 지남
 */
public class ExpiredTokenException extends JwtException {

    public ExpiredTokenException(String message) {
        super(message);
    }

    public ExpiredTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}