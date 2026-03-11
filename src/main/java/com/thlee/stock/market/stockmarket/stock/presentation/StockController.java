package com.thlee.stock.market.stockmarket.stock.presentation;

import com.thlee.stock.market.stockmarket.stock.application.StockPriceService;
import com.thlee.stock.market.stockmarket.stock.application.StockSearchService;
import com.thlee.stock.market.stockmarket.stock.application.dto.BulkStockPriceResponse;
import com.thlee.stock.market.stockmarket.stock.application.dto.StockPriceResponse;
import com.thlee.stock.market.stockmarket.stock.application.dto.StockResponse;
import com.thlee.stock.market.stockmarket.stock.domain.model.ExchangeCode;
import com.thlee.stock.market.stockmarket.stock.domain.model.MarketType;
import com.thlee.stock.market.stockmarket.stock.presentation.dto.BulkStockPriceRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/stocks")
@RequiredArgsConstructor
public class StockController {

    private final StockSearchService stockSearchService;
    private final StockPriceService stockPriceService;

    @GetMapping("/search")
    public ResponseEntity<List<StockResponse>> searchStocks(
            @RequestParam String name) {
        List<StockResponse> results = stockSearchService.searchStocks(name);
        return ResponseEntity.ok(results);
    }

    /**
     * 주식/ETF 현재가 조회.
     *
     * @param stockCode    종목코드 (국내: 005930, 해외: AAPL)
     * @param marketType   시장구분 (KOSPI, NASDAQ 등)
     * @param exchangeCode 거래소코드 (KRX, NAS 등)
     */
    @GetMapping("/{stockCode}/price")
    public ResponseEntity<StockPriceResponse> getPrice(
            @PathVariable String stockCode,
            @RequestParam MarketType marketType,
            @RequestParam ExchangeCode exchangeCode) {
        StockPriceResponse response = stockPriceService.getPrice(stockCode, marketType, exchangeCode);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/prices")
    public ResponseEntity<BulkStockPriceResponse> getPrices(
            @RequestBody BulkStockPriceRequest request) {
        BulkStockPriceResponse response = stockPriceService.getPrices(request.getStocks());
        return ResponseEntity.ok(response);
    }
}