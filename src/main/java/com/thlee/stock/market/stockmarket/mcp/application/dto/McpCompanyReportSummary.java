package com.thlee.stock.market.stockmarket.mcp.application.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 기업 리포트 요약 (Claude 응답용). 10년 원본 재무 행은 빼고 최근 5개 연도 비율만 담는다.
 *
 * @param currency   금액 통화 (KRW/USD). 금액은 이 통화의 기본 단위(원·달러)다
 * @param baseYear   지표 기준 연도 (최근 사업보고서 연도)
 * @param ratioUnit  비율 값 단위 (%)
 */
public record McpCompanyReportSummary(
        String stockCode,
        String stockName,
        String country,
        String currency,
        String baseYear,
        String ratioUnit,
        List<Column> columns,
        PriceMetrics priceMetrics,
        List<Ratio> ratios,
        RiskSignals riskSignals,
        Valuation valuation,
        boolean srimAvailable,
        List<String> unsupportedSections
) {

    /** 연도별 기준 보고서. partial=true면 그 해의 분기·반기 보고서(연간 아님) */
    public record Column(String year, String reportLabel, boolean partial) {}

    /**
     * 주가지표. referencePrice·marketCap은 금액, per·pbr·psr·pcfr·evEbitda는 배수, roic는 %.
     */
    public record PriceMetrics(
            String termName,
            String referencePriceDate,
            BigDecimal referencePrice,
            BigDecimal marketCap,
            BigDecimal eps,
            BigDecimal bps,
            BigDecimal per,
            BigDecimal pbr,
            BigDecimal psr,
            BigDecimal pcfr,
            BigDecimal evEbitda,
            BigDecimal roicPercent,
            List<String> warnings
    ) {}

    /** 비율 행. values: 연도 → 값(%), judgment: good/warn/risk */
    public record Ratio(String key, String name, String category, Map<String, String> values, String judgment) {}

    /** 위험 신호 (최신 사업연도 기준, 판정 불가 항목은 null) */
    public record RiskSignals(
            Boolean currentLiabilitiesExceedCurrentAssets,
            Boolean lowEquityRatio,
            Boolean negativeEquity,
            Boolean receivablesSurge,
            Boolean inventorySurge,
            Boolean recentCapitalIncrease,
            Map<String, String> evidence
    ) {}

    /** 청산가치·DCF (기본 가정). marketCapTo* 는 시가총액 ÷ 가치(배) */
    public record Valuation(
            String baseYear,
            BigDecimal liquidationValue,
            BigDecimal marketCapToLiquidation,
            boolean dcfCalculable,
            String dcfReason,
            BigDecimal dcfConservativeValue,
            BigDecimal dcfOptimisticValue,
            BigDecimal marketCap,
            BigDecimal marketCapToDcfConservative,
            BigDecimal marketCapToDcfOptimistic
    ) {}
}
