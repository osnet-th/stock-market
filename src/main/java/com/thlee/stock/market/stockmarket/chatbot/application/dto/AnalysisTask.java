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

    public String toUserMessage() {
        return switch (this) {
            case UNDERVALUATION -> "이 종목의 저평가/고평가 여부를 판단해주세요.";
            case TREND_SUMMARY -> "이 종목의 실적 추세를 요약해주세요.";
            case RISK_DIAGNOSIS -> "이 종목의 리스크 요인을 진단해주세요.";
            case INVESTMENT_OPINION -> "이 종목의 투자 적정성 의견을 제시해주세요.";
        };
    }
}