package com.thlee.stock.market.stockmarket.realestate.presentation.dto;

import java.math.BigDecimal;

public record HistoryPoint(String referenceText,
                           BigDecimal value,
                           String snapshotDate) {
}