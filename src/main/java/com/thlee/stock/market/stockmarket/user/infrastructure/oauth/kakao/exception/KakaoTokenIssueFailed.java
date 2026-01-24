package com.thlee.stock.market.stockmarket.user.infrastructure.oauth.kakao.exception;

public class KakaoTokenIssueFailed extends KakaoOAuthException {
    public KakaoTokenIssueFailed(String message) {
        super(message);
    }

    public KakaoTokenIssueFailed(String message, Throwable cause) {
        super(message, cause);
    }
}