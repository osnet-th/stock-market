package com.thlee.stock.market.stockmarket.stockevaluation.application;

import com.thlee.stock.market.stockmarket.stockevaluation.application.dto.StockBasicInfoResponse;
import com.thlee.stock.market.stockmarket.stockevaluation.infrastructure.kis.KisStockInfoClient;
import com.thlee.stock.market.stockmarket.stockevaluation.infrastructure.kis.dto.KisSearchInfoOutput;
import com.thlee.stock.market.stockmarket.stock.infrastructure.stock.kis.exception.KisApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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
