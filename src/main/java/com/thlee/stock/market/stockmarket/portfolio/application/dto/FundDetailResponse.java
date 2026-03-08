package com.thlee.stock.market.stockmarket.portfolio.application.dto;

import com.thlee.stock.market.stockmarket.portfolio.domain.model.FundDetail;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class FundDetailResponse {
    private final String subType;
    private final BigDecimal managementFee;

    private FundDetailResponse(String subType, BigDecimal managementFee) {
        this.subType = subType;
        this.managementFee = managementFee;
    }

    public static FundDetailResponse from(FundDetail detail) {
        return new FundDetailResponse(
                detail.getSubType() != null ? detail.getSubType().name() : null,
                detail.getManagementFee()
        );
    }
}