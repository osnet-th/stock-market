package com.thlee.stock.market.stockmarket.user.domain.exception;

/**
 * User 도메인의 최상위 예외 클래스
 */
public class UserDomainException extends RuntimeException {

    public UserDomainException(String message) {
        super(message);
    }

    public UserDomainException(String message, Throwable cause) {
        super(message, cause);
    }
}