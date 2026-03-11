package com.thlee.stock.market.stockmarket.stock.application.dto;

import com.thlee.stock.market.stockmarket.stock.domain.model.FinancialAccount;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class FinancialAccountResponse {
    private final String stockCode;
    private final String accountName;
    private final String fsDiv;
    private final String fsName;
    private final String statementDiv;
    private final String statementName;
    private final String currentTermName;
    private final String currentTermAmount;
    private final String previousTermName;
    private final String previousTermAmount;
    private final String beforePreviousTermName;
    private final String beforePreviousTermAmount;
    private final String currency;

    public static FinancialAccountResponse from(FinancialAccount account) {
        return new FinancialAccountResponse(
                account.stockCode(),
                account.accountName(),
                account.fsDiv(),
                account.fsName(),
                account.statementDiv(),
                account.statementName(),
                account.currentTermName(),
                account.currentTermAmount(),
                account.previousTermName(),
                account.previousTermAmount(),
                account.beforePreviousTermName(),
                account.beforePreviousTermAmount(),
                account.currency()
        );
    }
}
