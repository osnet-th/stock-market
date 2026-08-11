package com.thlee.stock.market.stockmarket.portfolio.application.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

/**
 * 배당 · 이자 집계 (#110)
 *
 * basis 는 집계 기준을 화면에 그대로 노출하기 위한 코드다.
 * `ESTIMATED_MONTHLY_AVERAGE` = 연 예상액을 12로 나눈 월 평균 환산치이며,
 * 실제 배당 지급일 기준 금액이 아니다.
 */
@Getter
@RequiredArgsConstructor
public class PortfolioIncomeResponse {
    private final BigDecimal monthAmount;
    private final BigDecimal yearEstimate;
    private final BigDecimal dividendYield;
    private final String basis;
    private final int excludedCount;
}
