package com.thlee.stock.market.stockmarket.stock.presentation;

import com.thlee.stock.market.stockmarket.stock.application.StockSearchService;
import com.thlee.stock.market.stockmarket.stock.application.dto.StockResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/stocks")
@RequiredArgsConstructor
public class StockController {

    private final StockSearchService stockSearchService;

    @GetMapping("/search")
    public ResponseEntity<List<StockResponse>> searchStocks(
            @RequestParam String name) {
        List<StockResponse> results = stockSearchService.searchStocks(name);
        return ResponseEntity.ok(results);
    }
}