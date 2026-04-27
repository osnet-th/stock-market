package com.thlee.stock.market.stockmarket.portfolio.application.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Getter
@RequiredArgsConstructor
public class PortfolioEvaluation {
    private final Long userId;
    private final List<ItemEvaluation> items;
    private final BigDecimal totalInvested;
    private final BigDecimal totalEvaluated;

    @Getter
    @RequiredArgsConstructor
    public static class ItemEvaluation {
        private final Long portfolioItemId;
        private final String itemName;
        private final String assetType;
        private final String country;
        private final BigDecimal investedAmount;
        private final BigDecimal evaluatedAmount;
        private final Integer quantity;
        private final String currentPrice;
        private final String changeRate;
    }
}