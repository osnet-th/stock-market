package com.thlee.stock.market.stockmarket.stockevaluation.application.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * 종목 기본정보 응답 DTO (상품기본조회 정제값).
 * 평가 화면 상단 헤더에 노출된다.
 */
@Getter
@Builder
public class StockBasicInfoResponse {

    private final String stockCode;          // 종목코드
    private final String productTypeCode;    // 상품유형코드 (300:주식)
    private final String productTypeName;    // 상품유형명 (라벨)
    private final String productClassCode;   // 상품분류코드
    private final String saleStatusCode;     // 상품판매상태코드
    private final String saleStartDate;      // 판매시작일자 (YYYY-MM-DD)
    private final String firstRegisterDate;  // 최초등록일자 (YYYY-MM-DD)
}
