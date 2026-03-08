package com.thlee.stock.market.stockmarket.portfolio.application.dto;

import com.thlee.stock.market.stockmarket.portfolio.domain.model.BondDetail;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
public class BondDetailResponse {
    private final String subType;
    private final LocalDate maturityDate;
    private final BigDecimal couponRate;
    private final String creditRating;

    private BondDetailResponse(String subType, LocalDate maturityDate,
                               BigDecimal couponRate, String creditRating) {
        this.subType = subType;
        this.maturityDate = maturityDate;
        this.couponRate = couponRate;
        this.creditRating = creditRating;
    }

    public static BondDetailResponse from(BondDetail detail) {
        return new BondDetailResponse(
                detail.getSubType() != null ? detail.getSubType().name() : null,
                detail.getMaturityDate(), detail.getCouponRate(), detail.getCreditRating()
        );
    }
}