package com.thlee.stock.market.stockmarket.stock.application;

import com.thlee.stock.market.stockmarket.stock.application.dto.FinancialAccountResponse;
import com.thlee.stock.market.stockmarket.stock.application.dto.StockQuantityResponse;
import com.thlee.stock.market.stockmarket.stock.application.dto.ValuationMetricResponse;
import com.thlee.stock.market.stockmarket.stock.domain.model.Disclosure;
import com.thlee.stock.market.stockmarket.stock.domain.service.StockFinancialPort;
import com.thlee.stock.market.stockmarket.stock.domain.service.StockPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
@DisplayName("ValuationMetricService — 유통주식수 사업·반기 기준")
class ValuationMetricServiceTest {

    private static final String CODE = "005930";
    private static final String YEAR = String.valueOf(LocalDate.now().getYear());

    @Mock StockFinancialService stockFinancialService;
    @Mock StockPriceService stockPriceService;
    @Mock StockPort stockPort;
    @Mock StockFinancialPort stockFinancialPort;

    ValuationMetricService service;

    @BeforeEach
    void setUp() {
        service = new ValuationMetricService(stockFinancialService, stockPriceService, stockPort,
                new ShareReportSelector(stockFinancialPort));
        given(stockFinancialService.getFinancialAccounts(eq(CODE), anyString(), eq("11011"))).willReturn(List.of(
                account("당기순이익", "9000"), account("자본총계", "18000")));
        given(stockPort.findByCode(CODE)).willReturn(Optional.empty());
        lenient().when(stockFinancialService.getStockQuantities(CODE, YEAR, "11011"))
                .thenReturn(List.of(quantity("1000", YEAR + "-12-31")));
        lenient().when(stockFinancialService.getStockQuantities(CODE, YEAR, "11012"))
                .thenReturn(List.of(quantity("900", YEAR + "-06-30")));
    }

    @Test
    @DisplayName("D1 반기보고서가 있으면 EPS·BPS를 반기 유통주식수로 계산한다")
    void usesSemiAnnualShares() {
        int year = Integer.parseInt(YEAR);
        given(stockFinancialPort.getDisclosures(eq(CODE), anyString(), anyString(), eq("A"))).willReturn(List.of(
                disclosure("반기보고서 (" + year + ".06)"), disclosure("사업보고서 (" + (year - 1) + ".12)")));

        ValuationMetricResponse response = service.calculate(CODE);

        assertThat(response.getEps()).isEqualByComparingTo("10.00");
        assertThat(response.getBps()).isEqualByComparingTo("20.00");
    }

    @Test
    @DisplayName("D2 공시 조회가 실패하면 사업보고서 유통주식수로 기존과 같은 결과를 낸다")
    void fallsBackToAnnualShares() {
        given(stockFinancialPort.getDisclosures(eq(CODE), anyString(), anyString(), eq("A")))
                .willThrow(new IllegalStateException("DART 장애"));

        ValuationMetricResponse response = service.calculate(CODE);

        assertThat(response.getEps()).isEqualByComparingTo("9.00");
        assertThat(response.getBps()).isEqualByComparingTo("18.00");
    }

    private FinancialAccountResponse account(String name, String amount) {
        return new FinancialAccountResponse(CODE, name, "CFS", "연결재무제표", "BS", "재무상태표",
                "제57기", amount, null, null, null, null, "KRW");
    }

    private StockQuantityResponse quantity(String distributed, String settlementDate) {
        return new StockQuantityResponse("보통주", null, null, null, null, null, null, null, null, "0",
                distributed, settlementDate);
    }

    private Disclosure disclosure(String reportName) {
        return new Disclosure("r", reportName, "삼성전자", "20260814", null);
    }
}
