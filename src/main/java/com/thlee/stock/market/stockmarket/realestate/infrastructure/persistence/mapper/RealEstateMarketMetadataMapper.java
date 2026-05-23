package com.thlee.stock.market.stockmarket.realestate.infrastructure.persistence.mapper;

import com.thlee.stock.market.stockmarket.realestate.domain.model.RealEstateMarketMetadata;
import com.thlee.stock.market.stockmarket.realestate.infrastructure.persistence.RealEstateMarketMetadataEntity;
import org.springframework.stereotype.Component;

@Component
public class RealEstateMarketMetadataMapper {

    public RealEstateMarketMetadataEntity toEntity(RealEstateMarketMetadata domain) {
        return new RealEstateMarketMetadataEntity(
                domain.getCategory(),
                domain.getSource(),
                domain.getIndicatorCode(),
                domain.getDisplayName(),
                domain.getDescription(),
                domain.getUnit(),
                domain.isDefaultVisible()
        );
    }

    public RealEstateMarketMetadata toDomain(RealEstateMarketMetadataEntity entity) {
        return RealEstateMarketMetadata.builder()
                .category(entity.getCategory())
                .source(entity.getSource())
                .indicatorCode(entity.getIndicatorCode())
                .displayName(entity.getDisplayName())
                .description(entity.getDescription())
                .unit(entity.getUnit())
                .defaultVisible(entity.isDefaultVisible())
                .build();
    }
}
