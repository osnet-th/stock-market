package com.thlee.stock.market.stockmarket.portfolio.domain.model;

import com.thlee.stock.market.stockmarket.portfolio.domain.model.enums.FundSubType;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class FundDetail {
    private final FundSubType subType;
    private final BigDecimal managementFee;

    public FundDetail(FundSubType subType,
                      BigDecimal managementFee) {
        this.subType = subType;
        this.managementFee = managementFee;
    }
}