package com.thlee.stock.market.stockmarket.mcp.application;

import com.thlee.stock.market.stockmarket.companyreport.application.dto.ReportSnapshot;
import com.thlee.stock.market.stockmarket.companyreport.application.dto.SrimInputData;
import com.thlee.stock.market.stockmarket.economics.domain.model.BondCreditGrade;
import com.thlee.stock.market.stockmarket.economics.domain.model.BondYield;
import com.thlee.stock.market.stockmarket.economics.domain.model.BondYieldLookup;
import com.thlee.stock.market.stockmarket.economics.domain.model.BondYieldSnapshot;
import com.thlee.stock.market.stockmarket.economics.domain.model.BondYieldType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class McpSrimInputAssemblerTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 20);
    private final McpSrimInputAssembler assembler = new McpSrimInputAssembler();

    @Test
    @DisplayName("S1: 근거·요구수익률·ROE를 주면 입력이 채워지고 비율은 소수, 연도 행은 올해 1개 DIRECT다")
    void fullInput() {
        // given
        ReportSnapshot snapshot = snapshot(basis(), roeRow(Map.of("2024", "12.5", "2025", "15.0")));

        // when
        McpSrimInputAssembler.Assembly result = assembler.assemble(
                snapshot, new BigDecimal("8"), new BigDecimal("10"), bondYields(true), 2026);

        // then
        SrimInputData input = result.input();
        assertThat(result.failureReason()).isNull();
        assertThat(input.equity()).isEqualByComparingTo("1000000000000");
        assertThat(input.equityDate()).isEqualTo("2026-06-30");
        assertThat(input.shares()).isEqualByComparingTo("50000000");
        assertThat(input.sharesDate()).isEqualTo("2026-06-30");
        assertThat(input.referencePrice()).isEqualByComparingTo("30000");
        assertThat(input.referencePriceDate()).isEqualTo("2026-10-17");
        assertThat(input.currency()).isEqualTo("KRW");
        assertThat(input.requiredReturn()).isEqualByComparingTo("0.08");
        assertThat(input.years()).hasSize(1);
        assertThat(input.years().get(0).year()).isEqualTo(2026);
        assertThat(input.years().get(0).mode()).isEqualTo("DIRECT");
        assertThat(input.years().get(0).directRoe()).isEqualByComparingTo("0.10");
        assertThat(result.requiredReturnSource()).isEqualTo("입력값");
        assertThat(result.roeSource()).isEqualTo("입력값");
    }

    @Test
    @DisplayName("S2: 요구수익률이 없으면 회사채 BBB- 5년 금리와 그 항목·기준일을 쓴다")
    void defaultRequiredReturn() {
        // given
        ReportSnapshot snapshot = snapshot(basis(), roeRow(Map.of("2025", "15.0")));

        // when
        McpSrimInputAssembler.Assembly result = assembler.assemble(
                snapshot, null, new BigDecimal("10"), bondYields(true), 2026);

        // then
        assertThat(result.requiredReturnPercent()).isEqualByComparingTo("9.75");
        assertThat(result.input().requiredReturn()).isEqualByComparingTo("0.0975");
        assertThat(result.requiredReturnSource()).contains("BBB- 5년").contains("2026-10-20");
    }

    @Test
    @DisplayName("S3: ROE가 없으면 리포트의 최근 연간 ROE를 쓴다(부분 연도 제외)")
    void defaultRoe() {
        // given: 2026은 반기(부분 연도)라 2025를 쓴다
        ReportSnapshot snapshot = snapshot(basis(), roeRow(Map.of("2024", "12.5", "2025", "15.0", "2026", "7.0")));

        // when
        McpSrimInputAssembler.Assembly result = assembler.assemble(
                snapshot, new BigDecimal("8"), null, bondYields(true), 2026);

        // then
        assertThat(result.roePercent()).isEqualByComparingTo("15.0");
        assertThat(result.input().years().get(0).directRoe()).isEqualByComparingTo("0.15");
        assertThat(result.roeSource()).contains("2025");
    }

    @Test
    @DisplayName("S4: S-RIM 근거가 없으면 계산하지 않고 사유를 돌려준다")
    void noBasis() {
        // given
        ReportSnapshot snapshot = snapshot(null, roeRow(Map.of("2025", "15.0")));

        // when
        McpSrimInputAssembler.Assembly result = assembler.assemble(
                snapshot, new BigDecimal("8"), new BigDecimal("10"), bondYields(true), 2026);

        // then
        assertThat(result.input()).isNull();
        assertThat(result.failureReason()).contains("자동 계산 불가");
    }

    @Test
    @DisplayName("S5: BBB- 5년 금리가 없으면 요구수익률 입력을 요구하는 사유를 돌려준다")
    void noDefaultRate() {
        // given
        ReportSnapshot snapshot = snapshot(basis(), roeRow(Map.of("2025", "15.0")));

        // when
        McpSrimInputAssembler.Assembly result = assembler.assemble(
                snapshot, null, new BigDecimal("10"), bondYields(false), 2026);

        // then
        assertThat(result.input()).isNull();
        assertThat(result.failureReason()).contains("requiredReturnPercent");
    }

    @Test
    @DisplayName("S6: ROE 자료가 없으면 ROE 입력을 요구하는 사유를 돌려준다")
    void noRoe() {
        // given
        ReportSnapshot snapshot = snapshot(basis(), null);

        // when
        McpSrimInputAssembler.Assembly result = assembler.assemble(
                snapshot, new BigDecimal("8"), null, bondYields(true), 2026);

        // then
        assertThat(result.input()).isNull();
        assertThat(result.failureReason()).contains("roePercent");
    }

    private static ReportSnapshot.SrimBasis basis() {
        return new ReportSnapshot.SrimBasis("1000000000000", "2026-06-30", "2026 반기보고서", Map.of(),
                "50000000", "2026-06-30", "2026 반기보고서", "합계");
    }

    private static ReportSnapshot.RatioRow roeRow(Map<String, String> values) {
        return values == null ? null
                : new ReportSnapshot.RatioRow("roe", "ROE(%)", "profitability", new LinkedHashMap<>(values), "good");
    }

    private static ReportSnapshot snapshot(ReportSnapshot.SrimBasis basis, ReportSnapshot.RatioRow roe) {
        List<ReportSnapshot.ColumnMeta> columns = List.of(
                new ReportSnapshot.ColumnMeta("2024", "사업보고서", false),
                new ReportSnapshot.ColumnMeta("2025", "사업보고서", false),
                new ReportSnapshot.ColumnMeta("2026", "반기보고서", true));
        ReportSnapshot.PriceMetrics price = new ReportSnapshot.PriceMetrics("2026 반기", "2026-10-17",
                new BigDecimal("30000"), null, null, null, null, null, null, null, null, null, null, null,
                List.of(), Map.of());
        return new ReportSnapshot(3, "005930", "테스트전자", "KR", "KRW", List.of(), "CFS", null, columns,
                List.of(), List.of(), roe == null ? List.of() : List.of(roe), price, null, null, null, "TOTAL", basis);
    }

    private static BondYieldLookup bondYields(boolean withBbbMinus5y) {
        List<BondYield> yields = withBbbMinus5y
                ? List.of(new BondYield(BondYieldType.TREASURY, null, 60, new BigDecimal("3.985")),
                        new BondYield(BondYieldType.CORPORATE_PUBLIC_UNSECURED, BondCreditGrade.BBB_MINUS, 60,
                                new BigDecimal("9.75")))
                : List.of(new BondYield(BondYieldType.TREASURY, null, 60, new BigDecimal("3.985")));
        return BondYieldLookup.applied(TODAY, "한국자산평가", new BondYieldSnapshot(TODAY, yields));
    }
}
