package com.thlee.stock.market.stockmarket.portfolio.domain.model;

import com.thlee.stock.market.stockmarket.portfolio.domain.model.enums.BondSubType;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
public class BondDetail {
    private final BondSubType subType;
    private final LocalDate maturityDate;
    private final BigDecimal couponRate;
    private final String creditRating;

    public BondDetail(BondSubType subType,
                      LocalDate maturityDate,
                      BigDecimal couponRate,
                      String creditRating) {
        this.subType = subType;
        this.maturityDate = maturityDate;
        this.couponRate = couponRate;
        this.creditRating = creditRating;
    }
}