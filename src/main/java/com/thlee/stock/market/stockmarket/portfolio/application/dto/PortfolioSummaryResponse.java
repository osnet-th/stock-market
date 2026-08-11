package com.thlee.stock.market.stockmarket.portfolio.application.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

/**
 * 포트폴리오 상단 요약 (#110)
 *
 * holdingDays / cagr 는 산출 불가 시 null 이다 (매수 이력 없음, 보유 30일 미만 등).
 * fxProfit 은 매수 환율이 기록된 이력만 집계하며, 제외된 건수를 fxUnknownCount 로 노출한다.
 */
@Getter
@RequiredArgsConstructor
public class PortfolioSummaryResponse {
    private final BigDecimal totalEvaluated;
    private final BigDecimal totalInvested;
    private final BigDecimal profit;
    private final BigDecimal profitRate;
    private final Long holdingDays;
    private final BigDecimal cagr;
    private final BigDecimal fxProfit;
    private final int fxUnknownCount;
    private final PortfolioIncomeResponse income;
}
