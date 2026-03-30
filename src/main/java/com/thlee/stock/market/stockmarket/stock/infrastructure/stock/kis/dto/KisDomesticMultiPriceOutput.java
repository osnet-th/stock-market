package com.thlee.stock.market.stockmarket.stock.infrastructure.stock.kis.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 국내 멀티종목 시세조회 API 응답 output 항목.
 * tr_id: FHKST11300006
 * 최대 30종목 일괄 조회 시 output 배열의 각 요소.
 */
@Getter
@NoArgsConstructor
public class KisDomesticMultiPriceOutput {

    @JsonProperty("inter_shrn_iscd")
    private String stockCode;           // 종목코드

    @JsonProperty("inter_kor_isnm")
    private String stockName;           // 종목명

    @JsonProperty("inter2_prpr")
    private String currentPrice;        // 현재가

    @JsonProperty("inter2_prdy_clpr")
    private String previousClose;       // 전일 종가

    @JsonProperty("inter2_prdy_vrss")
    private String change;              // 전일 대비

    @JsonProperty("prdy_vrss_sign")
    private String changeSign;          // 전일 대비 부호

    @JsonProperty("prdy_ctrt")
    private String changeRate;          // 전일 대비율

    @JsonProperty("acml_vol")
    private String volume;              // 누적 거래량

    @JsonProperty("acml_tr_pbmn")
    private String tradingAmount;       // 누적 거래대금

    @JsonProperty("inter2_hgpr")
    private String highPrice;           // 고가

    @JsonProperty("inter2_lwpr")
    private String lowPrice;            // 저가

    @JsonProperty("inter2_oprc")
    private String openPrice;           // 시가
}