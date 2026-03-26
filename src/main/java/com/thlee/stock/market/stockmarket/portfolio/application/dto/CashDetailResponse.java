package com.thlee.stock.market.stockmarket.portfolio.application.dto;

import com.thlee.stock.market.stockmarket.portfolio.domain.model.CashDetail;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
public class CashDetailResponse {
    private final String subType;
    private final BigDecimal interestRate;
    private final LocalDate startDate;
    private final LocalDate maturityDate;
    private final String taxType;

    private CashDetailResponse(String subType, BigDecimal interestRate,
                               LocalDate startDate, LocalDate maturityDate,
                               String taxType) {
        this.subType = subType;
        this.interestRate = interestRate;
        this.startDate = startDate;
        this.maturityDate = maturityDate;
        this.taxType = taxType;
    }

    public static CashDetailResponse from(CashDetail detail) {
        return new CashDetailResponse(
                detail.getSubType().name(),
                detail.getInterestRate(),
                detail.getStartDate(),
                detail.getMaturityDate(),
                detail.getTaxType() != null ? detail.getTaxType().name() : null
        );
    }
}