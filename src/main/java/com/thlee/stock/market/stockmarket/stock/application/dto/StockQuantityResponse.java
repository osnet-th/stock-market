package com.thlee.stock.market.stockmarket.stock.application.dto;

import com.thlee.stock.market.stockmarket.stock.domain.model.StockQuantity;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class StockQuantityResponse {
    private final String category;
    private final String totalIssuedStock;
    private final String currentlyIssuedStock;
    private final String currentlyDecreasedStock;
    private final String redeemed;
    private final String profitCancellation;
    private final String treasuryStockRetirement;
    private final String other;
    private final String issuedTotalQuantity;
    private final String treasuryStockCount;
    private final String distributedStockCount;

    public static StockQuantityResponse from(StockQuantity quantity) {
        return new StockQuantityResponse(
                quantity.category(),
                quantity.totalIssuedStock(),
                quantity.currentlyIssuedStock(),
                quantity.currentlyDecreasedStock(),
                quantity.redeemed(),
                quantity.profitCancellation(),
                quantity.treasuryStockRetirement(),
                quantity.other(),
                quantity.issuedTotalQuantity(),
                quantity.treasuryStockCount(),
                quantity.distributedStockCount()
        );
    }
}