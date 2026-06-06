package com.thlee.stock.market.stockmarket.stockevaluation.application;

import com.thlee.stock.market.stockmarket.stockevaluation.application.dto.CreditEligibilityResponse;
import com.thlee.stock.market.stockmarket.stockevaluation.application.dto.EstimatePerformResponse;
import com.thlee.stock.market.stockmarket.stockevaluation.application.dto.KisTableResponse;
import com.thlee.stock.market.stockmarket.stockevaluation.application.dto.StockBasicInfoResponse;
import com.thlee.stock.market.stockmarket.stockevaluation.domain.model.FinanceStatementType;
import com.thlee.stock.market.stockmarket.stockevaluation.domain.model.KsdScheduleType;
import com.thlee.stock.market.stockmarket.stockevaluation.infrastructure.kis.KisFinanceClient;
import com.thlee.stock.market.stockmarket.stockevaluation.infrastructure.kis.KisKsdScheduleClient;
import com.thlee.stock.market.stockmarket.stockevaluation.infrastructure.kis.KisStockInfoClient;
import com.thlee.stock.market.stockmarket.stockevaluation.infrastructure.kis.dto.KisEstimatePerformResponse;
import com.thlee.stock.market.stockmarket.stockevaluation.infrastructure.kis.dto.KisSearchInfoOutput;
import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.kis.exception.KisApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 종목 평가 서비스.
 * KIS 국내주식 종목정보 API를 호출하고 결과를 응답 DTO로 변환한다.
 * DB 저장 없이 온디맨드 pass-through 방식.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StockEvaluationService {

    private final KisStockInfoClient kisStockInfoClient;
    private final KisFinanceClient kisFinanceClient;
    private final KisKsdScheduleClient kisKsdScheduleClient;

    /**
     * 종목 기본정보 조회 (상품기본조회).
     */
    public StockBasicInfoResponse getBasicInfo(String stockCode) {
        try {
            KisSearchInfoOutput output = kisStockInfoClient.searchInfo(stockCode);
            if (output == null) {
                throw new KisApiException("종목 기본정보를 불러올 수 없습니다");
            }

            return StockBasicInfoResponse.builder()
                .stockCode(stockCode)
                .productTypeCode(output.getProductTypeCode())
                .productTypeName(productTypeName(output.getProductTypeCode()))
                .productClassCode(output.getProductClassCode())
                .saleStatusCode(output.getSaleStatusCode())
                .saleStartDate(formatDate(output.getSaleStartDate()))
                .firstRegisterDate(formatDate(output.getFirstRegisterDate()))
                .build();
        } catch (KisApiException e) {
            log.error("종목 기본정보 조회 실패 [{}]: {}", stockCode, e.getMessage());
            throw new KisApiException("종목 기본정보를 불러올 수 없습니다");
        }
    }

    /**
     * 재무 항목 조회 (대차대조표/손익계산서/각종 비율).
     */
    public KisTableResponse getFinanceStatement(String stockCode, FinanceStatementType type, String divCls) {
        try {
            List<Map<String, String>> rows = kisFinanceClient.fetch(type, stockCode, divCls);
            return KisTableResponse.from(rows, type.getLabels());
        } catch (KisApiException e) {
            log.error("재무 조회 실패 [{}:{}]: {}", type, stockCode, e.getMessage());
            throw new KisApiException("재무 정보를 불러올 수 없습니다");
        }
    }

    /**
     * 종목추정실적 조회 (output1~4 → 섹션 표).
     */
    public EstimatePerformResponse getEstimatePerform(String stockCode) {
        try {
            KisEstimatePerformResponse res = kisStockInfoClient.estimatePerform(stockCode);
            List<EstimatePerformResponse.Section> sections = new ArrayList<>();
            addSection(sections, "기본정보", res.getOutput1());
            addSection(sections, "추정 상세 1", res.getOutput2());
            addSection(sections, "추정 상세 2", res.getOutput3());
            addSection(sections, "추정 상세 3", res.getOutput4());
            return EstimatePerformResponse.builder().sections(sections).build();
        } catch (KisApiException e) {
            log.error("종목추정실적 조회 실패 [{}]: {}", stockCode, e.getMessage());
            throw new KisApiException("종목추정실적을 불러올 수 없습니다");
        }
    }

    private void addSection(List<EstimatePerformResponse.Section> sections, String title, List<Map<String, String>> rows) {
        if (rows != null && !rows.isEmpty()) {
            sections.add(EstimatePerformResponse.Section.builder()
                .title(title)
                .table(KisTableResponse.fromRaw(rows))
                .build());
        }
    }

    /**
     * 신용거래 가능 여부 조회.
     * 당사 신용가능종목(전체 목록)에서 종목코드 포함 여부로 판정한다.
     */
    public CreditEligibilityResponse getCreditEligibility(String stockCode) {
        try {
            List<Map<String, String>> list = kisStockInfoClient.creditEligibleList();
            if (list != null) {
                for (Map<String, String> row : list) {
                    if (stockCode.equals(row.get("stck_shrn_iscd"))) {
                        return CreditEligibilityResponse.builder()
                            .stockCode(stockCode)
                            .eligible(true)
                            .creditRate(row.getOrDefault("crdt_rate", ""))
                            .build();
                    }
                }
            }
            return CreditEligibilityResponse.builder()
                .stockCode(stockCode)
                .eligible(false)
                .creditRate("")
                .build();
        } catch (KisApiException e) {
            log.error("신용가능종목 조회 실패 [{}]: {}", stockCode, e.getMessage());
            throw new KisApiException("신용거래 가능 여부를 불러올 수 없습니다");
        }
    }

    /**
     * 예탁원 일정 조회 (배당/증자/합병 등 12종).
     */
    public KisTableResponse getSchedule(String stockCode, KsdScheduleType type, String fromDate, String toDate) {
        try {
            List<Map<String, String>> rows = kisKsdScheduleClient.fetch(type, stockCode, fromDate, toDate);
            return KisTableResponse.from(rows, type.getLabels());
        } catch (KisApiException e) {
            log.error("예탁원 일정 조회 실패 [{}:{}]: {}", type, stockCode, e.getMessage());
            throw new KisApiException("일정 정보를 불러올 수 없습니다");
        }
    }

    private String productTypeName(String code) {
        if ("300".equals(code)) {
            return "주식";
        }
        return code;
    }

    /**
     * KIS 날짜(YYYYMMDD) → "YYYY-MM-DD" 변환.
     */
    private String formatDate(String date) {
        if (date == null || date.length() < 8) {
            return "";
        }
        return date.substring(0, 4) + "-" + date.substring(4, 6) + "-" + date.substring(6, 8);
    }
}
