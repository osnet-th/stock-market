package com.thlee.stock.market.stockmarket.stock.presentation;

import com.thlee.stock.market.stockmarket.stock.application.SecFinancialService;
import com.thlee.stock.market.stockmarket.stock.application.dto.SecFilingResponse;
import com.thlee.stock.market.stockmarket.stock.application.dto.SecFinancialStatementResponse;
import com.thlee.stock.market.stockmarket.stock.application.dto.SecInvestmentMetricResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stocks")
@RequiredArgsConstructor
public class SecFinancialController {

    private final SecFinancialService secFinancialService;

    @GetMapping("/{ticker}/sec/financial/statements")
    public ResponseEntity<List<SecFinancialStatementResponse>> getFinancialStatements(
            @PathVariable String ticker) {
        return ResponseEntity.ok(secFinancialService.getFinancialStatements(ticker));
    }

    @GetMapping("/{ticker}/sec/financial/statements/quarterly")
    public ResponseEntity<List<SecFinancialStatementResponse>> getQuarterlyFinancialStatements(
            @PathVariable String ticker) {
        return ResponseEntity.ok(secFinancialService.getQuarterlyFinancialStatements(ticker));
    }

    @GetMapping("/{ticker}/sec/financial/metrics")
    public ResponseEntity<List<SecInvestmentMetricResponse>> getInvestmentMetrics(
            @PathVariable String ticker) {
        return ResponseEntity.ok(secFinancialService.getInvestmentMetrics(ticker));
    }

    @GetMapping("/{ticker}/sec/cik")
    public ResponseEntity<Map<String, Long>> getCik(@PathVariable String ticker) {
        return ResponseEntity.ok(Map.of("cik", secFinancialService.getCik(ticker)));
    }

    /**
     * 최근 제출 서식 목록 (US 리포트 공시 패널용). limit 기본 20, 최대 100.
     */
    @GetMapping("/{ticker}/sec/filings")
    public ResponseEntity<List<SecFilingResponse>> getRecentFilings(
            @PathVariable String ticker,
            @RequestParam(defaultValue = "20") int limit) {
        int boundedLimit = Math.min(Math.max(limit, 1), 100);
        return ResponseEntity.ok(secFinancialService.getRecentFilings(ticker, boundedLimit));
    }
}