package com.thlee.stock.market.stockmarket.stock.domain.model;

/**
 * 재무계정 항목 (단일/다중회사 공통)
 */
public record FinancialAccount(
        String stockCode,
        String accountName,
        String fsDiv,
        String fsName,
        String statementDiv,
        String statementName,
        String currentTermName,
        String currentTermAmount,
        String previousTermName,
        String previousTermAmount,
        String beforePreviousTermName,
        String beforePreviousTermAmount,
        String currency
) {}