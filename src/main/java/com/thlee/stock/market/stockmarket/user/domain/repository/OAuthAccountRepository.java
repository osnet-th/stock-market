package com.thlee.stock.market.stockmarket.user.domain.repository;

import com.thlee.stock.market.stockmarket.user.domain.model.OAuthAccount;
import com.thlee.stock.market.stockmarket.user.domain.model.OAuthProvider;

import java.util.List;
import java.util.Optional;

/**
 * OAuthAccount Repository 인터페이스
 */
public interface OAuthAccountRepository {
    /**
     * OAuth 계정 저장
     */
    OAuthAccount save(OAuthAccount oauthAccount);

    /**
     * provider + issuer + subject로 OAuth 계정 조회
     */
    Optional<OAuthAccount> findByProviderAndIssuerAndSubject(
            OAuthProvider provider,
            String issuer,
            String subject
    );

    /**
     * ID로 OAuth 계정 조회
     */
    Optional<OAuthAccount> findById(Long id);

    /**
     * 사용자 ID로 OAuth 계정 목록 조회
     */
    List<OAuthAccount> findByUserId(Long userId);

    /**
     * 여러 ID로 OAuth 계정 목록 조회
     */
    List<OAuthAccount> findByIdIn(List<Long> ids);
}