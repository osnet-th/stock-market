package com.thlee.stock.market.stockmarket.stockevaluation.infrastructure.kis;

import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.kis.exception.KisApiException;
import com.thlee.stock.market.stockmarket.stockevaluation.domain.model.DividendSchedule;
import com.thlee.stock.market.stockmarket.stockevaluation.domain.model.KsdScheduleType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("KisDividendScheduleAdapter — KIS 배당일정 변환과 캐시")
class KisDividendScheduleAdapterTest {

    private static final LocalDate FROM = LocalDate.of(2025, 5, 1);
    private static final LocalDate TO = LocalDate.of(2026, 10, 31);

    @Mock
    KisKsdScheduleClient ksdScheduleClient;

    @InjectMocks
    KisDividendScheduleAdapter adapter;

    @Test
    @DisplayName("A1 지급일과 현금배당금을 날짜·금액으로 바꾸고 순서를 유지한다")
    void convertsPaymentDateAndCashDividend() {
        givenRows(row("20261020", "361"), row("20260420", "1,444"));

        List<DividendSchedule> result = adapter.findCashDividends("005930", FROM, TO);

        assertThat(result).containsExactly(
                new DividendSchedule(LocalDate.of(2026, 10, 20), new BigDecimal("361")),
                new DividendSchedule(LocalDate.of(2026, 4, 20), new BigDecimal("1444")));
    }

    @Test
    @DisplayName("A2 현금배당금이 0·빈값·없음·문자·음수인 행은 뺀다")
    void skipsRowsWithoutPositiveCashDividend() {
        Map<String, String> missingAmount = new HashMap<>();
        missingAmount.put("divi_pay_dt", "20261020");
        givenRows(
                row("20261020", "0"),
                row("20261020", ""),
                missingAmount,
                row("20261020", "abc"),
                row("20261020", "-100"),
                row("20261020", "361"));

        List<DividendSchedule> result = adapter.findCashDividends("005930", FROM, TO);

        assertThat(result).containsExactly(
                new DividendSchedule(LocalDate.of(2026, 10, 20), new BigDecimal("361")));
    }

    @Test
    @DisplayName("A3 지급일은 숫자 8자리만 날짜로 읽고, 나머지는 지급일 없음으로 둔 채 행을 남긴다")
    void readsOnlyEightDigitPaymentDates() {
        givenRows(row("2026/10/20", "361"), row("", "500"), row("20261", "700"));

        List<DividendSchedule> result = adapter.findCashDividends("005930", FROM, TO);

        assertThat(result).containsExactly(
                new DividendSchedule(LocalDate.of(2026, 10, 20), new BigDecimal("361")),
                new DividendSchedule(null, new BigDecimal("500")),
                new DividendSchedule(null, new BigDecimal("700")));
    }

    @Test
    @DisplayName("A4 응답 목록이 null 이면 빈 목록을 돌려준다")
    void returnsEmptyListWhenRowsAreNull() {
        given(ksdScheduleClient.fetch(eq(KsdScheduleType.DIVIDEND), anyString(), anyString(), anyString()))
                .willReturn(null);

        assertThat(adapter.findCashDividends("005930", FROM, TO)).isEmpty();
    }

    @Test
    @DisplayName("A5 같은 종목·기간은 캐시된 결과를 돌려주고, 다른 종목은 새로 조회한다")
    void cachesBySameStockAndPeriod() {
        given(ksdScheduleClient.fetch(eq(KsdScheduleType.DIVIDEND), anyString(), anyString(), anyString()))
                .willReturn(List.of(row("20261020", "361")), List.of(row("20261120", "999")));

        List<DividendSchedule> first = adapter.findCashDividends("005930", FROM, TO);
        List<DividendSchedule> second = adapter.findCashDividends("005930", FROM, TO);
        List<DividendSchedule> other = adapter.findCashDividends("000660", FROM, TO);

        DividendSchedule cached = new DividendSchedule(LocalDate.of(2026, 10, 20), new BigDecimal("361"));
        assertThat(first).containsExactly(cached);
        assertThat(second).containsExactly(cached);
        assertThat(other).containsExactly(
                new DividendSchedule(LocalDate.of(2026, 11, 20), new BigDecimal("999")));
    }

    @Test
    @DisplayName("A6 조회 실패는 그대로 던지고 캐시하지 않아 다음 조회에서 다시 받는다")
    void doesNotCacheFailures() {
        given(ksdScheduleClient.fetch(eq(KsdScheduleType.DIVIDEND), anyString(), anyString(), anyString()))
                .willThrow(new KisApiException("DIVIDEND 조회 실패"))
                .willReturn(List.of(row("20261020", "361")));

        assertThatThrownBy(() -> adapter.findCashDividends("005930", FROM, TO))
                .isInstanceOf(KisApiException.class);
        assertThat(adapter.findCashDividends("005930", FROM, TO)).containsExactly(
                new DividendSchedule(LocalDate.of(2026, 10, 20), new BigDecimal("361")));
    }

    @SafeVarargs
    private void givenRows(Map<String, String>... rows) {
        given(ksdScheduleClient.fetch(eq(KsdScheduleType.DIVIDEND), anyString(), anyString(), anyString()))
                .willReturn(List.of(rows));
    }

    private static Map<String, String> row(String paymentDate, String cashDividend) {
        return Map.of("divi_pay_dt", paymentDate, "per_sto_divi_amt", cashDividend);
    }
}
