package com.thlee.stock.market.stockmarket.infrastructure.security.jwt.exception;

/**
 * 유효하지 않은 토큰 예외
 * - 토큰 서명이 유효하지 않음
 * - 토큰 형식이 잘못됨
 * - 토큰이 null이거나 빈 문자열
 * - Claims 파싱 실패
 */
public class InvalidTokenException extends JwtException {

    public InvalidTokenException(String message) {
        super(message);
    }

    public InvalidTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}