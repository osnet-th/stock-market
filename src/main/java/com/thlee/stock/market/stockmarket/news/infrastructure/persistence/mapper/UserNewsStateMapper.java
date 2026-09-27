package com.thlee.stock.market.stockmarket.news.infrastructure.persistence.mapper;

import com.thlee.stock.market.stockmarket.news.domain.model.UserNewsState;
import com.thlee.stock.market.stockmarket.news.infrastructure.persistence.UserNewsStateEntity;

/**
 * UserNewsState Entity ↔ Domain Model 변환 Mapper (#115)
 */
public class UserNewsStateMapper {

    public static UserNewsStateEntity toEntity(UserNewsState state) {
        return new UserNewsStateEntity(
                state.getId(),
                state.getUserId(),
                state.getNewsId(),
                state.isRead(),
                state.isSaved(),
                state.getUpdatedAt()
        );
    }

    public static UserNewsState toDomain(UserNewsStateEntity entity) {
        return new UserNewsState(
                entity.getId(),
                entity.getUserId(),
                entity.getNewsId(),
                entity.isRead(),
                entity.isSaved(),
                entity.getUpdatedAt()
        );
    }
}
