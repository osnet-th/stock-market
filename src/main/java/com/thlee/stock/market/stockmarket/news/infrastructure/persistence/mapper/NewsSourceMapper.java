package com.thlee.stock.market.stockmarket.news.infrastructure.persistence.mapper;

import com.thlee.stock.market.stockmarket.news.domain.model.NewsSource;
import com.thlee.stock.market.stockmarket.news.infrastructure.persistence.NewsSourceEntity;

/**
 * NewsSource Entity ↔ Domain Model 변환 Mapper
 */
public class NewsSourceMapper {

    public static NewsSourceEntity toEntity(NewsSource newsSource) {
        return new NewsSourceEntity(
                newsSource.getId(),
                newsSource.getNewsId(),
                newsSource.getUserId(),
                newsSource.getPurpose(),
                newsSource.getSourceId(),
                newsSource.getCreatedAt()
        );
    }

    public static NewsSource toDomain(NewsSourceEntity entity) {
        return new NewsSource(
                entity.getId(),
                entity.getNewsId(),
                entity.getUserId(),
                entity.getPurpose(),
                entity.getSourceId(),
                entity.getCreatedAt()
        );
    }
}