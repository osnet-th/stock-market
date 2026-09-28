package com.thlee.stock.market.stockmarket.companyreport.application;

import com.thlee.stock.market.stockmarket.companyreport.application.dto.ReportSnapshot;
import com.thlee.stock.market.stockmarket.companyreport.application.dto.ReportSnapshot.MetricRow;
import com.thlee.stock.market.stockmarket.companyreport.application.dto.ReportSnapshot.RatioRow;
import com.thlee.stock.market.stockmarket.companyreport.application.dto.ReportSnapshot.SrimBasis;
import com.thlee.stock.market.stockmarket.stock.application.dto.StockQuantityResponse;
import com.thlee.stock.market.stockmarket.stock.application.dto.ValuationMetricResponse;
import com.thlee.stock.market.stockmarket.stock.domain.model.PeriodicReport;
import com.thlee.stock.market.stockmarket.stock.domain.model.ReportCode;
import com.thlee.stock.market.stockmarket.stock.application.dto.FinancialTimelineResponse;
import com.thlee.stock.market.stockmarket.stock.application.dto.FinancialTimelineResponse.TimelineColumn;
import com.thlee.stock.market.stockmarket.stock.application.dto.FinancialTimelineResponse.TimelineDetailGroup;
import com.thlee.stock.market.stockmarket.stock.application.dto.FinancialTimelineResponse.TimelineDetailNode;
import com.thlee.stock.market.stockmarket.stock.application.dto.FinancialTimelineResponse.TimelineRow;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SnapshotFinancialExtractor — 지배주주 계정·ROE·S-RIM 근거")
class SnapshotFinancialExtractorTest {

    private static final String OWNERS_EQUITY_ID = "ifrs-full_EquityAttributableToOwnersOfParent";
    private static final String NCI_ID = "ifrs-full_NoncontrollingInterests";
    private static final String OWNERS_NET_INCOME_ID = "ifrs-full_ProfitLossAttributableToOwnersOfParent";
    private static final String OWNERS_NAME = "지배기업 소유주지분";

    private final SnapshotFinancialExtractor extractor = new SnapshotFinancialExtractor();

    @Test
    @DisplayName("A1 지배주주지분·비지배지분·지배주주순이익이 연도별 요약 행으로 생성된다")
    void ownersRowsByYear() {
        FinancialTimelineResponse timeline = timeline(
                List.of(group("BS", List.of(
                        row(OWNERS_EQUITY_ID, OWNERS_NAME, Map.of("2023", "800", "2024", "900", "2025", "1000")),
                        row(NCI_ID, "비지배지분", Map.of("2023", "20", "2024", "30", "2025", "40")))),
                        group("IS", List.of(
                                row(OWNERS_NET_INCOME_ID, OWNERS_NAME, Map.of("2023", "80", "2024", "90", "2025", "100"))))));

        List<MetricRow> rows = extractor.statementRows(timeline);

        assertThat(values(rows, "bs.ownersEquity")).containsExactlyInAnyOrderEntriesOf(
                Map.of("2023", "800", "2024", "900", "2025", "1000"));
        assertThat(values(rows, "bs.nonControllingInterests")).containsExactlyInAnyOrderEntriesOf(
                Map.of("2023", "20", "2024", "30", "2025", "40"));
        assertThat(values(rows, "is.ownersNetIncome")).containsExactlyInAnyOrderEntriesOf(
                Map.of("2023", "80", "2024", "90", "2025", "100"));
    }

    @Test
    @DisplayName("A2 BS·IS 계정명이 같아도 재무제표 구분과 계정 ID로 각각 올바른 값을 꺼낸다")
    void sameNameSeparatedByStatementAndId() {
        FinancialTimelineResponse timeline = timeline(List.of(
                group("BS", List.of(row(OWNERS_EQUITY_ID, OWNERS_NAME, Map.of("2025", "1000")))),
                group("IS", List.of(
                        row("ifrs-full_ProfitLoss", "당기순이익", Map.of("2025", "120")),
                        row(OWNERS_NET_INCOME_ID, OWNERS_NAME, Map.of("2025", "100"))))));

        List<MetricRow> rows = extractor.statementRows(timeline);

        assertThat(values(rows, "bs.ownersEquity")).containsEntry("2025", "1000");
        assertThat(values(rows, "is.ownersNetIncome")).containsEntry("2025", "100");
    }

    @Test
    @DisplayName("A3 지배주주순이익이 포괄손익계산서(CIS)에만 있으면 CIS 값으로 추출한다")
    void ownersNetIncomeFromCis() {
        FinancialTimelineResponse timeline = timeline(List.of(
                group("BS", List.of(row(OWNERS_EQUITY_ID, OWNERS_NAME, Map.of("2025", "1000")))),
                group("CIS", List.of(row(OWNERS_NET_INCOME_ID, OWNERS_NAME, Map.of("2025", "95"))))));

        List<MetricRow> rows = extractor.statementRows(timeline);

        assertThat(values(rows, "is.ownersNetIncome")).containsEntry("2025", "95");
    }

    @Test
    @DisplayName("A4 지배주주 계정이 모두 있으면 ROE = 지배주주순이익 ÷ 지배주주지분, 기준은 지배주주")
    void roeOnOwnersBasis() {
        FinancialTimelineResponse timeline = timeline(
                summary("120", "1100"),
                List.of(group("BS", List.of(row(OWNERS_EQUITY_ID, OWNERS_NAME, Map.of("2025", "1000")))),
                        group("IS", List.of(row(OWNERS_NET_INCOME_ID, OWNERS_NAME, Map.of("2025", "100"))))),
                List.of(column("2025", "11011", false)));

        assertThat(roe(timeline)).containsEntry("2025", "10.00");
        assertThat(extractor.roeBasis(timeline)).isEqualTo(ReportSnapshot.ROE_BASIS_OWNERS);
    }

    @Test
    @DisplayName("A5 지배주주 계정이 없으면(개별재무제표) ROE = 당기순이익 ÷ 자본총계, 기준은 전체")
    void roeOnTotalBasisWithoutOwnersAccounts() {
        FinancialTimelineResponse timeline = timeline(summary("120", "1100"), List.of(),
                List.of(column("2025", "11011", false)));

        assertThat(roe(timeline)).containsEntry("2025", "10.91");
        assertThat(extractor.roeBasis(timeline)).isEqualTo(ReportSnapshot.ROE_BASIS_TOTAL);
    }

    @Test
    @DisplayName("A6 지배주주지분만 있고 지배주주순이익이 없으면 기준을 섞지 않고 전체 기준으로 계산한다")
    void roeFallsBackToTotalWhenOwnersIncomeMissing() {
        FinancialTimelineResponse timeline = timeline(
                summary("120", "1100"),
                List.of(group("BS", List.of(row(OWNERS_EQUITY_ID, OWNERS_NAME, Map.of("2025", "1000"))))),
                List.of(column("2025", "11011", false)));

        assertThat(roe(timeline)).containsEntry("2025", "10.91");
        assertThat(extractor.roeBasis(timeline)).isEqualTo(ReportSnapshot.ROE_BASIS_TOTAL);
    }

    @Test
    @DisplayName("A7 최신 분기 컬럼에 지배주주지분이 있으면 그 값과 분기 결산일을 S-RIM 근거로 쓴다")
    void srimEquityFromLatestColumn() {
        FinancialTimelineResponse timeline = timeline(List.of(),
                List.of(group("BS", List.of(row(OWNERS_EQUITY_ID, OWNERS_NAME, Map.of("2025", "1000", "2026", "1100"))))),
                List.of(column("2025", "11011", false), column("2026", "11014", true)));

        SrimBasis basis = extractor.srimBasis(timeline, "2025", List.of(), null);

        assertThat(basis.equity()).isEqualByComparingTo("1100");
        assertThat(basis.equityDate()).isEqualTo("2026-09-30");
        assertThat(basis.yearEndEquities()).containsOnlyKeys("2025");
    }

    @Test
    @DisplayName("A8 최신 분기 컬럼에 값이 없으면 기준연도 연간 값과 12월 말 기준일을 쓴다")
    void srimEquityFallsBackToBaseYear() {
        FinancialTimelineResponse timeline = timeline(List.of(),
                List.of(group("BS", List.of(row(OWNERS_EQUITY_ID, OWNERS_NAME, Map.of("2025", "1000"))))),
                List.of(column("2025", "11011", false), column("2026", "11014", true)));

        SrimBasis basis = extractor.srimBasis(timeline, "2025", List.of(), null);

        assertThat(basis.equity()).isEqualByComparingTo("1000");
        assertThat(basis.equityDate()).isEqualTo("2025-12-31");
    }

    @Test
    @DisplayName("A9 S-RIM 주식수는 합계 유통주식수, 시가총액은 기존대로 보통주 유통주식수를 쓴다")
    void srimUsesTotalSharesWhilePriceMetricsKeepCommon() {
        FinancialTimelineResponse timeline = timeline(List.of(), List.of(), List.of(column("2025", "11011", false)));
        List<StockQuantityResponse> quantities = List.of(quantity("보통주", "900"), quantity("우선주", "100"),
                quantity("합계", "1000"));
        ValuationMetricResponse valuation = new ValuationMetricResponse(
                null, null, null, null, null, "2026-09-25", new BigDecimal("10"), List.of());

        SrimBasis basis = extractor.srimBasis(timeline, "2025", quantities,
                new PeriodicReport(2026, ReportCode.SEMI_ANNUAL));
        BigDecimal marketCap = extractor.priceMetrics(timeline, "2025", valuation, quantities).marketCap();

        assertThat(basis.shares()).isEqualByComparingTo("1000");
        assertThat(basis.sharesCategory()).isEqualTo("합계");
        assertThat(basis.sharesReport()).isEqualTo("2026 반기보고서");
        assertThat(basis.sharesDate()).isEqualTo("2026-06-30");
        assertThat(marketCap).isEqualByComparingTo("9000");
    }

    @Test
    @DisplayName("A10 합계 행이 없으면 S-RIM 주식수를 비운다 (보통주로 대체하지 않음)")
    void srimSharesEmptyWithoutTotalRow() {
        FinancialTimelineResponse timeline = timeline(List.of(), List.of(), List.of(column("2025", "11011", false)));

        SrimBasis basis = extractor.srimBasis(timeline, "2025", List.of(quantity("보통주", "900")),
                new PeriodicReport(2026, ReportCode.SEMI_ANNUAL));

        assertThat(basis.shares()).isNull();
    }

    private StockQuantityResponse quantity(String category, String distributed) {
        return new StockQuantityResponse(category, null, null, null, null, null, null, null, null, "0",
                distributed, "2026-06-30");
    }

    private Map<String, String> roe(FinancialTimelineResponse timeline) {
        return extractor.ratioRows(timeline, "2025").stream()
                .filter(r -> "roe".equals(r.key())).findFirst()
                .map(RatioRow::values).orElse(Map.of());
    }

    // === fixture ===

    static List<TimelineRow> summary(String netIncome, String totalEquity) {
        return List.of(row(null, "당기순이익", Map.of("2025", netIncome)),
                row(null, "자본총계", Map.of("2025", totalEquity)));
    }

    static FinancialTimelineResponse timeline(List<TimelineDetailGroup> details) {
        return timeline(List.of(), details, List.of(column("2025", "11011", false)));
    }

    static FinancialTimelineResponse timeline(List<TimelineRow> summary, List<TimelineDetailGroup> details,
            List<TimelineColumn> columns) {
        return FinancialTimelineResponse.builder()
                .columns(columns)
                .summaryAccounts(summary)
                .details(details)
                .build();
    }

    static TimelineColumn column(String year, String reportCode, boolean partial) {
        return new TimelineColumn(year, reportCode, partial ? "진행중" : "사업보고서", partial);
    }

    static TimelineDetailGroup group(String statementDiv, List<TimelineRow> rows) {
        List<TimelineDetailNode> nodes = new ArrayList<>();
        rows.forEach(r -> nodes.add(new TimelineDetailNode("ITEM", r, List.of())));
        return new TimelineDetailGroup(statementDiv, statementDiv, nodes);
    }

    static TimelineRow row(String id, String name, Map<String, String> values) {
        return new TimelineRow(id, name, values);
    }

    static Map<String, String> values(List<MetricRow> rows, String key) {
        return rows.stream().filter(r -> key.equals(r.key())).findFirst()
                .map(MetricRow::values).orElse(Map.of());
    }
}
