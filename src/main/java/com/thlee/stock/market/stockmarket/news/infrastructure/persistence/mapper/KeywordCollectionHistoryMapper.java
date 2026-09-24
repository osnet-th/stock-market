package com.thlee.stock.market.stockmarket.news.infrastructure.persistence.mapper;

import com.thlee.stock.market.stockmarket.news.domain.model.KeywordCollectionHistory;
import com.thlee.stock.market.stockmarket.news.infrastructure.persistence.KeywordCollectionHistoryEntity;

/**
 * KeywordCollectionHistory Entity ↔ Domain Model 변환 Mapper (#115)
 */
public class KeywordCollectionHistoryMapper {

    public static KeywordCollectionHistoryEntity toEntity(KeywordCollectionHistory history) {
        return new KeywordCollectionHistoryEntity(
                history.getId(),
                history.getKeywordId(),
                history.getAttemptedAt(),
                history.getStatus(),
                history.getSavedCount(),
                history.getIgnoredCount(),
                history.getErrorMessage()
        );
    }

    public static KeywordCollectionHistory toDomain(KeywordCollectionHistoryEntity entity) {
        return new KeywordCollectionHistory(
                entity.getId(),
                entity.getKeywordId(),
                entity.getAttemptedAt(),
                entity.getStatus(),
                entity.getSavedCount(),
                entity.getIgnoredCount(),
                entity.getErrorMessage()
        );
    }
}
