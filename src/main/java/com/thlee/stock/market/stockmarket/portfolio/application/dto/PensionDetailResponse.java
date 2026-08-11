package com.thlee.stock.market.stockmarket.portfolio.application.dto;

import com.thlee.stock.market.stockmarket.portfolio.domain.model.PensionDetail;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class PensionDetailResponse {
    private final String subType;
    private final String provider;
    private final BigDecimal evaluatedAmount;
    private final BigDecimal monthlyDepositAmount;
    private final Integer depositDay;

    private PensionDetailResponse(String subType, String provider, BigDecimal evaluatedAmount,
                                  BigDecimal monthlyDepositAmount, Integer depositDay) {
        this.subType = subType;
        this.provider = provider;
        this.evaluatedAmount = evaluatedAmount;
        this.monthlyDepositAmount = monthlyDepositAmount;
        this.depositDay = depositDay;
    }

    public static PensionDetailResponse from(PensionDetail detail) {
        return new PensionDetailResponse(
                detail.getSubType() != null ? detail.getSubType().name() : null,
                detail.getProvider(),
                detail.getEvaluatedAmount(),
                detail.getMonthlyDepositAmount(),
                detail.getDepositDay()
        );
    }
}
