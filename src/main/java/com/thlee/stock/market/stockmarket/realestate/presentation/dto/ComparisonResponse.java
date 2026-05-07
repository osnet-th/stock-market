package com.thlee.stock.market.stockmarket.realestate.presentation.dto;

import java.math.BigDecimal;
import java.util.List;

public record ComparisonResponse(String category,
                                 String period,
                                 List<ComparisonRow> rows,
                                 String lastUpdatedAt) {

    public record ComparisonRow(String regionCode,
                                String regionLabel,
                                String indicatorCode,
                                String displayName,
                                String unit,
                                BigDecimal value,
                                BigDecimal changeRate,
                                String referenceText,
                                String source) {
    }
}