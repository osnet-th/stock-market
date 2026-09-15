package com.thlee.stock.market.stockmarket.stock.infrastructure.stock.kis.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * KIS 국내휴장일조회 output 단건 항목.
 * tr_id: CTCA0903R
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class KisHolidayOutput {

    @JsonProperty("bass_dt")
    private String baseDate;          // 기준일자 (YYYYMMDD)

    @JsonProperty("bzdy_yn")
    private String businessDayYn;     // 영업일 여부

    @JsonProperty("tr_day_yn")
    private String tradeDayYn;        // 거래일 여부

    @JsonProperty("opnd_yn")
    private String openDayYn;         // 개장일 여부

    @JsonProperty("sttl_day_yn")
    private String settlementDayYn;   // 결제일 여부
}
