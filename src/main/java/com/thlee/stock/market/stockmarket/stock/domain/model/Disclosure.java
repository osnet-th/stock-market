package com.thlee.stock.market.stockmarket.stock.domain.model;

/**
 * DART 공시 1건
 */
public record Disclosure(
        String receiptNo,
        String reportName,
        String submitterName,
        String receiptDate,
        String remark
) {}
