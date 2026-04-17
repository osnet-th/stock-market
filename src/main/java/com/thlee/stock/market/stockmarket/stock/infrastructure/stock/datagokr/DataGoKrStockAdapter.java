package com.thlee.stock.market.stockmarket.stock.infrastructure.stock.datagokr;

import com.thlee.stock.market.stockmarket.stock.domain.model.ExchangeCode;
import com.thlee.stock.market.stockmarket.stock.domain.model.MarketType;
import com.thlee.stock.market.stockmarket.stock.domain.model.Stock;
import com.thlee.stock.market.stockmarket.stock.domain.service.StockPort;
import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.datagokr.dto.DataGoKrStockResponse;
import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.datagokr.exception.DataGoKrApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class DataGoKrStockAdapter implements StockPort {

    private final DataGoKrStockApiClient apiClient;

    @Override
    public List<Stock> searchByName(String stockName) {
        DataGoKrStockResponse response = apiClient.searchByName(stockName);

        if (!response.isSuccess()) {
            throw new DataGoKrApiException("공공데이터포털 응답 오류: "
                + response.getResponse().getHeader().getResultMsg());
        }

        Map<String, Stock> uniqueStocks = new LinkedHashMap<>();
        response.getItemList().forEach(item -> uniqueStocks.putIfAbsent(
            item.getSrtnCd(),
            new Stock(
                item.getSrtnCd(),
                item.getItmsNm(),
                null,
                MarketType.valueOf(item.getMrktCtg()),
                ExchangeCode.KRX
            )
        ));

        return new ArrayList<>(uniqueStocks.values());
    }

    @Override
    public Optional<Stock> findByCode(String stockCode) {
        if (stockCode == null || stockCode.isBlank()) {
            return Optional.empty();
        }
        DataGoKrStockResponse response = apiClient.searchByName(stockCode);
        if (!response.isSuccess()) {
            return Optional.empty();
        }
        return response.getItemList().stream()
            .filter(item -> stockCode.equals(item.getSrtnCd()))
            .findFirst()
            .map(item -> new Stock(
                item.getSrtnCd(),
                item.getItmsNm(),
                null,
                MarketType.valueOf(item.getMrktCtg()),
                ExchangeCode.KRX
            ));
    }
}