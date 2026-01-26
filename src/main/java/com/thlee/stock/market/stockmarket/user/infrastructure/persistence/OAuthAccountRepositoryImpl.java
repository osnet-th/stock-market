package com.thlee.stock.market.stockmarket.user.infrastructure.persistence;

import com.thlee.stock.market.stockmarket.user.domain.model.OAuthAccount;
import com.thlee.stock.market.stockmarket.user.domain.model.OAuthProvider;
import com.thlee.stock.market.stockmarket.user.domain.repository.OAuthAccountRepository;
import com.thlee.stock.market.stockmarket.user.infrastructure.persistence.mapper.OAuthAccountMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * OAuthAccountRepository 구현체 (Adapter)
 */
@Repository
@RequiredArgsConstructor
public class OAuthAccountRepositoryImpl implements OAuthAccountRepository {

    private final OAuthAccountJpaRepository oauthAccountJpaRepository;

    @Override
    public OAuthAccount save(OAuthAccount oauthAccount) {
        OAuthAccountEntity entity = OAuthAccountMapper.toEntity(oauthAccount);
        OAuthAccountEntity savedEntity = oauthAccountJpaRepository.save(entity);
        return OAuthAccountMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<OAuthAccount> findByProviderAndIssuerAndSubject(
            OAuthProvider provider,
            String issuer,
            String subject
    ) {
        return oauthAccountJpaRepository.findByProviderAndIssuerAndSubject(
                        provider.name(),
                        issuer,
                        subject
                )
                .map(OAuthAccountMapper::toDomain);
    }

    @Override
    public Optional<OAuthAccount> findById(Long id) {
        return oauthAccountJpaRepository.findById(id)
                .map(OAuthAccountMapper::toDomain);
    }

    @Override
    public List<OAuthAccount> findByUserId(Long userId) {
        return oauthAccountJpaRepository.findByUserId(userId)
                .stream()
                .map(OAuthAccountMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<OAuthAccount> findByIdIn(List<Long> ids) {
        return oauthAccountJpaRepository.findByIdIn(ids)
                .stream()
                .map(OAuthAccountMapper::toDomain)
                .collect(Collectors.toList());
    }
}