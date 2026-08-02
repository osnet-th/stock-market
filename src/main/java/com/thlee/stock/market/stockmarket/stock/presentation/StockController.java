package com.thlee.stock.market.stockmarket.stock.presentation;

import com.thlee.stock.market.stockmarket.stock.application.StockPriceService;
import com.thlee.stock.market.stockmarket.stock.application.StockSearchService;
import com.thlee.stock.market.stockmarket.stock.application.dto.BulkStockPriceResponse;
import com.thlee.stock.market.stockmarket.stock.application.dto.PriceHistoryResponse;
import com.thlee.stock.market.stockmarket.stock.application.dto.StockPriceResponse;
import com.thlee.stock.market.stockmarket.stock.application.dto.StockResponse;
import com.thlee.stock.market.stockmarket.stock.domain.model.ChartPeriod;
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

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
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

    /**
     * 기간별(일/주/월봉) 가격 히스토리 조회.
     *
     * @param stockCode 종목코드 (국내 KRX 6자리 또는 미국 영문 티커)
     * @param period    봉 주기 D|W|M (기본 M)
     * @param from      시작일 yyyy-MM-dd (미지정 시 1960-01-01 — 상장 이후 전구간)
     * @param to        종료일 yyyy-MM-dd (미지정 시 오늘)
     */
    @GetMapping("/{stockCode}/price-history")
    public ResponseEntity<PriceHistoryResponse> getPriceHistory(
            @PathVariable String stockCode,
            @RequestParam(defaultValue = "M") String period,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        if (stockCode == null || !stockCode.matches("\\d{6}|[A-Za-z][A-Za-z0-9.\\-]{0,9}")) {
            throw new IllegalArgumentException("유효하지 않은 종목코드: " + stockCode);
        }
        ChartPeriod chartPeriod = ChartPeriod.fromCode(period);
        PriceHistoryResponse response = stockPriceService.getPriceHistory(
            stockCode, chartPeriod, parseDate(from), parseDate(to));
        return ResponseEntity.ok(response);
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("유효하지 않은 날짜 형식(yyyy-MM-dd): " + value);
        }
    }
}