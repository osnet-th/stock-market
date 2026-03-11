package com.thlee.stock.market.stockmarket.stock.presentation.dto;

import com.thlee.stock.market.stockmarket.stock.domain.model.ExchangeCode;
import com.thlee.stock.market.stockmarket.stock.domain.model.MarketType;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class BulkStockPriceRequest {

    private List<StockPriceItem> stocks;

    @Getter
    @NoArgsConstructor
    public static class StockPriceItem {
        private String stockCode;
        private MarketType marketType;
        private ExchangeCode exchangeCode;
    }
}