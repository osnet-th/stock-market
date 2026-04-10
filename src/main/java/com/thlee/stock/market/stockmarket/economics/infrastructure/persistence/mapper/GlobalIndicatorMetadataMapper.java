package com.thlee.stock.market.stockmarket.economics.infrastructure.persistence.mapper;

import com.thlee.stock.market.stockmarket.economics.domain.model.GlobalIndicatorMetadata;
import com.thlee.stock.market.stockmarket.economics.infrastructure.persistence.GlobalIndicatorMetadataEntity;
import org.springframework.stereotype.Component;

@Component
public class GlobalIndicatorMetadataMapper {

    public GlobalIndicatorMetadataEntity toEntity(GlobalIndicatorMetadata domain) {
        return new GlobalIndicatorMetadataEntity(
            domain.getIndicatorType(),
            domain.getDescription()
        );
    }

    public GlobalIndicatorMetadata toDomain(GlobalIndicatorMetadataEntity entity) {
        return new GlobalIndicatorMetadata(
            entity.getIndicatorType(),
            entity.getDescription()
        );
    }
}