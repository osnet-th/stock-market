package com.thlee.stock.market.stockmarket.stockevaluation.presentation;

import com.thlee.stock.market.stockmarket.stockevaluation.application.StockEvaluationService;
import com.thlee.stock.market.stockmarket.stockevaluation.application.dto.CreditEligibilityResponse;
import com.thlee.stock.market.stockmarket.stockevaluation.application.dto.EstimatePerformResponse;
import com.thlee.stock.market.stockmarket.stockevaluation.application.dto.KisTableResponse;
import com.thlee.stock.market.stockmarket.stockevaluation.application.dto.StockBasicInfoResponse;
import com.thlee.stock.market.stockmarket.stockevaluation.domain.model.FinanceDivCls;
import com.thlee.stock.market.stockmarket.stockevaluation.domain.model.FinanceStatementType;
import com.thlee.stock.market.stockmarket.stockevaluation.domain.model.KsdScheduleType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 종목 평가 API.
 * KIS 국내주식 종목정보를 실시간 조회한다. (보유 여부 무관)
 */
@RestController
@RequestMapping("/api/stock-evaluation")
@RequiredArgsConstructor
public class StockEvaluationController {

    private static final String STOCK_CODE_PATTERN = "^\\d{6}$"; // KRX 6자리
    private static final String DATE_PATTERN = "^\\d{8}$";       // YYYYMMDD
    private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final int DEFAULT_FROM_MONTHS = 6;            // 기본 조회 시작: 6개월 전
    private static final int DEFAULT_TO_MONTHS = 3;              // 기본 조회 종료: 3개월 후

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

    /**
     * 재무 항목 조회 (대차대조표/손익계산서/각종 비율).
     *
     * @param stockCode 종목코드 (KRX 6자리)
     * @param type      재무 항목 코드 (balance-sheet, income-statement, financial-ratio, profit-ratio,
     *                  other-major-ratios, stability-ratio, growth-ratio)
     * @param divCls    조회 단위 (ANNUAL: 년, QUARTER: 분기)
     */
    @GetMapping("/{stockCode}/finance/{type}")
    public ResponseEntity<KisTableResponse> getFinance(
            @PathVariable String stockCode,
            @PathVariable String type,
            @RequestParam(defaultValue = "ANNUAL") FinanceDivCls divCls) {
        validateStockCode(stockCode);
        FinanceStatementType statementType = FinanceStatementType.fromCode(type);
        return ResponseEntity.ok(
                stockEvaluationService.getFinanceStatement(stockCode, statementType, divCls.getCode()));
    }

    /**
     * 종목추정실적 조회.
     */
    @GetMapping("/{stockCode}/estimate-perform")
    public ResponseEntity<EstimatePerformResponse> getEstimatePerform(@PathVariable String stockCode) {
        validateStockCode(stockCode);
        return ResponseEntity.ok(stockEvaluationService.getEstimatePerform(stockCode));
    }

    /**
     * 신용거래 가능 여부 조회.
     */
    @GetMapping("/{stockCode}/credit-eligibility")
    public ResponseEntity<CreditEligibilityResponse> getCreditEligibility(@PathVariable String stockCode) {
        validateStockCode(stockCode);
        return ResponseEntity.ok(stockEvaluationService.getCreditEligibility(stockCode));
    }

    /**
     * 예탁원 일정 조회 (배당/증자/합병 등 12종).
     *
     * @param stockCode 종목코드 (KRX 6자리, SHT_CD 필터)
     * @param type      일정 종류 코드 (dividend, purchase-request, merger-split, face-value-change,
     *                  capital-reduction, listing, public-offering, forfeited, mandatory-deposit,
     *                  paid-in-capital, bonus-issue, shareholders-meeting)
     * @param fromDate  조회 시작일 (YYYYMMDD, 미지정 시 6개월 전)
     * @param toDate    조회 종료일 (YYYYMMDD, 미지정 시 3개월 후)
     */
    @GetMapping("/{stockCode}/schedules/{type}")
    public ResponseEntity<KisTableResponse> getSchedule(
            @PathVariable String stockCode,
            @PathVariable String type,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate) {
        validateStockCode(stockCode);
        KsdScheduleType scheduleType = KsdScheduleType.fromCode(type);
        String from = resolveDate(fromDate, LocalDate.now().minusMonths(DEFAULT_FROM_MONTHS));
        String to = resolveDate(toDate, LocalDate.now().plusMonths(DEFAULT_TO_MONTHS));
        return ResponseEntity.ok(stockEvaluationService.getSchedule(stockCode, scheduleType, from, to));
    }

    private void validateStockCode(String stockCode) {
        if (stockCode == null || !stockCode.matches(STOCK_CODE_PATTERN)) {
            throw new IllegalArgumentException("유효하지 않은 종목코드: " + stockCode);
        }
    }

    private String resolveDate(String value, LocalDate defaultDate) {
        if (value == null || value.isBlank()) {
            return defaultDate.format(YMD);
        }
        if (!value.matches(DATE_PATTERN)) {
            throw new IllegalArgumentException("유효하지 않은 날짜 형식: " + value);
        }
        return value;
    }
}
