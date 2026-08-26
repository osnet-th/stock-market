package com.thlee.stock.market.stockmarket.salary.infrastructure.persistence.mapper;

import com.thlee.stock.market.stockmarket.salary.domain.model.SalarySetting;
import com.thlee.stock.market.stockmarket.salary.infrastructure.persistence.SalarySettingEntity;

/**
 * SalarySetting Entity ↔ Domain 변환.
 */
public class SalarySettingMapper {

    private SalarySettingMapper() {
    }

    public static SalarySettingEntity toEntity(SalarySetting domain) {
        return new SalarySettingEntity(
                domain.getId(),
                domain.getUserId(),
                domain.getSavingTargetPct(),
                domain.getCreatedAt(),
                domain.getUpdatedAt()
        );
    }

    public static SalarySetting toDomain(SalarySettingEntity entity) {
        return new SalarySetting(
                entity.getId(),
                entity.getUserId(),
                entity.getSavingTargetPct(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
