package com.thlee.stock.market.stockmarket.stock.application;

import com.thlee.stock.market.stockmarket.stock.application.dto.BulkStockPriceResponse;
import com.thlee.stock.market.stockmarket.stock.application.dto.StockPriceResponse;
import com.thlee.stock.market.stockmarket.stock.domain.model.ExchangeCode;
import com.thlee.stock.market.stockmarket.stock.domain.model.MarketType;
import com.thlee.stock.market.stockmarket.stock.domain.model.StockPrice;
import com.thlee.stock.market.stockmarket.stock.domain.service.ExchangeRatePort;
import com.thlee.stock.market.stockmarket.stock.domain.service.StockPricePort;
import com.thlee.stock.market.stockmarket.stock.presentation.dto.BulkStockPriceRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StockPriceService {

    private final StockPricePort stockPricePort;
    private final ExchangeRatePort exchangeRatePort;

    public StockPriceResponse getPrice(String stockCode, MarketType marketType, ExchangeCode exchangeCode) {
        StockPrice price = stockPricePort.getPrice(stockCode, marketType, exchangeCode);
        String currency = exchangeCode.getCurrency();
        BigDecimal exchangeRate = exchangeRatePort.getRate(currency);
        return StockPriceResponse.from(price, currency, exchangeRate);
    }

    public BulkStockPriceResponse getPrices(List<BulkStockPriceRequest.StockPriceItem> stocks) {
        Map<String, StockPriceResponse> prices = new LinkedHashMap<>();

        for (BulkStockPriceRequest.StockPriceItem item : stocks) {
            try {
                StockPriceResponse response = getPrice(
                        item.getStockCode(),
                        item.getMarketType(),
                        item.getExchangeCode()
                );
                prices.put(item.getStockCode(), response);
            } catch (Exception e) {
                prices.put(item.getStockCode(), null);
            }
        }

        return new BulkStockPriceResponse(prices);
    }
}