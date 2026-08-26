package com.thlee.stock.market.stockmarket.realestate.infrastructure.persistence.mapper;

import com.thlee.stock.market.stockmarket.realestate.domain.model.RealEstateMarketIndicator;
import com.thlee.stock.market.stockmarket.realestate.infrastructure.persistence.RealEstateMarketIndicatorEntity;
import org.springframework.stereotype.Component;

@Component
public class RealEstateMarketIndicatorMapper {

    public RealEstateMarketIndicatorEntity toEntity(RealEstateMarketIndicator domain) {
        return new RealEstateMarketIndicatorEntity(
                domain.getId(),
                domain.getRegionCode(),
                domain.getCategory(),
                domain.getSource(),
                domain.getIndicatorCode(),
                domain.getReferenceText(),
                domain.getValue(),
                domain.getPayload(),
                domain.getSourceUrl(),
                domain.getCycle(),
                domain.getSnapshotDate(),
                domain.getCreatedAt()
        );
    }

    public RealEstateMarketIndicator toDomain(RealEstateMarketIndicatorEntity entity) {
        return RealEstateMarketIndicator.builder()
                .id(entity.getId())
                .regionCode(entity.getRegionCode())
                .category(entity.getCategory())
                .source(entity.getSource())
                .indicatorCode(entity.getIndicatorCode())
                .referenceText(entity.getReferenceText())
                .value(entity.getValue())
                .payload(entity.getPayload())
                .sourceUrl(entity.getSourceUrl())
                .cycle(entity.getCycle())
                .snapshotDate(entity.getSnapshotDate())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
