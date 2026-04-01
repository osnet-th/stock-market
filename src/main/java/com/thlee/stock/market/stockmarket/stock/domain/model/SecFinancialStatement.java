package com.thlee.stock.market.stockmarket.stock.domain.model;

import java.util.List;

/**
 * SEC 재무제표 (손익계산서, 재무상태표, 현금흐름표)
 */
public record SecFinancialStatement(
        StatementType statementType,
        List<SecFinancialItem> items
) {
    public enum StatementType {
        INCOME,
        BALANCE,
        CASHFLOW
    }
}