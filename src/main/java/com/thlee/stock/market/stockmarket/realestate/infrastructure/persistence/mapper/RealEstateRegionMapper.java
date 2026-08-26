package com.thlee.stock.market.stockmarket.realestate.infrastructure.persistence.mapper;

import com.thlee.stock.market.stockmarket.realestate.domain.model.RealEstateRegion;
import com.thlee.stock.market.stockmarket.realestate.infrastructure.persistence.RealEstateRegionEntity;
import org.springframework.stereotype.Component;

@Component
public class RealEstateRegionMapper {

    public RealEstateRegionEntity toEntity(RealEstateRegion domain) {
        return new RealEstateRegionEntity(
                domain.getRegionCode(),
                domain.getEmdCode() == null ? "" : domain.getEmdCode(),
                domain.getSidoCode(),
                domain.getSidoName(),
                domain.getSigunguName(),
                domain.getEmdName(),
                domain.getDisplayOrder()
        );
    }

    public RealEstateRegion toDomain(RealEstateRegionEntity entity) {
        return RealEstateRegion.builder()
                .regionCode(entity.getRegionCode())
                .sidoCode(entity.getSidoCode())
                .sidoName(entity.getSidoName())
                .sigunguName(entity.getSigunguName())
                .emdCode(entity.getEmdCode())
                .emdName(entity.getEmdName())
                .displayOrder(entity.getDisplayOrder())
                .build();
    }
}
