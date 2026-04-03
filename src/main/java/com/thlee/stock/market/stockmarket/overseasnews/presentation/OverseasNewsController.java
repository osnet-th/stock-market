package com.thlee.stock.market.stockmarket.overseasnews.presentation;

import com.thlee.stock.market.stockmarket.overseasnews.application.OverseasNewsService;
import com.thlee.stock.market.stockmarket.overseasnews.application.dto.BreakingNewsResponse;
import com.thlee.stock.market.stockmarket.overseasnews.application.dto.ComprehensiveNewsResponse;
import com.thlee.stock.market.stockmarket.stock.domain.model.ExchangeCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

/**
 * 해외뉴스 조회 API.
 * KIS 해외속보/해외뉴스종합을 실시간 조회한다.
 */
@RestController
@RequestMapping("/api/overseas-news")
@RequiredArgsConstructor
public class OverseasNewsController {

    private static final Set<String> VALID_COUNTRIES = Set.of("US", "CN", "JP", "HK", "VN");
    private static final String STOCK_CODE_PATTERN = "^[A-Z0-9]{1,12}$";
    private static final String DATE_PATTERN = "^\\d{8}$";
    private static final String TIME_PATTERN = "^\\d{6}$";

    private final OverseasNewsService overseasNewsService;

    /**
     * 해외속보(제목) 조회.
     *
     * @param stockCode    종목코드 (예: AAPL)
     * @param exchangeCode 거래소코드 (enum 자동 검증)
     */
    @GetMapping("/breaking")
    public ResponseEntity<List<BreakingNewsResponse>> getBreakingNews(
            @RequestParam String stockCode,
            @RequestParam ExchangeCode exchangeCode) {

        validateStockCode(stockCode);

        List<BreakingNewsResponse> result = overseasNewsService.getBreakingNews(
            stockCode, exchangeCode.name());

        return ResponseEntity.ok(result);
    }

    /**
     * 해외뉴스종합(제목) 조회.
     *
     * @param stockCode    종목코드 (예: AAPL)
     * @param exchangeCode 거래소코드 (enum 자동 검증)
     * @param country      국가코드 (US, CN, JP, HK, VN)
     * @param dataDt       연속조회 기준 일자 (최초 조회 시 빈 문자열)
     * @param dataTm       연속조회 기준 시간 (최초 조회 시 빈 문자열)
     */
    @GetMapping("/comprehensive")
    public ResponseEntity<ComprehensiveNewsResponse> getComprehensiveNews(
            @RequestParam String stockCode,
            @RequestParam ExchangeCode exchangeCode,
            @RequestParam String country,
            @RequestParam(defaultValue = "") String dataDt,
            @RequestParam(defaultValue = "") String dataTm) {

        validateStockCode(stockCode);
        validateCountry(country);
        validatePaginationParams(dataDt, dataTm);

        ComprehensiveNewsResponse result = overseasNewsService.getComprehensiveNews(
            stockCode, exchangeCode.name(), country, dataDt, dataTm);

        return ResponseEntity.ok(result);
    }

    private void validateStockCode(String stockCode) {
        if (stockCode == null || !stockCode.matches(STOCK_CODE_PATTERN)) {
            throw new IllegalArgumentException("유효하지 않은 종목코드: " + stockCode);
        }
    }

    private void validateCountry(String country) {
        if (!VALID_COUNTRIES.contains(country)) {
            throw new IllegalArgumentException("유효하지 않은 국가코드: " + country);
        }
    }

    private void validatePaginationParams(String dataDt, String dataTm) {
        if (!dataDt.isEmpty() && !dataDt.matches(DATE_PATTERN)) {
            throw new IllegalArgumentException("유효하지 않은 날짜 형식: " + dataDt);
        }
        if (!dataTm.isEmpty() && !dataTm.matches(TIME_PATTERN)) {
            throw new IllegalArgumentException("유효하지 않은 시간 형식: " + dataTm);
        }
    }
}