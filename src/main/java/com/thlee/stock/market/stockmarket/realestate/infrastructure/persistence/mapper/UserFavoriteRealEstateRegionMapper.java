package com.thlee.stock.market.stockmarket.realestate.infrastructure.persistence.mapper;

import com.thlee.stock.market.stockmarket.realestate.domain.model.UserFavoriteRealEstateRegion;
import com.thlee.stock.market.stockmarket.realestate.infrastructure.persistence.UserFavoriteRealEstateRegionEntity;
import org.springframework.stereotype.Component;

@Component
public class UserFavoriteRealEstateRegionMapper {

    public UserFavoriteRealEstateRegionEntity toEntity(UserFavoriteRealEstateRegion domain) {
        return new UserFavoriteRealEstateRegionEntity(
                domain.getId(),
                domain.getUserId(),
                domain.getSidoCode(),
                domain.getRegionCode(),
                domain.getEmdCode() == null ? "" : domain.getEmdCode(),
                domain.getDisplayOrder(),
                domain.getCreatedAt()
        );
    }

    public UserFavoriteRealEstateRegion toDomain(UserFavoriteRealEstateRegionEntity entity) {
        return UserFavoriteRealEstateRegion.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .sidoCode(entity.getSidoCode())
                .regionCode(entity.getRegionCode())
                .emdCode(entity.getEmdCode())
                .displayOrder(entity.getDisplayOrder())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
