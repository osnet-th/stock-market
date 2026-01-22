package com.thlee.stock.market.stockmarket.user.domain.exception;

/**
 * User 도메인 상태 검증 실패 예외
 */
public class InvalidUserStateException extends UserDomainException {

    public InvalidUserStateException(String message) {
        super(message);
    }
}