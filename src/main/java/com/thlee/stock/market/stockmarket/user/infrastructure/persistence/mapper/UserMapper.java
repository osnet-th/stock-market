package com.thlee.stock.market.stockmarket.user.infrastructure.persistence.mapper;

import com.thlee.stock.market.stockmarket.user.domain.model.Nickname;
import com.thlee.stock.market.stockmarket.user.domain.model.PhoneNumber;
import com.thlee.stock.market.stockmarket.user.domain.model.User;
import com.thlee.stock.market.stockmarket.user.domain.model.UserRole;
import com.thlee.stock.market.stockmarket.user.domain.model.UserStatus;
import com.thlee.stock.market.stockmarket.user.infrastructure.persistence.UserEntity;

import java.util.List;

/**
 * User Entity ↔ Domain Model 변환 Mapper
 */
public class UserMapper {

    /**
     * Domain → Entity 변환
     */
    public static UserEntity toEntity(User user) {
        return new UserEntity(
                user.getId(),
                user.getName(),
                user.getNickname() != null ? user.getNickname().getValue() : null,
                user.getPhoneNumber() != null ? user.getPhoneNumber().getValue() : null,
                user.getStatus().name(),
                user.getRole().name(),
                user.getCreatedAt(),
                user.getDeletedAt()
        );
    }

    /**
     * Entity → Domain 변환 (OAuthAccount IDs 포함)
     */
    public static User toDomain(UserEntity entity, List<Long> oauthAccountIds) {
        return new User(
                entity.getId(),
                entity.getName(),
                entity.getNickname() != null ? new Nickname(entity.getNickname()) : null,
                entity.getPhoneNumber() != null ? new PhoneNumber(entity.getPhoneNumber()) : null,
                oauthAccountIds,
                UserStatus.valueOf(entity.getStatus()),
                UserRole.valueOf(entity.getRole()),
                entity.getCreatedAt(),
                entity.getDeletedAt()
        );
    }
}