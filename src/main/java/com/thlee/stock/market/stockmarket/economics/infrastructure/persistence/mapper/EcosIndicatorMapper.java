package com.thlee.stock.market.stockmarket.economics.infrastructure.persistence.mapper;

import com.thlee.stock.market.stockmarket.economics.domain.model.EcosIndicator;
import com.thlee.stock.market.stockmarket.economics.infrastructure.persistence.EcosIndicatorEntity;
import org.springframework.stereotype.Component;

@Component
public class EcosIndicatorMapper {

    public EcosIndicatorEntity toEntity(EcosIndicator domain) {
        return new EcosIndicatorEntity(
            domain.getId(),
            domain.getClassName(),
            domain.getKeystatName(),
            domain.getDataValue(),
            domain.getCycle(),
            domain.getUnitName(),
            domain.getSnapshotDate(),
            domain.getCreatedAt()
        );
    }

    public EcosIndicator toDomain(EcosIndicatorEntity entity) {
        return new EcosIndicator(
            entity.getId(),
            entity.getClassName(),
            entity.getKeystatName(),
            entity.getDataValue(),
            entity.getCycle(),
            entity.getUnitName(),
            entity.getSnapshotDate(),
            entity.getCreatedAt()
        );
    }
}
