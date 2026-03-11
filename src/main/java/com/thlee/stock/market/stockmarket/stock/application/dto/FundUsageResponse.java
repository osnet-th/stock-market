package com.thlee.stock.market.stockmarket.stock.application.dto;

import com.thlee.stock.market.stockmarket.stock.domain.model.FundUsage;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class FundUsageResponse {
    private final String category;
    private final String sequence;
    private final String paymentDate;
    private final String usePurpose;
    private final String planAmount;
    private final String actualContent;
    private final String actualAmount;
    private final String differenceReason;

    public static FundUsageResponse from(FundUsage usage) {
        return new FundUsageResponse(
                usage.category(),
                usage.sequence(),
                usage.paymentDate(),
                usage.usePurpose(),
                usage.planAmount(),
                usage.actualContent(),
                usage.actualAmount(),
                usage.differenceReason()
        );
    }
}