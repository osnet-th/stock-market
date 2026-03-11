package com.thlee.stock.market.stockmarket.stock.application.dto;

import com.thlee.stock.market.stockmarket.stock.domain.model.Lawsuit;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class LawsuitResponse {
    private final String plaintiffName;
    private final String lawsuitAmount;
    private final String claimContent;
    private final String currentProgress;
    private final String futureCounterplan;
    private final String litigationDate;
    private final String confirmationDate;

    public static LawsuitResponse from(Lawsuit lawsuit) {
        return new LawsuitResponse(
                lawsuit.plaintiffName(),
                lawsuit.lawsuitAmount(),
                lawsuit.claimContent(),
                lawsuit.currentProgress(),
                lawsuit.futureCounterplan(),
                lawsuit.litigationDate(),
                lawsuit.confirmationDate()
        );
    }
}