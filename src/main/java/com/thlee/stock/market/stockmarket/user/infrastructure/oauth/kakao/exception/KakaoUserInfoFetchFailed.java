package com.thlee.stock.market.stockmarket.user.infrastructure.oauth.kakao.exception;

public class KakaoUserInfoFetchFailed extends KakaoOAuthException {
    public KakaoUserInfoFetchFailed(String message) {
        super(message);
    }

    public KakaoUserInfoFetchFailed(String message, Throwable cause) {
        super(message, cause);
    }
}