package com.thlee.stock.market.stockmarket.salary.infrastructure.persistence.mapper;

import com.thlee.stock.market.stockmarket.salary.domain.model.SpendingConfig;
import com.thlee.stock.market.stockmarket.salary.infrastructure.persistence.SpendingConfigEntity;

/**
 * SpendingConfig Entity ↔ Domain 변환.
 */
public class SpendingConfigMapper {

    private SpendingConfigMapper() {
    }

    public static SpendingConfigEntity toEntity(SpendingConfig domain) {
        return new SpendingConfigEntity(
                domain.getId(),
                domain.getUserId(),
                domain.getCategory(),
                YearMonthConverter.toLocalDate(domain.getEffectiveFromMonth()),
                domain.getAmount(),
                domain.getMemo(),
                domain.getCreatedAt(),
                domain.getUpdatedAt()
        );
    }

    public static SpendingConfig toDomain(SpendingConfigEntity entity) {
        return new SpendingConfig(
                entity.getId(),
                entity.getUserId(),
                entity.getCategory(),
                YearMonthConverter.toYearMonth(entity.getEffectiveFromMonth()),
                entity.getAmount(),
                entity.getMemo(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}