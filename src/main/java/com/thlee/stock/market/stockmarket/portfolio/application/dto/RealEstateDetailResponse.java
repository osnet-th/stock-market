package com.thlee.stock.market.stockmarket.portfolio.application.dto;

import com.thlee.stock.market.stockmarket.portfolio.domain.model.RealEstateDetail;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class RealEstateDetailResponse {
    private final String subType;
    private final String address;
    private final BigDecimal area;

    private RealEstateDetailResponse(String subType, String address, BigDecimal area) {
        this.subType = subType;
        this.address = address;
        this.area = area;
    }

    public static RealEstateDetailResponse from(RealEstateDetail detail) {
        return new RealEstateDetailResponse(
                detail.getSubType() != null ? detail.getSubType().name() : null,
                detail.getAddress(), detail.getArea()
        );
    }
}