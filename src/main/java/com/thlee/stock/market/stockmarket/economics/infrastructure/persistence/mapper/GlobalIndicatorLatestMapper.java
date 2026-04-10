package com.thlee.stock.market.stockmarket.economics.infrastructure.persistence.mapper;

import com.thlee.stock.market.stockmarket.economics.domain.model.GlobalIndicatorLatest;
import com.thlee.stock.market.stockmarket.economics.infrastructure.persistence.GlobalIndicatorLatestEntity;
import org.springframework.stereotype.Component;

@Component
public class GlobalIndicatorLatestMapper {

    public GlobalIndicatorLatestEntity toEntity(GlobalIndicatorLatest domain) {
        return new GlobalIndicatorLatestEntity(
            domain.getCountryName(),
            domain.getIndicatorType(),
            domain.getDataValue(),
            domain.getPreviousDataValue(),
            domain.getCycle(),
            domain.getUnit(),
            domain.getUpdatedAt()
        );
    }

    public GlobalIndicatorLatest toDomain(GlobalIndicatorLatestEntity entity) {
        return new GlobalIndicatorLatest(
            entity.getCountryName(),
            entity.getIndicatorType(),
            entity.getDataValue(),
            entity.getPreviousDataValue(),
            entity.getCycle(),
            entity.getUnit(),
            entity.getUpdatedAt()
        );
    }
}