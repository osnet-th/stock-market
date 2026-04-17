package com.thlee.stock.market.stockmarket.chatbot.application.dto;

import java.util.List;

public enum AnalysisTask {
    UNDERVALUATION(List.of(
            FinancialCategory.ACCOUNT,
            FinancialCategory.PROFITABILITY,
            FinancialCategory.VALUATION
    )),
    TREND_SUMMARY(List.of(
            FinancialCategory.ACCOUNT,
            FinancialCategory.PROFITABILITY,
            FinancialCategory.GROWTH
    )),
    RISK_DIAGNOSIS(List.of(
            FinancialCategory.ACCOUNT,
            FinancialCategory.STABILITY,
            FinancialCategory.ACTIVITY
    )),
    INVESTMENT_OPINION(List.of(
            FinancialCategory.ACCOUNT,
            FinancialCategory.PROFITABILITY,
            FinancialCategory.STABILITY,
            FinancialCategory.GROWTH,
            FinancialCategory.ACTIVITY,
            FinancialCategory.VALUATION
    ));

    private final List<FinancialCategory> categories;

    AnalysisTask(List<FinancialCategory> categories) {
        this.categories = categories;
    }

    public List<FinancialCategory> categories() {
        return categories;
    }
}