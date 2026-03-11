package com.thlee.stock.market.stockmarket.stock.domain.model;

/**
 * 주식 총수 현황
 */
public record StockQuantity(
        String category,
        String totalIssuedStock,
        String currentlyIssuedStock,
        String currentlyDecreasedStock,
        String redeemed,
        String profitCancellation,
        String treasuryStockRetirement,
        String other,
        String issuedTotalQuantity,
        String treasuryStockCount,
        String distributedStockCount
) {}