package com.thlee.stock.market.stockmarket.economics.infrastructure.persistence.mapper;

import com.thlee.stock.market.stockmarket.economics.domain.model.EcosIndicatorLatest;
import com.thlee.stock.market.stockmarket.economics.infrastructure.persistence.EcosIndicatorLatestEntity;
import org.springframework.stereotype.Component;

@Component
public class EcosIndicatorLatestMapper {

    public EcosIndicatorLatestEntity toEntity(EcosIndicatorLatest domain) {
        return new EcosIndicatorLatestEntity(
            domain.getClassName(),
            domain.getKeystatName(),
            domain.getDataValue(),
            domain.getPreviousDataValue(),
            domain.getCycle(),
            domain.getUpdatedAt()
        );
    }

    public EcosIndicatorLatest toDomain(EcosIndicatorLatestEntity entity) {
        return new EcosIndicatorLatest(
            entity.getClassName(),
            entity.getKeystatName(),
            entity.getDataValue(),
            entity.getPreviousDataValue(),
            entity.getCycle(),
            entity.getUpdatedAt()
        );
    }
}
