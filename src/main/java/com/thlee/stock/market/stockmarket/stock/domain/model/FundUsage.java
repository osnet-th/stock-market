package com.thlee.stock.market.stockmarket.stock.domain.model;

/**
 * 자금 사용내역 (사모/공모 공통)
 */
public record FundUsage(
        String category,
        String sequence,
        String paymentDate,
        String usePurpose,
        String planAmount,
        String actualContent,
        String actualAmount,
        String differenceReason
) {}