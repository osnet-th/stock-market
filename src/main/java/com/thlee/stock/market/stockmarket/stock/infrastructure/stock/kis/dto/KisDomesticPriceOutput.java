package com.thlee.stock.market.stockmarket.stock.infrastructure.stock.kis.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 국내 주식 현재가 시세 API 응답 output.
 * tr_id: FHKST01010100
 */
@Getter
@NoArgsConstructor
public class KisDomesticPriceOutput {

    @JsonProperty("stck_prpr")
    private String currentPrice;       // 현재가

    @JsonProperty("stck_oprc")
    private String openPrice;          // 시가

    @JsonProperty("stck_hgpr")
    private String highPrice;          // 고가

    @JsonProperty("stck_lwpr")
    private String lowPrice;           // 저가

    @JsonProperty("stck_sdpr")
    private String previousClose;      // 전일 종가 (기준가)

    @JsonProperty("prdy_vrss")
    private String change;             // 전일 대비

    @JsonProperty("prdy_vrss_sign")
    private String changeSign;         // 전일 대비 부호 (1:상한, 2:상승, 3:보합, 4:하한, 5:하락)

    @JsonProperty("prdy_ctrt")
    private String changeRate;         // 전일 대비율

    @JsonProperty("acml_vol")
    private String volume;             // 누적 거래량

    @JsonProperty("acml_tr_pbmn")
    private String tradingAmount;      // 누적 거래대금

    @JsonProperty("w52_hgpr")
    private String week52High;         // 52주 최고가

    @JsonProperty("w52_lwpr")
    private String week52Low;          // 52주 최저가
}