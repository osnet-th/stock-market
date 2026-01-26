package com.thlee.stock.market.stockmarket.user.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * OAuthAccount JPA Repository
 */
public interface OAuthAccountJpaRepository extends JpaRepository<OAuthAccountEntity, Long> {
    /**
     * provider + issuer + subject로 OAuth 계정 조회
     */
    Optional<OAuthAccountEntity> findByProviderAndIssuerAndSubject(String provider, String issuer, String subject);

    /**
     * 사용자 ID로 OAuth 계정 목록 조회
     */
    List<OAuthAccountEntity> findByUserId(Long userId);

    /**
     * 여러 ID로 OAuth 계정 목록 조회
     */
    List<OAuthAccountEntity> findByIdIn(List<Long> ids);
}