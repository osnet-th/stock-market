package com.thlee.stock.market.stockmarket.stock.domain.model;

/**
 * 재무지표 (단일/다중회사 공통)
 */
public record FinancialIndex(
        String stockCode,
        String indexClassCode,
        String indexClassName,
        String indexCode,
        String indexName,
        String indexValue
) {}