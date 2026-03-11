package com.thlee.stock.market.stockmarket.stock.domain.model;

/**
 * 전체 재무제표 항목
 */
public record FullFinancialStatement(
        String statementDiv,
        String statementName,
        String accountId,
        String accountName,
        String accountDetail,
        String currentTermName,
        String currentTermAmount,
        String previousTermName,
        String previousTermAmount,
        String currency
) {}