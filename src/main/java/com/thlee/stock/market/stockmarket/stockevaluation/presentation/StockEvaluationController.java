package com.thlee.stock.market.stockmarket.stockevaluation.presentation;

import com.thlee.stock.market.stockmarket.stockevaluation.application.StockEvaluationService;
import com.thlee.stock.market.stockmarket.stockevaluation.application.dto.StockBasicInfoResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 종목 평가 API.
 * KIS 국내주식 종목정보를 실시간 조회한다. (보유 여부 무관)
 */
@RestController
@RequestMapping("/api/stock-evaluation")
@RequiredArgsConstructor
public class StockEvaluationController {

    private static final String STOCK_CODE_PATTERN = "^\\d{6}$"; // KRX 6자리

    private final StockEvaluationService stockEvaluationService;

    /**
     * 종목 기본정보 조회 (상품기본조회).
     *
     * @param stockCode 종목코드 (KRX 6자리)
     */
    @GetMapping("/{stockCode}/basic-info")
    public ResponseEntity<StockBasicInfoResponse> getBasicInfo(@PathVariable String stockCode) {
        validateStockCode(stockCode);
        return ResponseEntity.ok(stockEvaluationService.getBasicInfo(stockCode));
    }

    private void validateStockCode(String stockCode) {
        if (stockCode == null || !stockCode.matches(STOCK_CODE_PATTERN)) {
            throw new IllegalArgumentException("유효하지 않은 종목코드: " + stockCode);
        }
    }
}
