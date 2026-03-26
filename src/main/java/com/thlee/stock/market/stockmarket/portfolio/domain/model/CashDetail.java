package com.thlee.stock.market.stockmarket.portfolio.domain.model;

import com.thlee.stock.market.stockmarket.portfolio.domain.model.enums.CashSubType;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.enums.TaxType;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
public class CashDetail {
    private final CashSubType subType;
    private final BigDecimal interestRate;
    private final LocalDate startDate;
    private final LocalDate maturityDate;
    private final TaxType taxType;

    public CashDetail(CashSubType subType,
                      BigDecimal interestRate,
                      LocalDate startDate,
                      LocalDate maturityDate,
                      TaxType taxType) {
        this.subType = subType;
        this.interestRate = interestRate;
        this.startDate = startDate;
        this.maturityDate = maturityDate;
        this.taxType = taxType;
    }
}