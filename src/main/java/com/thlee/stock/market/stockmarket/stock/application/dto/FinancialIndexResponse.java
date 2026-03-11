package com.thlee.stock.market.stockmarket.stock.application.dto;

import com.thlee.stock.market.stockmarket.stock.domain.model.FinancialIndex;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class FinancialIndexResponse {
    private final String stockCode;
    private final String indexClassCode;
    private final String indexClassName;
    private final String indexCode;
    private final String indexName;
    private final String indexValue;

    public static FinancialIndexResponse from(FinancialIndex index) {
        return new FinancialIndexResponse(
                index.stockCode(),
                index.indexClassCode(),
                index.indexClassName(),
                index.indexCode(),
                index.indexName(),
                index.indexValue()
        );
    }
}