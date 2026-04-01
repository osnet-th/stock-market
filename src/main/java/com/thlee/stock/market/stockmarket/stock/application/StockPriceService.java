package com.thlee.stock.market.stockmarket.stock.application;

import com.thlee.stock.market.stockmarket.stock.application.dto.BulkStockPriceResponse;
import com.thlee.stock.market.stockmarket.stock.application.dto.StockPriceResponse;
import com.thlee.stock.market.stockmarket.stock.domain.model.CachedStockPrice;
import com.thlee.stock.market.stockmarket.stock.domain.model.ExchangeCode;
import com.thlee.stock.market.stockmarket.stock.domain.model.MarketType;
import com.thlee.stock.market.stockmarket.stock.domain.service.ExchangeRatePort;
import com.thlee.stock.market.stockmarket.stock.domain.service.StockPricePort;
import com.thlee.stock.market.stockmarket.stock.presentation.dto.BulkStockPriceRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StockPriceService {

    private final StockPricePort stockPricePort;
    private final ExchangeRatePort exchangeRatePort;

    public StockPriceResponse getPrice(String stockCode, MarketType marketType, ExchangeCode exchangeCode) {
        CachedStockPrice cached = stockPricePort.getPriceWithCacheInfo(stockCode, marketType, exchangeCode);
        String currency = exchangeCode.getCurrency();
        BigDecimal exchangeRate = exchangeRatePort.getRate(currency);
        return StockPriceResponse.from(cached.stockPrice(), currency, exchangeRate, cached.cachedAt());
    }

    public BulkStockPriceResponse getPrices(List<BulkStockPriceRequest.StockPriceItem> stocks) {
        Map<String, StockPriceResponse> prices = new LinkedHashMap<>();

        List<BulkStockPriceRequest.StockPriceItem> domesticStocks = new ArrayList<>();
        List<BulkStockPriceRequest.StockPriceItem> overseasStocks = new ArrayList<>();

        for (BulkStockPriceRequest.StockPriceItem item : stocks) {
            if (item.getMarketType().isDomestic()) {
                domesticStocks.add(item);
            } else {
                overseasStocks.add(item);
            }
        }

        // 국내 주식: 멀티종목 API로 일괄 조회
        if (!domesticStocks.isEmpty()) {
            List<String> stockCodes = domesticStocks.stream()
                .map(BulkStockPriceRequest.StockPriceItem::getStockCode)
                .toList();
            Map<String, CachedStockPrice> domesticPrices = stockPricePort.getDomesticPricesWithCacheInfo(stockCodes);
            BigDecimal krwRate = exchangeRatePort.getRate("KRW");

            for (BulkStockPriceRequest.StockPriceItem item : domesticStocks) {
                CachedStockPrice cached = domesticPrices.get(item.getStockCode());
                if (cached != null) {
                    prices.put(item.getStockCode(),
                        StockPriceResponse.from(cached.stockPrice(), "KRW", krwRate, cached.cachedAt()));
                } else {
                    prices.put(item.getStockCode(), null);
                }
            }
        }

        // 해외 주식: 기존 개별 조회
        for (BulkStockPriceRequest.StockPriceItem item : overseasStocks) {
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
