package com.thlee.stock.market.stockmarket.stock.application.dto;

import com.thlee.stock.market.stockmarket.stock.domain.model.FullFinancialStatement;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class FullFinancialStatementResponse {
    private final String statementDiv;
    private final String statementName;
    private final String accountId;
    private final String accountName;
    private final String accountDetail;
    private final String currentTermName;
    private final String currentTermAmount;
    private final String previousTermName;
    private final String previousTermAmount;
    private final String currency;

    public static FullFinancialStatementResponse from(FullFinancialStatement statement) {
        return new FullFinancialStatementResponse(
                statement.statementDiv(),
                statement.statementName(),
                statement.accountId(),
                statement.accountName(),
                statement.accountDetail(),
                statement.currentTermName(),
                statement.currentTermAmount(),
                statement.previousTermName(),
                statement.previousTermAmount(),
                statement.currency()
        );
    }
}