package com.thlee.stock.market.stockmarket.economics.infrastructure.persistence.mapper;

import com.thlee.stock.market.stockmarket.economics.domain.model.EcosIndicatorMetadata;
import com.thlee.stock.market.stockmarket.economics.infrastructure.persistence.EcosIndicatorMetadataEntity;
import org.springframework.stereotype.Component;

@Component
public class EcosIndicatorMetadataMapper {

    public EcosIndicatorMetadata toDomain(EcosIndicatorMetadataEntity entity) {
        return new EcosIndicatorMetadata(
            entity.getClassName(),
            entity.getKeystatName(),
            entity.getDescription(),
            entity.getPositiveDirection(),
            entity.isKeyIndicator()
        );
    }

    public EcosIndicatorMetadataEntity toEntity(EcosIndicatorMetadata domain) {
        return new EcosIndicatorMetadataEntity(
            domain.getClassName(),
            domain.getKeystatName(),
            domain.getDescription(),
            domain.getPositiveDirection(),
            domain.isKeyIndicator()
        );
    }
}