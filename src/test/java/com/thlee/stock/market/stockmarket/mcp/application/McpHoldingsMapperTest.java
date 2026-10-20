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
                "005930", new McpHoldingsMapper.Quote("72000", "KRW", "72000", "1.5", "2026-10-20T03:00:00Z"));

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

    private static PortfolioItemResponse stock(String name, String code, int quantity, String avgPrice) {
        return PortfolioItemResponse.from(PortfolioItem.createWithStock(1L, name, Region.DOMESTIC,
                new StockDetail(StockSubType.INDIVIDUAL, code, "KOSPI", "KRX", "KR",
                        quantity, new BigDecimal(avgPrice), null, PriceCurrency.KRW, null)));
    }
}
