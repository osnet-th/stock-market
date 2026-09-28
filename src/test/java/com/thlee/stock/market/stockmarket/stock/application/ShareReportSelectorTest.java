package com.thlee.stock.market.stockmarket.stock.application;

import com.thlee.stock.market.stockmarket.stock.domain.model.PeriodicReport;
import com.thlee.stock.market.stockmarket.stock.domain.model.ReportCode;
import com.thlee.stock.market.stockmarket.stock.domain.service.StockFinancialPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@DisplayName("ShareReportSelector — 주식총수 기준 보고서(사업·반기) 선택")
class ShareReportSelectorTest {

    private final ShareReportSelector selector = new ShareReportSelector(mock(StockFinancialPort.class));

    @Test
    @DisplayName("B1 당해 반기와 전년 사업보고서가 있으면 당해 반기를 고른다")
    void picksCurrentSemiAnnual() {
        PeriodicReport selected = selector.select(List.of(
                new PeriodicReport(2026, ReportCode.SEMI_ANNUAL), new PeriodicReport(2025, ReportCode.ANNUAL)), "2025");

        assertThat(selected).isEqualTo(new PeriodicReport(2026, ReportCode.SEMI_ANNUAL));
    }

    @Test
    @DisplayName("B2 당해 1분기만 있는 구간에서는 분기를 제외하고 전년 사업보고서를 고른다")
    void excludesQuarterly() {
        PeriodicReport selected = selector.select(List.of(
                new PeriodicReport(2026, ReportCode.Q1), new PeriodicReport(2025, ReportCode.ANNUAL)), "2025");

        assertThat(selected).isEqualTo(new PeriodicReport(2025, ReportCode.ANNUAL));
    }

    @Test
    @DisplayName("B3 연도와 종료월을 쌍으로 비교해 전년 반기가 전전년 사업보고서보다 우선한다")
    void comparesYearAndMonthAsPair() {
        PeriodicReport selected = selector.select(List.of(
                new PeriodicReport(2025, ReportCode.Q3), new PeriodicReport(2025, ReportCode.SEMI_ANNUAL),
                new PeriodicReport(2024, ReportCode.ANNUAL)), "2024");

        assertThat(selected).isEqualTo(new PeriodicReport(2025, ReportCode.SEMI_ANNUAL));
    }

    @Test
    @DisplayName("B4 후보가 없으면 기준연도 사업보고서로 폴백한다")
    void fallsBackToAnnualOfBaseYear() {
        PeriodicReport selected = selector.select(List.of(), "2025");

        assertThat(selected).isEqualTo(new PeriodicReport(2025, ReportCode.ANNUAL));
    }
}
