package com.thlee.stock.market.stockmarket.portfolio.domain.model;

import com.thlee.stock.market.stockmarket.portfolio.domain.model.enums.CashSubType;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.enums.DepositMode;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.enums.TaxType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CashDetail — 만기 판단")
class CashDetailMaturityTest {

    private static final BigDecimal MONTHLY = BigDecimal.valueOf(300_000);
    private static final int DEPOSIT_DAY = 25;

    @Test
    @DisplayName("E1 만기일 전날은 만기가 아니고, 만기일 당일과 다음 날은 만기다")
    void maturedFromMaturityDate() {
        // given
        CashDetail savings = cash(CashSubType.SAVINGS, LocalDate.of(2026, 10, 25));

        // when, then
        assertThat(savings.isMaturedOn(LocalDate.of(2026, 10, 24))).isFalse();
        assertThat(savings.isMaturedOn(LocalDate.of(2026, 10, 25))).isTrue();
        assertThat(savings.isMaturedOn(LocalDate.of(2026, 10, 26))).isTrue();
    }

    @Test
    @DisplayName("E2 만기일이 없으면 만기가 아니다")
    void notMaturedWithoutMaturityDate() {
        // given
        CashDetail savings = cash(CashSubType.SAVINGS, null);
        CashDetail cma = cash(CashSubType.CMA, null);

        // when, then
        assertThat(savings.isMaturedOn(LocalDate.of(2026, 10, 25))).isFalse();
        assertThat(cma.isMaturedOn(LocalDate.of(2026, 10, 25))).isFalse();
    }

    private static CashDetail cash(CashSubType subType, LocalDate maturityDate) {
        return new CashDetail(subType, BigDecimal.valueOf(3), LocalDate.of(2026, 1, 1), maturityDate, TaxType.GENERAL,
                MONTHLY, DEPOSIT_DAY, DepositMode.NOTIFY);
    }
}
