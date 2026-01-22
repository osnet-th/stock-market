package com.thlee.stock.market.stockmarket.user.domain.exception;

/**
 * User 도메인 파라미터 검증 실패 예외
 */
public class InvalidUserArgumentException extends UserDomainException {

    public InvalidUserArgumentException(String message) {
        super(message);
    }
}