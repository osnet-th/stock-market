package com.thlee.stock.market.stockmarket.stock.infrastructure.stock.kis.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 시간외 현재가 시세 API 응답 output.
 * tr_id: FHPST02300000
 * path: /uapi/domestic-stock/v1/quotations/inquire-overtime-price
 */
@Getter
@NoArgsConstructor
public class KisOvertimePriceOutput {

    @JsonProperty("ovtm_untp_prpr")
    private String currentPrice;           // 시간외 단일가 현재가

    @JsonProperty("ovtm_untp_prdy_vrss")
    private String change;                 // 시간외 단일가 전일 대비

    @JsonProperty("ovtm_untp_prdy_vrss_sign")
    private String changeSign;             // 전일 대비 부호 (1:상한, 2:상승, 3:보합, 4:하한, 5:하락)

    @JsonProperty("ovtm_untp_prdy_ctrt")
    private String changeRate;             // 시간외 단일가 전일 대비율 (%)

    @JsonProperty("ovtm_untp_vol")
    private String volume;                 // 시간외 단일가 거래량

    @JsonProperty("ovtm_untp_tr_pbmn")
    private String tradingAmount;          // 시간외 단일가 거래대금

    @JsonProperty("ovtm_untp_hgpr")
    private String highPrice;              // 시간외 단일가 최고가

    @JsonProperty("ovtm_untp_lwpr")
    private String lowPrice;               // 시간외 단일가 최저가

    @JsonProperty("ovtm_untp_oprc")
    private String openPrice;              // 시간외 단일가 시가

    @JsonProperty("ovtm_untp_sdpr")
    private String previousClose;          // 시간외 단일가 기준가 (전일 종가)
}