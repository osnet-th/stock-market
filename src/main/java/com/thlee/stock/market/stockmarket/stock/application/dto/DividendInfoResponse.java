package com.thlee.stock.market.stockmarket.stock.application.dto;

import com.thlee.stock.market.stockmarket.stock.domain.model.DividendInfo;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class DividendInfoResponse {
    private final String category;
    private final String stockKind;
    private final String currentTerm;
    private final String previousTerm;
    private final String beforePreviousTerm;

    public static DividendInfoResponse from(DividendInfo info) {
        return new DividendInfoResponse(
                info.category(),
                info.stockKind(),
                info.currentTerm(),
                info.previousTerm(),
                info.beforePreviousTerm()
        );
    }
}