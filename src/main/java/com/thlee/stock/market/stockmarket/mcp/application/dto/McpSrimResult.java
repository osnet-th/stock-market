package com.thlee.stock.market.stockmarket.mcp.application.dto;

import java.math.BigDecimal;

/**
 * S-RIM 적정주가 계산 결과 (Claude 응답용).
 * calculable=false면 reason에 이유가 있고 나머지는 비어 있다.
 *
 * @param equityReport          자기자본 근거 보고서 (예: 2026 반기보고서)
 * @param sharesReport          주식수 근거 보고서
 * @param currency              금액 통화 (KRW/USD). 금액은 이 통화의 기본 단위(원·달러)다
 * @param requiredReturnPercent 쓴 요구수익률(%)
 * @param requiredReturnSource  요구수익률 출처 (입력값 또는 회사채 BBB- 5년 금리와 기준일)
 * @param roePercent            쓴 ROE(%)
 * @param roeSource             ROE 출처 (입력값 또는 리포트 연도)
 */
public record McpSrimResult(
        String stockCode,
        String stockName,
        boolean calculable,
        String reason,
        String currency,
        BigDecimal equity,
        String equityDate,
        String equityReport,
        BigDecimal shares,
        String sharesDate,
        String sharesReport,
        BigDecimal requiredReturnPercent,
        String requiredReturnSource,
        BigDecimal roePercent,
        String roeSource,
        BigDecimal referencePrice,
        String referencePriceDate,
        Integer baseYear,
        Scenario perpetual,
        Scenario decline10,
        Scenario decline20
) {

    /** 시나리오별 적정주가. differencePercent는 기준 주가 대비 차이(%) */
    public record Scenario(BigDecimal price, BigDecimal differencePercent) {}

    public static McpSrimResult notCalculable(String stockCode, String stockName, String reason) {
        return new McpSrimResult(stockCode, stockName, false, reason, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null);
    }
}
