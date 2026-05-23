package com.thlee.stock.market.stockmarket.realestate.infrastructure.persistence.mapper;

import com.thlee.stock.market.stockmarket.realestate.domain.model.RealEstateMarketLatest;
import com.thlee.stock.market.stockmarket.realestate.infrastructure.persistence.RealEstateMarketLatestEntity;
import org.springframework.stereotype.Component;

@Component
public class RealEstateMarketLatestMapper {

    public RealEstateMarketLatestEntity toEntity(RealEstateMarketLatest domain) {
        return new RealEstateMarketLatestEntity(
                domain.getRegionCode(),
                domain.getCategory(),
                domain.getSource(),
                domain.getIndicatorCode(),
                domain.getReferenceText(),
                domain.getPreviousReferenceText(),
                domain.getValue(),
                domain.getPayload(),
                domain.getUpdatedAt()
        );
    }

    public RealEstateMarketLatest toDomain(RealEstateMarketLatestEntity entity) {
        return RealEstateMarketLatest.builder()
                .regionCode(entity.getRegionCode())
                .category(entity.getCategory())
                .source(entity.getSource())
                .indicatorCode(entity.getIndicatorCode())
                .referenceText(entity.getReferenceText())
                .previousReferenceText(entity.getPreviousReferenceText())
                .value(entity.getValue())
                .payload(entity.getPayload())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
