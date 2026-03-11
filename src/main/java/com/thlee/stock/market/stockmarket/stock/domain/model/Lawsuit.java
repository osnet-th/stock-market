package com.thlee.stock.market.stockmarket.stock.domain.model;

/**
 * 소송 정보
 */
public record Lawsuit(
        String plaintiffName,
        String lawsuitAmount,
        String claimContent,
        String currentProgress,
        String futureCounterplan,
        String litigationDate,
        String confirmationDate
) {}