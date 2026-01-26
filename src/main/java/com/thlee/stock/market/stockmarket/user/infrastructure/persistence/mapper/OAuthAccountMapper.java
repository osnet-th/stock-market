package com.thlee.stock.market.stockmarket.user.infrastructure.persistence.mapper;

import com.thlee.stock.market.stockmarket.user.domain.model.OAuthAccount;
import com.thlee.stock.market.stockmarket.user.domain.model.OAuthProvider;
import com.thlee.stock.market.stockmarket.user.infrastructure.persistence.OAuthAccountEntity;

/**
 * OAuthAccount Entity ↔ Domain Model 변환 Mapper
 */
public class OAuthAccountMapper {

    /**
     * Domain → Entity 변환
     */
    public static OAuthAccountEntity toEntity(OAuthAccount domain) {
        return new OAuthAccountEntity(
                domain.getId(),
                domain.getUserId(),
                domain.getProvider().name(),
                domain.getIssuer(),
                domain.getSubject(),
                domain.getEmail(),
                domain.getConnectedAt()
        );
    }

    /**
     * Entity → Domain 변환
     */
    public static OAuthAccount toDomain(OAuthAccountEntity entity) {
        return new OAuthAccount(
                entity.getId(),
                entity.getUserId(),
                OAuthProvider.valueOf(entity.getProvider()),
                entity.getIssuer(),
                entity.getSubject(),
                entity.getEmail(),
                entity.getConnectedAt()
        );
    }
}