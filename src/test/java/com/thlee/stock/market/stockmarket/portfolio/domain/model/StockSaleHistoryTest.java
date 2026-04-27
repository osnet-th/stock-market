package com.thlee.stock.market.stockmarket.portfolio.domain.model;

import com.thlee.stock.market.stockmarket.portfolio.domain.model.enums.SaleReason;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("StockSaleHistory 도메인 모델")
class StockSaleHistoryTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 4, 27);
    private static final Long ITEM_ID = 100L;

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("KRW 매도: 모든 입력이 유효하면 파생값(profit/profitRate/contributionRate)을 자동 계산한다")
        void create_krw_computesDerivedValues() {
            StockSaleHistory history = StockSaleHistory.create(
                    ITEM_ID, 10,
                    BigDecimal.valueOf(70_000),     // avgBuyPrice
                    BigDecimal.valueOf(80_000),     // salePrice
                    "KRW",
                    BigDecimal.ONE,                 // fxRate (KRW면 1)
                    BigDecimal.valueOf(10_000_000), // totalAssetAtSale
                    SaleReason.TARGET_PRICE_REACHED,
                    "익절",
                    "005930", "삼성전자",
                    false,
                    TODAY, TODAY
            );

            // profit = (80000 - 70000) * 10 = 100,000
            assertThat(history.getProfit()).isEqualByComparingTo(BigDecimal.valueOf(100_000));
            // profitRate = 100,000 / 700,000 * 100 = 14.29
            assertThat(history.getProfitRate()).isEqualByComparingTo(new BigDecimal("14.29"));
            // KRW이므로 salePriceKrw = 800,000, profitKrw = 100,000
            assertThat(history.getSalePriceKrw()).isEqualByComparingTo(BigDecimal.valueOf(800_000));
            assertThat(history.getProfitKrw()).isEqualByComparingTo(BigDecimal.valueOf(100_000));
            // contributionRate = 100,000 / 10,000,000 * 100 = 1.00
            assertThat(history.getContributionRate()).isEqualByComparingTo(new BigDecimal("1.00"));
            assertThat(history.isUnrecordedDeposit()).isFalse();
        }

        @Test
        @DisplayName("외화 매도: fxRate가 적용된 KRW 환산값과 contributionRate를 계산한다")
        void create_foreign_computesKrwValues() {
            StockSaleHistory history = StockSaleHistory.create(
                    ITEM_ID, 5,
                    BigDecimal.valueOf(150),       // avgBuyPrice (USD)
                    BigDecimal.valueOf(180),       // salePrice (USD)
                    "USD",
                    BigDecimal.valueOf(1_400),     // fxRate (1 USD = 1,400 KRW)
                    BigDecimal.valueOf(20_000_000),
                    SaleReason.REBALANCING,
                    null,
                    "AAPL", "Apple Inc.",
                    false,
                    TODAY, TODAY
            );

            // profit (USD) = (180 - 150) * 5 = 150
            assertThat(history.getProfit()).isEqualByComparingTo(BigDecimal.valueOf(150));
            // salePriceKrw = 180 * 1400 * 5 = 1,260,000
            assertThat(history.getSalePriceKrw()).isEqualByComparingTo(BigDecimal.valueOf(1_260_000));
            // profitKrw = 150 * 1400 = 210,000
            assertThat(history.getProfitKrw()).isEqualByComparingTo(BigDecimal.valueOf(210_000));
            // contributionRate = 210,000 / 20,000,000 * 100 = 1.05
            assertThat(history.getContributionRate()).isEqualByComparingTo(new BigDecimal("1.05"));
        }

        @Test
        @DisplayName("외화 매도 + fxRate=null이면 KRW 환산값은 null, contributionRate=0")
        void create_foreign_nullFxRate_skipsKrw() {
            StockSaleHistory history = StockSaleHistory.create(
                    ITEM_ID, 5,
                    BigDecimal.valueOf(150),
                    BigDecimal.valueOf(180),
                    "USD",
                    null,
                    BigDecimal.valueOf(20_000_000),
                    SaleReason.OTHER,
                    null,
                    "AAPL", "Apple Inc.",
                    true,
                    TODAY, TODAY
            );

            assertThat(history.getSalePriceKrw()).isNull();
            assertThat(history.getProfitKrw()).isNull();
            assertThat(history.getContributionRate()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(history.isUnrecordedDeposit()).isTrue();
        }

        @Test
        @DisplayName("totalAssetAtSale=0이면 contributionRate는 0으로 계산된다")
        void create_zeroTotalAsset_contributionIsZero() {
            StockSaleHistory history = StockSaleHistory.create(
                    ITEM_ID, 1,
                    BigDecimal.valueOf(100),
                    BigDecimal.valueOf(150),
                    "KRW",
                    BigDecimal.ONE,
                    BigDecimal.ZERO,
                    SaleReason.OTHER,
                    null, "X", "X종목", false,
                    TODAY, TODAY
            );

            assertThat(history.getContributionRate()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("quantity가 0이면 IllegalArgumentException")
        void create_zeroQuantity_throws() {
            assertThatThrownBy(() -> StockSaleHistory.create(
                    ITEM_ID, 0,
                    BigDecimal.valueOf(100), BigDecimal.valueOf(150),
                    "KRW", BigDecimal.ONE, BigDecimal.ZERO,
                    SaleReason.OTHER, null, "X", "X", false, TODAY, TODAY
            )).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("quantity가 음수이면 IllegalArgumentException")
        void create_negativeQuantity_throws() {
            assertThatThrownBy(() -> StockSaleHistory.create(
                    ITEM_ID, -1,
                    BigDecimal.valueOf(100), BigDecimal.valueOf(150),
                    "KRW", BigDecimal.ONE, BigDecimal.ZERO,
                    SaleReason.OTHER, null, "X", "X", false, TODAY, TODAY
            )).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("salePrice가 0 이하이면 IllegalArgumentException")
        void create_invalidSalePrice_throws() {
            assertThatThrownBy(() -> StockSaleHistory.create(
                    ITEM_ID, 1,
                    BigDecimal.valueOf(100), BigDecimal.ZERO,
                    "KRW", BigDecimal.ONE, BigDecimal.ZERO,
                    SaleReason.OTHER, null, "X", "X", false, TODAY, TODAY
            )).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("avgBuyPrice가 0이면 IllegalArgumentException (수익률 계산 불가)")
        void create_zeroAvgBuyPrice_throws() {
            assertThatThrownBy(() -> StockSaleHistory.create(
                    ITEM_ID, 1,
                    BigDecimal.ZERO, BigDecimal.valueOf(150),
                    "KRW", BigDecimal.ONE, BigDecimal.ZERO,
                    SaleReason.OTHER, null, "X", "X", false, TODAY, TODAY
            )).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("미래 soldAt이면 IllegalArgumentException")
        void create_futureSoldAt_throws() {
            assertThatThrownBy(() -> StockSaleHistory.create(
                    ITEM_ID, 1,
                    BigDecimal.valueOf(100), BigDecimal.valueOf(150),
                    "KRW", BigDecimal.ONE, BigDecimal.ZERO,
                    SaleReason.OTHER, null, "X", "X", false,
                    TODAY.plusDays(1), TODAY
            )).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("reason이 null이면 IllegalArgumentException")
        void create_nullReason_throws() {
            assertThatThrownBy(() -> StockSaleHistory.create(
                    ITEM_ID, 1,
                    BigDecimal.valueOf(100), BigDecimal.valueOf(150),
                    "KRW", BigDecimal.ONE, BigDecimal.ZERO,
                    null, null, "X", "X", false, TODAY, TODAY
            )).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("update + recomputeProfit")
    class UpdateAndRecompute {

        @Test
        @DisplayName("update 후 recomputeProfit 호출 시 파생값이 새 입력 기준으로 재계산된다")
        void updateAndRecompute_refreshesDerivedValues() {
            StockSaleHistory history = StockSaleHistory.create(
                    ITEM_ID, 10,
                    BigDecimal.valueOf(70_000), BigDecimal.valueOf(80_000),
                    "KRW", BigDecimal.ONE, BigDecimal.valueOf(10_000_000),
                    SaleReason.TARGET_PRICE_REACHED, null,
                    "005930", "삼성전자", false, TODAY, TODAY
            );

            history.update(5, BigDecimal.valueOf(90_000), SaleReason.REBALANCING, "수정");
            history.recomputeProfit(BigDecimal.valueOf(10_000_000), BigDecimal.ONE);

            // profit = (90000 - 70000) * 5 = 100,000
            assertThat(history.getProfit()).isEqualByComparingTo(BigDecimal.valueOf(100_000));
            // profitRate = 100,000 / 350,000 * 100 = 28.57
            assertThat(history.getProfitRate()).isEqualByComparingTo(new BigDecimal("28.57"));
            assertThat(history.getReason()).isEqualTo(SaleReason.REBALANCING);
            assertThat(history.getMemo()).isEqualTo("수정");
        }

        @Test
        @DisplayName("markDepositRecorded 호출 시 unrecordedDeposit=false")
        void markDepositRecorded_clearsFlag() {
            StockSaleHistory history = StockSaleHistory.create(
                    ITEM_ID, 1,
                    BigDecimal.valueOf(100), BigDecimal.valueOf(150),
                    "KRW", BigDecimal.ONE, BigDecimal.valueOf(1000),
                    SaleReason.OTHER, null, "X", "X", true, TODAY, TODAY
            );

            history.markDepositRecorded();

            assertThat(history.isUnrecordedDeposit()).isFalse();
        }
    }
}