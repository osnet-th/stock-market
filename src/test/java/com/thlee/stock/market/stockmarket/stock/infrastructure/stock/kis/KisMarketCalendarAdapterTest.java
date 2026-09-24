package com.thlee.stock.market.stockmarket.stock.infrastructure.stock.kis;

import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.kis.dto.KisHolidayOutput;
import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.kis.exception.KisApiException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("KisMarketCalendarAdapter — 국내 개장일 판정")
class KisMarketCalendarAdapterTest {

    private static final LocalDate TARGET = LocalDate.of(2026, 9, 15);

    @Mock
    KisHolidayClient holidayClient;

    @InjectMocks
    KisMarketCalendarAdapter adapter;

    @Test
    @DisplayName("C1 개장일 여부가 Y 이면 개장일로 본다")
    void returnsTrueWhenOpenDayYnIsY() {
        given(holidayClient.fetchHolidays(TARGET)).willReturn(List.of(holiday("20260915", "Y")));

        assertThat(adapter.isOpen(TARGET)).isTrue();
    }

    @Test
    @DisplayName("C2 개장일 여부가 N 이면 휴장일로 본다")
    void returnsFalseWhenOpenDayYnIsN() {
        given(holidayClient.fetchHolidays(TARGET)).willReturn(List.of(holiday("20260915", "N")));

        assertThat(adapter.isOpen(TARGET)).isFalse();
    }

    @Test
    @DisplayName("C3 여러 날짜가 함께 와도 대상 일자 항목으로 판정한다")
    void picksTargetDateAmongMultipleEntries() {
        given(holidayClient.fetchHolidays(TARGET)).willReturn(List.of(
                holiday("20260915", "N"),
                holiday("20260916", "Y"),
                holiday("20260917", "Y")));

        assertThat(adapter.isOpen(TARGET)).isFalse();
    }

    @Test
    @DisplayName("C4 응답에 대상 일자가 없으면 휴장이 아니라 판별 실패로 본다")
    void throwsWhenTargetDateMissing() {
        given(holidayClient.fetchHolidays(TARGET)).willReturn(List.of(
                holiday("20260916", "Y"),
                holiday("20260917", "Y")));

        assertThatThrownBy(() -> adapter.isOpen(TARGET))
                .isInstanceOf(KisApiException.class);
    }

    @Test
    @DisplayName("C5 응답이 비었거나 null 이면 판별 실패로 본다")
    void throwsWhenResponseEmptyOrNull() {
        given(holidayClient.fetchHolidays(TARGET)).willReturn(List.of());
        assertThatThrownBy(() -> adapter.isOpen(TARGET))
                .isInstanceOf(KisApiException.class);

        given(holidayClient.fetchHolidays(TARGET)).willReturn(null);
        assertThatThrownBy(() -> adapter.isOpen(TARGET))
                .isInstanceOf(KisApiException.class);
    }

    @Test
    @DisplayName("C6 개장일 여부가 Y 도 N 도 아니면 휴장이 아니라 판별 실패로 본다")
    void throwsWhenOpenDayYnUnknown() {
        given(holidayClient.fetchHolidays(TARGET)).willReturn(List.of(holiday("20260915", "y")));

        assertThatThrownBy(() -> adapter.isOpen(TARGET))
                .isInstanceOf(KisApiException.class);
    }

    private KisHolidayOutput holiday(String baseDate, String openDayYn) {
        return new KisHolidayOutput(baseDate, "Y", "Y", openDayYn, "Y");
    }
}
