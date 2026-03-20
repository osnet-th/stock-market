package com.thlee.stock.market.stockmarket.news.infrastructure.persistence.mapper;

import com.thlee.stock.market.stockmarket.news.domain.model.UserKeyword;
import com.thlee.stock.market.stockmarket.news.infrastructure.persistence.UserKeywordEntity;

/**
 * UserKeyword Entity ↔ Domain Model 변환 Mapper
 */
public class UserKeywordMapper {

    public static UserKeywordEntity toEntity(UserKeyword userKeyword) {
        return new UserKeywordEntity(
                userKeyword.getId(),
                userKeyword.getUserId(),
                userKeyword.getKeywordId(),
                userKeyword.isActive(),
                userKeyword.getCreatedAt(),
                userKeyword.getUpdatedAt()
        );
    }

    public static UserKeyword toDomain(UserKeywordEntity entity) {
        return new UserKeyword(
                entity.getId(),
                entity.getUserId(),
                entity.getKeywordId(),
                entity.isActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
