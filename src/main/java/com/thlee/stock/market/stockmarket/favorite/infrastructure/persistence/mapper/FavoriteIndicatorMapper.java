package com.thlee.stock.market.stockmarket.favorite.infrastructure.persistence.mapper;

import com.thlee.stock.market.stockmarket.favorite.domain.model.FavoriteIndicator;
import com.thlee.stock.market.stockmarket.favorite.infrastructure.persistence.UserFavoriteIndicatorEntity;
import org.springframework.stereotype.Component;

@Component
public class FavoriteIndicatorMapper {

    public UserFavoriteIndicatorEntity toEntity(FavoriteIndicator domain) {
        return new UserFavoriteIndicatorEntity(
            domain.getId(),
            domain.getUserId(),
            domain.getSourceType(),
            domain.getIndicatorCode(),
            domain.getDisplayMode(),
            domain.getCreatedAt()
        );
    }

    public FavoriteIndicator toDomain(UserFavoriteIndicatorEntity entity) {
        return new FavoriteIndicator(
            entity.getId(),
            entity.getUserId(),
            entity.getSourceType(),
            entity.getIndicatorCode(),
            entity.getDisplayMode(),
            entity.getCreatedAt()
        );
    }
}
