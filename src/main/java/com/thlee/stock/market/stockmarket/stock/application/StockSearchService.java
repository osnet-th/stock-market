package com.thlee.stock.market.stockmarket.stock.application;

import com.thlee.stock.market.stockmarket.stock.application.dto.StockResponse;
import com.thlee.stock.market.stockmarket.stock.domain.model.Stock;
import com.thlee.stock.market.stockmarket.stock.domain.service.StockPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StockSearchService {

    private final StockPort stockPort;

    public List<StockResponse> searchStocks(String stockName) {
        List<Stock> stocks = stockPort.searchByName(stockName);

        return stocks.stream()
            .map(StockResponse::from)
            .toList();
    }
}