package com.thlee.stock.market.stockmarket.mcp.presentation.dto;

import com.thlee.stock.market.stockmarket.portfolio.application.dto.PortfolioSummaryResponse;

/**
 * 포트폴리오 요약. 금액은 원(KRW), profitRate·cagr·income.dividendYield는 %다.
 * income.basis: ACTUAL_PAYMENT_DATE(국내 배당 지급일 기준) / ESTIMATED_MONTHLY_AVERAGE(연 예상액의 월 평균)
 */
public record McpPortfolioSummary(String currency, String rateUnit, PortfolioSummaryResponse summary) {

    public static McpPortfolioSummary from(PortfolioSummaryResponse summary) {
        return new McpPortfolioSummary("KRW", "%", summary);
    }
}
