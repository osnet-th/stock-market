package com.thlee.stock.market.stockmarket.companyreport.application;

import com.thlee.stock.market.stockmarket.companyreport.application.dto.ReportSnapshot.MetricRow;
import com.thlee.stock.market.stockmarket.stock.application.dto.FinancialTimelineResponse;
import com.thlee.stock.market.stockmarket.stock.application.dto.FinancialTimelineResponse.TimelineColumn;
import com.thlee.stock.market.stockmarket.stock.application.dto.FinancialTimelineResponse.TimelineDetailGroup;
import com.thlee.stock.market.stockmarket.stock.application.dto.FinancialTimelineResponse.TimelineDetailNode;
import com.thlee.stock.market.stockmarket.stock.application.dto.FinancialTimelineResponse.TimelineRow;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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

    // === fixture ===

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
