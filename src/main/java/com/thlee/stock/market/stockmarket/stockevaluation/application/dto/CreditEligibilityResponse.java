package com.thlee.stock.market.stockmarket.stockevaluation.application.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * 신용거래 가능 여부 응답 DTO.
 * 당사 신용가능종목(전체 목록) 조회 결과에서 해당 종목의 포함 여부로 판정한다.
 */
@Getter
@Builder
public class CreditEligibilityResponse {

    private final String stockCode;
    private final boolean eligible;     // 신용주문 가능 여부
    private final String creditRate;    // 신용 비율 (가능 시)
}
