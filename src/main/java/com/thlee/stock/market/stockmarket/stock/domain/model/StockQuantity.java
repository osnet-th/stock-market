package com.thlee.stock.market.stockmarket.stock.domain.model;

/**
 * 주식 총수 현황. settlementDate는 DART 결산기준일(stlm_dt, yyyy-MM-dd)이며 응답에 없으면 null.
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
        String distributedStockCount,
        String settlementDate
) {}