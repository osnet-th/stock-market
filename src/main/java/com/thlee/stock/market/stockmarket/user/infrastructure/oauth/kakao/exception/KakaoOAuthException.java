package com.thlee.stock.market.stockmarket.user.infrastructure.oauth.kakao.exception;

public abstract class KakaoOAuthException extends RuntimeException {
    protected KakaoOAuthException(String message) {
        super(message);
    }

    protected KakaoOAuthException(String message, Throwable cause) {
        super(message, cause);
    }
}