package com.thlee.stock.market.stockmarket.stockevaluation.infrastructure.kis.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * KIS 상품기본조회 API 응답 output.
 * tr_id: CTPF1604R
 */
@Getter
@NoArgsConstructor
public class KisSearchInfoOutput {

    @JsonProperty("pdno")
    private String productNo;            // 상품번호 (종목코드)

    @JsonProperty("prdt_type_cd")
    private String productTypeCode;      // 상품유형코드 (300:주식)

    @JsonProperty("prdt_clsf_cd")
    private String productClassCode;     // 상품분류코드

    @JsonProperty("prdt_sale_stat_cd")
    private String saleStatusCode;       // 상품판매상태코드

    @JsonProperty("prdt_risk_grad_cd")
    private String riskGradeCode;        // 상품위험등급코드

    @JsonProperty("sale_strt_dt")
    private String saleStartDate;        // 판매시작일자 (YYYYMMDD)

    @JsonProperty("sale_end_dt")
    private String saleEndDate;          // 판매종료일자 (YYYYMMDD)

    @JsonProperty("frst_erlm_dt")
    private String firstRegisterDate;    // 최초등록일자 (YYYYMMDD)
}
