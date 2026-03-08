package com.thlee.stock.market.stockmarket.portfolio.domain.model;

import com.thlee.stock.market.stockmarket.portfolio.domain.model.enums.RealEstateSubType;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class RealEstateDetail {
    private final RealEstateSubType subType;
    private final String address;
    private final BigDecimal area;

    public RealEstateDetail(RealEstateSubType subType,
                            String address,
                            BigDecimal area) {
        this.subType = subType;
        this.address = address;
        this.area = area;
    }
}