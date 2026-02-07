package com.thlee.stock.market.stockmarket.news.infrastructure.persistence.mapper;

import com.thlee.stock.market.stockmarket.news.domain.model.Keyword;
import com.thlee.stock.market.stockmarket.news.infrastructure.persistence.KeywordEntity;

/**
 * Keyword Entity ↔ Domain Model 변환 Mapper
 */
public class KeywordMapper {

    /**
     * Domain → Entity 변환
     */
    public static KeywordEntity toEntity(Keyword keyword) {
        return new KeywordEntity(
                keyword.getId(),
                keyword.getKeyword(),
                keyword.getUserId(),
                keyword.isActive(),
                keyword.getCreatedAt()
        );
    }

    /**
     * Entity → Domain 변환
     */
    public static Keyword toDomain(KeywordEntity entity) {
        return new Keyword(
                entity.getId(),
                entity.getKeyword(),
                entity.getUserId(),
                entity.isActive(),
                entity.getCreatedAt()
        );
    }
}