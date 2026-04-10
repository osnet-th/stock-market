package com.thlee.stock.market.stockmarket.economics.infrastructure.persistence.mapper;

import com.thlee.stock.market.stockmarket.economics.domain.model.GlobalIndicator;
import com.thlee.stock.market.stockmarket.economics.infrastructure.persistence.GlobalIndicatorEntity;
import org.springframework.stereotype.Component;

@Component
public class GlobalIndicatorMapper {

    public GlobalIndicatorEntity toEntity(GlobalIndicator domain) {
        return new GlobalIndicatorEntity(
            domain.getId(),
            domain.getCountryName(),
            domain.getIndicatorType(),
            domain.getDataValue(),
            domain.getCycle(),
            domain.getUnit(),
            domain.getSnapshotDate(),
            domain.getCreatedAt()
        );
    }

    public GlobalIndicator toDomain(GlobalIndicatorEntity entity) {
        return new GlobalIndicator(
            entity.getId(),
            entity.getCountryName(),
            entity.getIndicatorType(),
            entity.getDataValue(),
            entity.getCycle(),
            entity.getUnit(),
            entity.getSnapshotDate(),
            entity.getCreatedAt()
        );
    }
}