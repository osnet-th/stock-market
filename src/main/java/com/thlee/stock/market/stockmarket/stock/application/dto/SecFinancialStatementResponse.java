package com.thlee.stock.market.stockmarket.stock.application.dto;

import com.thlee.stock.market.stockmarket.stock.domain.model.SecFinancialStatement;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class SecFinancialStatementResponse {
    private final String statementType;
    private final List<SecFinancialItemResponse> items;

    public static SecFinancialStatementResponse from(SecFinancialStatement statement) {
        List<SecFinancialItemResponse> itemResponses = statement.items().stream()
                .map(SecFinancialItemResponse::from)
                .toList();
        return new SecFinancialStatementResponse(
                statement.statementType().name(),
                itemResponses
        );
    }
}