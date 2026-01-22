package com.thlee.stock.market.stockmarket.user.domain.exception;

import com.thlee.stock.market.stockmarket.user.domain.model.OAuthProvider;

/**
 * OAuth Provider 중복 연결 예외 (도메인 규칙: 같은 provider 중복 연결 불가)
 */
public class DuplicateOAuthProviderException extends UserDomainException {

    public DuplicateOAuthProviderException(OAuthProvider provider) {
        super("이미 연결된 OAuth Provider입니다: " + provider);
    }
}