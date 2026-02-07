package com.thlee.stock.market.stockmarket.news.infrastructure.persistence.mapper;

import com.thlee.stock.market.stockmarket.news.domain.model.News;
import com.thlee.stock.market.stockmarket.news.infrastructure.persistence.NewsEntity;

/**
 * News Entity ↔ Domain Model 변환 Mapper
 */
public class NewsMapper {

    public static NewsEntity toEntity(News news) {
        return new NewsEntity(
                news.getId(),
                news.getOriginalUrl(),
                news.getUserId(),
                news.getTitle(),
                news.getContent(),
                news.getPublishedAt(),
                news.getCreatedAt(),
                news.getPurpose(),
                news.getSearchKeyword()
        );
    }

    public static News toDomain(NewsEntity entity) {
        return new News(
                entity.getId(),
                entity.getOriginalUrl(),
                entity.getUserId(),
                entity.getTitle(),
                entity.getContent(),
                entity.getPublishedAt(),
                entity.getCreatedAt(),
                entity.getPurpose(),
                entity.getSearchKeyword()
        );
    }
}
