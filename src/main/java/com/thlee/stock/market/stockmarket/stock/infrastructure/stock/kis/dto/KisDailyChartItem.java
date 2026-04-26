package com.thlee.stock.market.stockmarket.stock.infrastructure.stock.kis.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * KIS 일봉(국내주식기간별시세) output2 단건 항목.
 * tr_id: FHKST03010100
 */
@Getter
@NoArgsConstructor
public class KisDailyChartItem {

    @JsonProperty("stck_bsop_date")
    private String businessDate;     // 영업일자 (YYYYMMDD)

    @JsonProperty("stck_clpr")
    private String closePrice;       // 종가

    @JsonProperty("stck_oprc")
    private String openPrice;        // 시가

    @JsonProperty("stck_hgpr")
    private String highPrice;        // 고가

    @JsonProperty("stck_lwpr")
    private String lowPrice;         // 저가

    @JsonProperty("acml_vol")
    private String volume;           // 누적 거래량
}