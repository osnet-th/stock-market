package com.thlee.stock.market.stockmarket.stock.domain.model;

/**
 * 대량보유 상황보고 (5%룰 공시)
 */
public record BulkHoldingReport(
        String receiptNo,
        String receiptDate,
        String reportType,
        String reporter,
        String holdingQuantity,
        String quantityChange,
        String holdingRatio,
        String ratioChange,
        String contractQuantity,
        String contractRatio,
        String reportReason
) {}
