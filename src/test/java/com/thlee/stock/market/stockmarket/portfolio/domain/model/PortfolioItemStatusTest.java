package com.thlee.stock.market.stockmarket.portfolio.domain.model;

import com.thlee.stock.market.stockmarket.news.domain.model.Region;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.enums.AssetType;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.enums.PortfolioItemStatus;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.enums.PriceCurrency;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.enums.StockSubType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("PortfolioItem 상태/매도 도메인 메서드")
class PortfolioItemStatusTest {

    private static final Long USER_ID = 1L;
    private static final String ITEM_NAME = "삼성전자";

    private PortfolioItem stockItem(int quantity, BigDecimal avgBuyPrice) {
        StockDetail detail = new StockDetail(
                StockSubType.INDIVIDUAL,
                "005930",
                "KOSPI",
                "KS",
                "KR",
                quantity,
                avgBuyPrice,
                BigDecimal.valueOf(2.5),
                PriceCurrency.KRW,
                null
        );
        return PortfolioItem.createWithStock(USER_ID, ITEM_NAME, Region.DOMESTIC, detail);
    }

    @Nested
    @DisplayName("createWithStock 등 factory")
    class Factory {

        @Test
        @DisplayName("새로 생성한 항목의 status는 ACTIVE다")
        void create_isActive() {
            PortfolioItem item = stockItem(10, BigDecimal.valueOf(70_000));

            assertThat(item.getStatus()).isEqualTo(PortfolioItemStatus.ACTIVE);
        }
    }

    @Nested
    @DisplayName("deductStockQuantity")
    class DeductStockQuantity {

        @Test
        @DisplayName("부분 매도 시 quantity가 차감되고 status는 ACTIVE를 유지한다")
        void partialSale_keepsActive() {
            PortfolioItem item = stockItem(10, BigDecimal.valueOf(70_000));

            item.deductStockQuantity(3);

            assertThat(item.getStockDetail().getQuantity()).isEqualTo(7);
            assertThat(item.getStatus()).isEqualTo(PortfolioItemStatus.ACTIVE);
            assertThat(item.getInvestedAmount())
                    .isEqualByComparingTo(BigDecimal.valueOf(70_000)
                            .multiply(BigDecimal.valueOf(7))
                            .setScale(2, RoundingMode.HALF_UP));
        }

        @Test
        @DisplayName("전량 매도 시 quantity는 0, status는 CLOSED, investedAmount는 0이 된다")
        void fullSale_closes() {
            PortfolioItem item = stockItem(10, BigDecimal.valueOf(70_000));

            item.deductStockQuantity(10);

            assertThat(item.getStockDetail().getQuantity()).isZero();
            assertThat(item.getStatus()).isEqualTo(PortfolioItemStatus.CLOSED);
            assertThat(item.getInvestedAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("매도 수량이 0이면 IllegalArgumentException")
        void zeroQuantity_throws() {
            PortfolioItem item = stockItem(10, BigDecimal.valueOf(70_000));

            assertThatThrownBy(() -> item.deductStockQuantity(0))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("매도 수량이 음수면 IllegalArgumentException")
        void negativeQuantity_throws() {
            PortfolioItem item = stockItem(10, BigDecimal.valueOf(70_000));

            assertThatThrownBy(() -> item.deductStockQuantity(-1))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("매도 수량이 보유 수량을 초과하면 IllegalArgumentException")
        void overQuantity_throws() {
            PortfolioItem item = stockItem(10, BigDecimal.valueOf(70_000));

            assertThatThrownBy(() -> item.deductStockQuantity(11))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("보유 수량");
        }

        @Test
        @DisplayName("주식이 아닌 항목에서 호출하면 IllegalArgumentException")
        void nonStock_throws() {
            PortfolioItem item = PortfolioItem.create(USER_ID, "비트코인",
                    AssetType.CRYPTO, BigDecimal.valueOf(1_000_000), Region.INTERNATIONAL);

            assertThatThrownBy(() -> item.deductStockQuantity(1))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("CLOSED 항목에서 호출하면 IllegalArgumentException")
        void closedItem_throws() {
            PortfolioItem item = stockItem(10, BigDecimal.valueOf(70_000));
            item.deductStockQuantity(10);

            assertThatThrownBy(() -> item.deductStockQuantity(1))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("closeItem / reopenItem")
    class CloseAndReopen {

        @Test
        @DisplayName("ACTIVE → closeItem 호출 시 CLOSED로 전환")
        void close_fromActive() {
            PortfolioItem item = stockItem(10, BigDecimal.valueOf(70_000));

            item.closeItem();

            assertThat(item.getStatus()).isEqualTo(PortfolioItemStatus.CLOSED);
        }

        @Test
        @DisplayName("CLOSED → closeItem 재호출 시 IllegalArgumentException")
        void close_fromClosed_throws() {
            PortfolioItem item = stockItem(10, BigDecimal.valueOf(70_000));
            item.closeItem();

            assertThatThrownBy(item::closeItem)
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("CLOSED → reopenItem 호출 시 ACTIVE로 복원")
        void reopen_fromClosed() {
            PortfolioItem item = stockItem(10, BigDecimal.valueOf(70_000));
            item.closeItem();

            item.reopenItem();

            assertThat(item.getStatus()).isEqualTo(PortfolioItemStatus.ACTIVE);
        }

        @Test
        @DisplayName("ACTIVE → reopenItem 호출 시 IllegalArgumentException")
        void reopen_fromActive_throws() {
            PortfolioItem item = stockItem(10, BigDecimal.valueOf(70_000));

            assertThatThrownBy(item::reopenItem)
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}