package com.thlee.stock.market.stockmarket.companyreport.application.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 리포트 스냅샷 (자동 산출 원시 데이터, JSON 컬럼으로 직렬화).
 * 청산가치/DCF 결과는 저장하지 않는다 — valuationInputs와 저장된 파라미터로 조회 시 파생 계산한다.
 * 모든 금액은 통화 최소 단위(KRW: 원, USD: 달러) plain 문자열(쉼표 없음), 비율(%)은 소수 둘째 자리 문자열이다.
 * 값이 없으면 null (0 대체 금지).
 *
 * <p>schemaVersion 2: country/currency/unsupportedSections 추가. v1 스냅샷은 역직렬화 시
 * KR/KRW/빈 목록으로 해석한다 ({@code ReportSnapshotJsonMapper} 참조).
 * unsupportedSections는 데이터 소스가 구조적으로 제공하지 않는 섹션 키 목록 —
 * "데이터 없음(조회 실패)"과 "미지원(설계)"을 구분해 프론트가 섹션 제거·안내 배너를 렌더한다.
 */
public record ReportSnapshot(
        int schemaVersion,
        String stockCode,
        String stockName,
        String country,
        String currency,
        List<String> unsupportedSections,
        String fsDiv,
        CompanyProfileData companyProfile,
        List<ColumnMeta> columns,
        List<MetricRow> performance,
        List<MetricRow> statements,
        List<RatioRow> ratios,
        PriceMetrics priceMetrics,
        ValuationInputs valuationInputs,
        Shareholders shareholders,
        RiskSignals riskSignals
) {

    public static final int CURRENT_SCHEMA_VERSION = 2;

    public static final String COUNTRY_KR = "KR";
    public static final String COUNTRY_US = "US";
    public static final String CURRENCY_KRW = "KRW";
    public static final String CURRENCY_USD = "USD";

    public record ColumnMeta(String year, String reportLabel, boolean partial) {}

    /**
     * 항목 × 연도 매트릭스 행. values: 연도 → 값
     */
    public record MetricRow(String key, String name, Map<String, String> values) {}

    /**
     * 재무지표 행. category: health|profitability|growth, judgment: good|warn|risk (최신 사업연도 기준, 판정 불가 시 null)
     */
    public record RatioRow(String key, String name, String category, Map<String, String> values, String judgment) {}

    public record CompanyProfileData(
            String corpName,
            String corpNameEng,
            String stockName,
            String ceoName,
            String address,
            String homepageUrl,
            String industryCode,
            String establishedDate,
            String accountSettlementMonth
    ) {}

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
            BigDecimal perTimesPbr,
            BigDecimal evEbitda,
            BigDecimal roic,
            BigDecimal accrualRatio,
            List<String> warnings,
            Map<String, MetricBreakdown> breakdowns
    ) {}

    /**
     * 주가지표 계산 근거 (툴팁 표시용). terms: 식의 항(op = 앞 항과의 연산자, 첫 항은 null),
     * result: 지표값, extras: 식에는 안 들어가지만 참고로 노출하는 중간값(상각비·실효세율 등, 없으면 null).
     * 근거를 완전히 구성할 수 없으면(중간값 결손) 해당 지표는 breakdowns에서 생략한다 → 프론트는 공식만 표시.
     */
    public record MetricBreakdown(
            List<BreakdownTerm> terms,
            BigDecimal result,
            List<BreakdownTerm> extras
    ) {}

    /**
     * 계산 근거 항. unit: amount(큰 금액→조/억)|price(주당 원)|shares(주식수)|x(배)|pct(%)
     */
    public record BreakdownTerm(String op, String label, BigDecimal value, String unit) {}

    /**
     * 청산가치/DCF 파생 계산의 원시 입력 (최신 사업연도 기준)
     */
    public record ValuationInputs(
            String baseYear,
            BigDecimal cash,
            BigDecimal securities,
            BigDecimal receivables,
            BigDecimal inventory,
            BigDecimal investments,
            BigDecimal tangible,
            BigDecimal intangible,
            BigDecimal otherCurrent,
            BigDecimal otherNonCurrent,
            BigDecimal totalLiabilities,
            BigDecimal borrowings,
            BigDecimal netCash,
            BigDecimal fcf,
            List<UnclassifiedLine> unclassified
    ) {}

    /**
     * 조정자산 카테고리에 매핑되지 않아 0% 처리된 계정 (검산용)
     */
    public record UnclassifiedLine(String name, BigDecimal bookValue, String bucket) {}

    public record Shareholders(
            List<ShareholderRow> majorShareholders,
            List<BulkHoldingRow> bulkHoldings,
            List<DividendRow> dividends,
            String treasuryStockCount
    ) {}

    public record ShareholderRow(
            String bsnsYear,
            String stockKind,
            String name,
            String relation,
            String baseQuantity,
            String baseRatio,
            String termEndQuantity,
            String termEndRatio,
            String remark
    ) {}

    public record BulkHoldingRow(
            String receiptNo,
            String receiptDate,
            String reporter,
            String holdingQuantity,
            String quantityChange,
            String holdingRatio,
            String ratioChange,
            String reportReason
    ) {}

    public record DividendRow(
            String category,
            String stockKind,
            String currentTerm,
            String previousTerm,
            String beforePreviousTerm
    ) {}

    /**
     * 위험 기업 시그널 (최신 사업연도 기준). 판정 불가 항목은 null.
     */
    public record RiskSignals(
            Boolean currentLiabilitiesExceedCurrentAssets,
            Boolean lowEquityRatio,
            Boolean negativeEquity,
            Boolean receivablesSurge,
            Boolean inventorySurge,
            Boolean recentCapitalIncrease,
            Map<String, String> evidence
    ) {}
}
