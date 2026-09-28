package com.thlee.stock.market.stockmarket.companyreport.application;

import com.thlee.stock.market.stockmarket.companyreport.application.dto.ReportSnapshot;
import com.thlee.stock.market.stockmarket.stock.application.CompanyInfoService;
import com.thlee.stock.market.stockmarket.stock.application.DisclosureQueryService;
import com.thlee.stock.market.stockmarket.stock.application.FinancialTimelineService;
import com.thlee.stock.market.stockmarket.stock.application.ShareReportSelector;
import com.thlee.stock.market.stockmarket.stock.application.StockFinancialService;
import com.thlee.stock.market.stockmarket.stock.application.ValuationMetricService;
import com.thlee.stock.market.stockmarket.stock.application.dto.FinancialTimelineResponse;
import com.thlee.stock.market.stockmarket.stock.application.dto.StockQuantityResponse;
import com.thlee.stock.market.stockmarket.stock.application.dto.ValuationMetricResponse;
import com.thlee.stock.market.stockmarket.stock.domain.model.Disclosure;
import com.thlee.stock.market.stockmarket.stock.domain.service.StockFinancialPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
@DisplayName("KrReportSnapshotAssembler — 주식총수 사업·반기 기준")
class KrReportSnapshotAssemblerTest {

    private static final String CODE = "005930";

    @Mock FinancialTimelineService timelineService;
    @Mock StockFinancialService stockFinancialService;
    @Mock ValuationMetricService valuationMetricService;
    @Mock CompanyInfoService companyInfoService;
    @Mock DisclosureQueryService disclosureQueryService;
    @Mock StockFinancialPort stockFinancialPort;

    KrReportSnapshotAssembler assembler;

    @BeforeEach
    void setUp() {
        assembler = new KrReportSnapshotAssembler(timelineService, stockFinancialService, valuationMetricService,
                companyInfoService, disclosureQueryService, new SnapshotFinancialExtractor(),
                new ShareReportSelector(stockFinancialPort), Runnable::run);
        given(timelineService.getTimeline(eq(CODE), anyInt(), eq("CFS"), any())).willReturn(annualTimeline());
        given(valuationMetricService.calculate(CODE)).willReturn(new ValuationMetricResponse(
                "2025", null, null, null, null, "2026-09-25", new BigDecimal("100"), List.of()));
        lenient().when(stockFinancialService.getStockQuantities(CODE, "2025", "11011"))
                .thenReturn(List.of(quantity("보통주", "1000", "2025-12-31")));
        lenient().when(stockFinancialService.getStockQuantities(CODE, "2026", "11012"))
                .thenReturn(List.of(quantity("보통주", "900", "2026-06-30"), quantity("합계", "950", "2026-06-30")));
    }

    @Test
    @DisplayName("C1 공시 목록에 당해 반기보고서가 있으면 반기 주식수로 시가총액을 계산하고 schemaVersion 3을 쓴다")
    void usesSemiAnnualShares() {
        given(stockFinancialPort.getDisclosures(eq(CODE), anyString(), anyString(), eq("A"))).willReturn(List.of(
                disclosure("반기보고서 (2026.06)"), disclosure("사업보고서 (2025.12)")));

        ReportSnapshot snapshot = assembler.assemble(CODE);

        assertThat(snapshot.priceMetrics().marketCap()).isEqualByComparingTo("90000");
        assertThat(snapshot.srimBasis().sharesDate()).isEqualTo("2026-06-30");
        assertThat(snapshot.srimBasis().sharesReport()).isEqualTo("2026 반기보고서");
        assertThat(snapshot.schemaVersion()).isEqualTo(3);
    }

    @Test
    @DisplayName("C2 공시 목록 조회가 실패하면 기준연도 사업보고서 주식수로 폴백하고 스냅샷은 정상 생성된다")
    void fallsBackToAnnualWhenDisclosureLookupFails() {
        given(stockFinancialPort.getDisclosures(eq(CODE), anyString(), anyString(), eq("A")))
                .willThrow(new IllegalStateException("DART 장애"));

        ReportSnapshot snapshot = assembler.assemble(CODE);

        assertThat(snapshot.priceMetrics().marketCap()).isEqualByComparingTo("100000");
        assertThat(snapshot.stockCode()).isEqualTo(CODE);
    }

    private FinancialTimelineResponse annualTimeline() {
        return FinancialTimelineResponse.builder()
                .columns(List.of(new FinancialTimelineResponse.TimelineColumn("2025", "11011", "사업보고서", false)))
                .summaryAccounts(List.of(new FinancialTimelineResponse.TimelineRow(null, "매출액", Map.of("2025", "5000"))))
                .details(List.of())
                .build();
    }

    private StockQuantityResponse quantity(String category, String distributed, String settlementDate) {
        return new StockQuantityResponse(category, null, null, null, null, null, null, null, null, "0",
                distributed, settlementDate);
    }

    private Disclosure disclosure(String reportName) {
        return new Disclosure("r", reportName, "삼성전자", "20260814", null);
    }
}
