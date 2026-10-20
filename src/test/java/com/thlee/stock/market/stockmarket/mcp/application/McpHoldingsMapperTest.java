package com.thlee.stock.market.stockmarket.mcp.application;

import com.thlee.stock.market.stockmarket.mcp.application.dto.McpHoldings;
import com.thlee.stock.market.stockmarket.news.domain.model.Region;
import com.thlee.stock.market.stockmarket.portfolio.application.dto.PortfolioItemResponse;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.PortfolioItem;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.StockDetail;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.enums.PriceCurrency;
import com.thlee.stock.market.stockmarket.portfolio.domain.model.enums.StockSubType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class McpHoldingsMapperTest {

    @Test
    @DisplayName("M2: 평가액은 수량×원화 현재가이고, 현재가가 없으면 평가액이 비고 사유가 표시된다")
    void evaluatedAmount() {
        // given
        PortfolioItemResponse samsung = stock("삼성전자", "005930", 10, "70000");
        PortfolioItemResponse hynix = stock("SK하이닉스", "000660", 3, "150000");
        Map<String, McpHoldingsMapper.Quote> quotes = Map.of(
                "005930", new McpHoldingsMapper.Quote("72000", "KRW", "72000", BigDecimal.ONE, "1.5", "2026-10-20T03:00:00Z"));

        // when
        McpHoldings result = McpHoldingsMapper.toHoldings(List.of(samsung, hynix), quotes);

        // then
        assertThat(result.currency()).isEqualTo("KRW");
        McpHoldings.Holding first = result.items().get(0);
        assertThat(first.stockCode()).isEqualTo("005930");
        assertThat(first.investedAmountKrw()).isEqualByComparingTo("700000");
        assertThat(first.evaluatedAmountKrw()).isEqualByComparingTo("720000");
        assertThat(first.profitKrw()).isEqualByComparingTo("20000");
        assertThat(first.priceNote()).isNull();

        McpHoldings.Holding second = result.items().get(1);
        assertThat(second.evaluatedAmountKrw()).isNull();
        assertThat(second.profitKrw()).isNull();
        assertThat(second.priceNote()).contains("현재가");
        assertThat(result.priceMissingCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("M3: 원화 현재가가 0이면 현재가 없음, 해외 종목 원화 원금이 없으면 환율로 환산하고 환율도 없으면 원금·손익을 비운다")
    void zeroPriceAndForeignPrincipal() {
        // given
        PortfolioItemResponse zero = stock("거래정지", "123456", 5, "10000");
        PortfolioItemResponse apple = foreign("애플", "AAPL", 2, "150");
        PortfolioItemResponse msft = foreign("마이크로소프트", "MSFT", 1, "400");
        Map<String, McpHoldingsMapper.Quote> quotes = Map.of(
                "123456", new McpHoldingsMapper.Quote("0", "KRW", "0", BigDecimal.ONE, "0", "t"),
                "AAPL", new McpHoldingsMapper.Quote("200", "USD", "280000", new BigDecimal("1400"), "0.5", "t"));

        // when
        McpHoldings result = McpHoldingsMapper.toHoldings(List.of(zero, apple, msft), quotes);

        // then
        McpHoldings.Holding zeroHolding = result.items().get(0);
        assertThat(zeroHolding.evaluatedAmountKrw()).isNull();
        assertThat(zeroHolding.priceNote()).contains("현재가");

        McpHoldings.Holding appleHolding = result.items().get(1);
        assertThat(appleHolding.investedAmountKrw()).isEqualByComparingTo("420000");
        assertThat(appleHolding.evaluatedAmountKrw()).isEqualByComparingTo("560000");
        assertThat(appleHolding.profitKrw()).isEqualByComparingTo("140000");
        assertThat(appleHolding.priceNote()).isNull();

        McpHoldings.Holding msftHolding = result.items().get(2);
        assertThat(msftHolding.investedAmountKrw()).isNull();
        assertThat(msftHolding.profitKrw()).isNull();
        assertThat(msftHolding.priceNote()).contains("원화 원금");
        assertThat(result.priceMissingCount()).isEqualTo(2);
    }

    private static PortfolioItemResponse stock(String name, String code, int quantity, String avgPrice) {
        return PortfolioItemResponse.from(PortfolioItem.createWithStock(1L, name, Region.DOMESTIC,
                new StockDetail(StockSubType.INDIVIDUAL, code, "KOSPI", "KRX", "KR",
                        quantity, new BigDecimal(avgPrice), null, PriceCurrency.KRW, null)));
    }

    private static PortfolioItemResponse foreign(String name, String code, int quantity, String avgPrice) {
        return PortfolioItemResponse.from(PortfolioItem.createWithStock(1L, name, Region.INTERNATIONAL,
                new StockDetail(StockSubType.INDIVIDUAL, code, "NASDAQ", "NAS", "US",
                        quantity, new BigDecimal(avgPrice), null, PriceCurrency.USD, null)));
    }
}
