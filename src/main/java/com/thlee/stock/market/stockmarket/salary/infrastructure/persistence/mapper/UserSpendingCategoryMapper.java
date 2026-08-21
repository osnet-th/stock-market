package com.thlee.stock.market.stockmarket.salary.infrastructure.persistence.mapper;

import com.thlee.stock.market.stockmarket.salary.domain.model.UserSpendingCategory;
import com.thlee.stock.market.stockmarket.salary.infrastructure.persistence.UserSpendingCategoryEntity;

/**
 * UserSpendingCategory Entity ↔ Domain 변환.
 */
public class UserSpendingCategoryMapper {

    private UserSpendingCategoryMapper() {
    }

    public static UserSpendingCategoryEntity toEntity(UserSpendingCategory domain) {
        return new UserSpendingCategoryEntity(
                domain.getId(),
                domain.getUserId(),
                domain.getCode(),
                domain.getName(),
                domain.getColor(),
                domain.isSavings(),
                domain.isSystem(),
                domain.isActive(),
                domain.getSortOrder(),
                domain.getCreatedAt(),
                domain.getUpdatedAt()
        );
    }

    public static UserSpendingCategory toDomain(UserSpendingCategoryEntity entity) {
        return new UserSpendingCategory(
                entity.getId(),
                entity.getUserId(),
                entity.getCode(),
                entity.getName(),
                entity.getColor(),
                entity.isSavings(),
                entity.isSystem(),
                entity.isActive(),
                entity.getSortOrder(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
