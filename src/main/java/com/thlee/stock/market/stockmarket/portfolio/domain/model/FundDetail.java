package com.thlee.stock.market.stockmarket.portfolio.domain.model;

import com.thlee.stock.market.stockmarket.portfolio.domain.model.enums.FundSubType;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class FundDetail {
    private final FundSubType subType;
    private final BigDecimal managementFee;
    private final BigDecimal monthlyDepositAmount;
    private final Integer depositDay;

    public FundDetail(FundSubType subType,
                      BigDecimal managementFee) {
        this(subType, managementFee, null, null);
    }

    public FundDetail(FundSubType subType,
                      BigDecimal managementFee,
                      BigDecimal monthlyDepositAmount,
                      Integer depositDay) {
        this.subType = subType;
        this.managementFee = managementFee;
        this.monthlyDepositAmount = monthlyDepositAmount;
        this.depositDay = depositDay;
    }
}