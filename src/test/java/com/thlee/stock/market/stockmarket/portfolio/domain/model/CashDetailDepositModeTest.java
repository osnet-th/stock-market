package com.thlee.stock.market.stockmarket.portfolio.domain.model;

import com.thlee.stock.market.stockmarket.portfolio.domain.model.enums.CashSubType;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.enums.DepositMode;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.enums.TaxType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CashDetail — 자동납입 처리 방식")
class CashDetailDepositModeTest {

    private static final BigDecimal MONTHLY = BigDecimal.valueOf(300_000);
    private static final String INVALID_AUTO_MESSAGE = "자동 납입은 월 납입액과 납입일(1~31일)을 입력해야 합니다.";

    @Test
    @DisplayName("D1 처리 방식이 없으면 알림 확인이다")
    void defaultsToNotify() {
        CashDetail fiveArgs = new CashDetail(CashSubType.SAVINGS, BigDecimal.valueOf(3), null, null, TaxType.GENERAL);
        CashDetail sevenArgs = new CashDetail(CashSubType.SAVINGS, BigDecimal.valueOf(3), null, null, TaxType.GENERAL,
                MONTHLY, 25);
        CashDetail nullMode = savings(MONTHLY, 25, null, null, null);

        assertThat(fiveArgs.getDepositMode()).isEqualTo(DepositMode.NOTIFY);
        assertThat(sevenArgs.getDepositMode()).isEqualTo(DepositMode.NOTIFY);
        assertThat(nullMode.getDepositMode()).isEqualTo(DepositMode.NOTIFY);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "NOTIFY"})
    @DisplayName("D2 빈 값과 NOTIFY는 알림 확인으로 바꾼다")
    void from_blankOrNotify_isNotify(String value) {
        assertThat(DepositMode.from(value)).isEqualTo(DepositMode.NOTIFY);
    }

    @Test
    @DisplayName("D2 AUTO는 자동 반영으로 바꾸고, 모르는 값은 거부한다")
    void from_autoAndUnknown() {
        assertThat(DepositMode.from("AUTO")).isEqualTo(DepositMode.AUTO);
        assertThatThrownBy(() -> DepositMode.from("WRONG"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("납입 처리 방식이 올바르지 않습니다: WRONG");
    }

    @Test
    @DisplayName("D3 자동 반영은 월 납입액·납입일이 있으면 통과하고, 알림 확인은 없어도 통과한다")
    void validate_passes() {
        assertThatCode(() -> savings(MONTHLY, 25, DepositMode.AUTO, null, null).validateDepositMode())
                .doesNotThrowAnyException();
        assertThatCode(() -> savings(null, null, DepositMode.NOTIFY, null, null).validateDepositMode())
                .doesNotThrowAnyException();
    }

    @ParameterizedTest(name = "[{index}] 월 납입액={0}, 납입일={1}")
    @MethodSource("invalidAutoSettings")
    @DisplayName("D4 자동 반영인데 월 납입액이나 납입일이 없거나 범위를 벗어나면 거부한다")
    void validate_rejectsInvalidAuto(BigDecimal monthly, Integer day) {
        CashDetail detail = savings(monthly, day, DepositMode.AUTO, null, null);

        assertThatThrownBy(detail::validateDepositMode)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(INVALID_AUTO_MESSAGE);
    }

    static Stream<Arguments> invalidAutoSettings() {
        return Stream.of(
                Arguments.of(null, 25),
                Arguments.of(BigDecimal.ZERO, 25),
                Arguments.of(MONTHLY, null),
                Arguments.of(MONTHLY, 0),
                Arguments.of(MONTHLY, 32));
    }

    @Test
    @DisplayName("D5 자동 반영 항목은 납입일에만 대상이고, 알림 확인 항목은 대상이 아니다")
    void dueOnlyOnDepositDay() {
        CashDetail auto = savings(MONTHLY, 25, DepositMode.AUTO, null, null);
        CashDetail notify = savings(MONTHLY, 25, DepositMode.NOTIFY, null, null);

        assertThat(auto.isAutoDepositDueOn(LocalDate.of(2026, 10, 25))).isTrue();
        assertThat(auto.isAutoDepositDueOn(LocalDate.of(2026, 10, 24))).isFalse();
        assertThat(notify.isAutoDepositDueOn(LocalDate.of(2026, 10, 25))).isFalse();
    }

    @Test
    @DisplayName("D6 납입일이 그달 일수보다 크면 말일에 대상이다")
    void clampsToMonthEnd() {
        CashDetail auto = savings(MONTHLY, 31, DepositMode.AUTO, null, null);

        assertThat(auto.isAutoDepositDueOn(LocalDate.of(2026, 2, 28))).isTrue();
        assertThat(auto.isAutoDepositDueOn(LocalDate.of(2026, 2, 27))).isFalse();
        assertThat(auto.isAutoDepositDueOn(LocalDate.of(2026, 4, 30))).isTrue();
    }

    @ParameterizedTest(name = "[{index}] 시작일={0}, 만기일={1} → {2}")
    @MethodSource("periods")
    @DisplayName("D7 시작일 전과 만기일 당일 이후에는 대상이 아니다")
    void respectsPeriod(LocalDate startDate, LocalDate maturityDate, boolean expected) {
        CashDetail auto = savings(MONTHLY, 25, DepositMode.AUTO, startDate, maturityDate);

        assertThat(auto.isAutoDepositDueOn(LocalDate.of(2026, 10, 25))).isEqualTo(expected);
    }

    static Stream<Arguments> periods() {
        return Stream.of(
                Arguments.of(LocalDate.of(2026, 9, 25), null, true),
                Arguments.of(LocalDate.of(2026, 11, 1), null, false),
                Arguments.of(null, LocalDate.of(2026, 10, 25), false),
                Arguments.of(null, LocalDate.of(2026, 10, 20), false),
                Arguments.of(null, LocalDate.of(2026, 10, 26), true),
                Arguments.of(null, null, true));
    }

    @Test
    @DisplayName("D8 시작일이 속한 달에는 대상이 아니고, 다음 달 납입일부터 대상이다")
    void skipsStartMonth() {
        CashDetail startedEarlyInMonth = savings(MONTHLY, 25, DepositMode.AUTO, LocalDate.of(2026, 10, 5), null);
        CashDetail startedOnDepositDay = savings(MONTHLY, 25, DepositMode.AUTO, LocalDate.of(2026, 10, 25), null);

        assertThat(startedEarlyInMonth.isAutoDepositDueOn(LocalDate.of(2026, 10, 25))).isFalse();
        assertThat(startedEarlyInMonth.isAutoDepositDueOn(LocalDate.of(2026, 11, 25))).isTrue();
        assertThat(startedOnDepositDay.isAutoDepositDueOn(LocalDate.of(2026, 10, 25))).isFalse();
    }

    private static CashDetail savings(BigDecimal monthly, Integer day, DepositMode mode,
                                      LocalDate startDate, LocalDate maturityDate) {
        return new CashDetail(CashSubType.SAVINGS, BigDecimal.valueOf(3), startDate, maturityDate,
                TaxType.GENERAL, monthly, day, mode);
    }
}
