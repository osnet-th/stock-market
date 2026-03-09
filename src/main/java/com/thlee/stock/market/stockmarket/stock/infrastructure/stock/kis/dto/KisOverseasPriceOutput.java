package com.thlee.stock.market.stockmarket.stock.infrastructure.stock.kis.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 해외 주식 현재체결가 API 응답 output.
 * tr_id: HHDFS00000300
 */
@Getter
@NoArgsConstructor
public class KisOverseasPriceOutput {

    @JsonProperty("last")
    private String currentPrice;       // 현재가

    @JsonProperty("base")
    private String previousClose;      // 전일 종가

    @JsonProperty("diff")
    private String change;             // 전일 대비

    @JsonProperty("sign")
    private String changeSign;         // 대비 부호

    @JsonProperty("rate")
    private String changeRate;         // 등락률

    @JsonProperty("tvol")
    private String volume;             // 거래량

    @JsonProperty("tamt")
    private String tradingAmount;      // 거래대금

    @JsonProperty("ordy")
    private String tradeable;          // 매수가능여부
}