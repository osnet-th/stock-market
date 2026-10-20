package com.thlee.stock.market.stockmarket.mcp.application;

import com.thlee.stock.market.stockmarket.companyreport.application.dto.ReportSnapshot;
import com.thlee.stock.market.stockmarket.mcp.application.dto.McpCompanyReportSummary;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class McpCompanyReportMapperTest {

    @Test
    @DisplayName("M1: 요약은 최근 5개 연도 비율만 담고 기준연도·주가 기준일·통화·단위를 담는다")
    void recentFiveYears() {
        // given: 2017~2026 (2026은 반기)
        List<ReportSnapshot.ColumnMeta> columns = IntStream.rangeClosed(2017, 2026)
                .mapToObj(y -> new ReportSnapshot.ColumnMeta(String.valueOf(y), y == 2026 ? "반기보고서" : "사업보고서",
                        y == 2026))
                .toList();
        Map<String, String> roe = new LinkedHashMap<>();
        IntStream.rangeClosed(2017, 2026).forEach(y -> roe.put(String.valueOf(y), String.valueOf(y - 2010)));
        ReportSnapshot.PriceMetrics price = new ReportSnapshot.PriceMetrics("2026 반기", "2026-10-17",
                new BigDecimal("30000"), new BigDecimal("1500000000000"), null, null, new BigDecimal("12.3"),
                null, null, null, null, null, null, null, List.of(), Map.of());
        ReportSnapshot snapshot = new ReportSnapshot(3, "005930", "테스트전자", "KR", "KRW", List.of(), "CFS", null,
                columns, List.of(), List.of(),
                List.of(new ReportSnapshot.RatioRow("roe", "ROE(%)", "profitability", roe, "good")),
                price, null, null, null, "TOTAL", null);

        // when
        McpCompanyReportSummary summary = McpCompanyReportMapper.toSummary(snapshot, null);

        // then
        assertThat(summary.columns()).extracting(McpCompanyReportSummary.Column::year)
                .containsExactly("2022", "2023", "2024", "2025", "2026");
        assertThat(summary.ratios().get(0).values()).containsOnlyKeys("2022", "2023", "2024", "2025", "2026");
        assertThat(summary.baseYear()).isEqualTo("2025");
        assertThat(summary.currency()).isEqualTo("KRW");
        assertThat(summary.ratioUnit()).isEqualTo("%");
        assertThat(summary.priceMetrics().referencePriceDate()).isEqualTo("2026-10-17");
        assertThat(summary.priceMetrics().per()).isEqualByComparingTo("12.3");
        assertThat(summary.srimAvailable()).isFalse();
    }
}
